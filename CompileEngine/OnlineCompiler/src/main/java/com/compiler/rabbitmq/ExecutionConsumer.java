package com.compiler.rabbitmq;

import com.compiler.config.RabbitMQConfig;
import com.compiler.dto.ResultEvent;
import com.compiler.kafka.KafkaProducerService;
import com.compiler.model.Submission;
import com.compiler.model.TestCase;
import com.compiler.repository.SubmissionRepository;
import com.compiler.service.TestCaseSyncService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ExecutionConsumer {

    private final SubmissionRepository submissionRepository;
    private final TestCaseSyncService testCaseSyncService;
    private final KafkaProducerService kafkaProducer;

    // Maps to "submission-result" so Dharma's Leaderboard can hear it
    @Value("${kafka.topic.result:submission-result}")
    private String resultTopic;

    private static final int COMPILE_LIMIT = 10;
    private static final String MEMORY_LIMIT = "256m";

    public ExecutionConsumer(SubmissionRepository submissionRepository,
                             TestCaseSyncService testCaseSyncService,
                             KafkaProducerService kafkaProducer) {
        this.submissionRepository = submissionRepository;
        this.testCaseSyncService = testCaseSyncService;
        this.kafkaProducer = kafkaProducer;
    }

    @RabbitListener(queues = RabbitMQConfig.EXECUTION_QUEUE)
    public void consume(Long submissionId) {
        Path tempDir = null;
        Submission submission = null; // Declared outside try-block for safe error handling

        try {
            submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found in DB"));

            tempDir = Files.createTempDirectory("exec-" + submissionId);
            String language = submission.getLanguage().toUpperCase();
            String imageName = getImageName(language);

            // 1. Write Source Code
            String fileName = getSourceFileName(language);
            Files.writeString(tempDir.resolve(fileName), submission.getCode());

            // 2. Compilation Phase
            if (isCompiledLanguage(language)) {
                ExecutionResult compileRes = runInDocker(tempDir, imageName, getCompileCmd(language), null, COMPILE_LIMIT);
                if (compileRes.getExitCode() != 0) {
                    sendKafkaResult(submission, "COMPILATION_ERROR", compileRes.getOutput());
                    return;
                }
            }

            // 3. Execution Phase (Test Case Loop)
            List<TestCase> testCases = testCaseSyncService.getOrSyncTestCases(submission.getProblemId());
            if (testCases.isEmpty()) {
                sendKafkaResult(submission, "SYSTEM_ERROR", "No test cases found for this problem.");
                return;
            }
            int timeLimit = language.equals("PYTHON") ? 5 : 2;

            for (TestCase tc : testCases) {
                ExecutionResult result = runInDocker(tempDir, imageName, getRunCmd(language), tc.getInput(), timeLimit);

                if (result.isTle()) {
                    sendKafkaResult(submission, "TIME_LIMIT_EXCEEDED", "Time Limit Exceeded");
                    return;
                }

                if (result.getExitCode() != 0) {
                    sendKafkaResult(submission, "RUNTIME_ERROR", result.getOutput());
                    return;
                }

                if (!compareOutputs(tc.getExpectedOutput(), result.getOutput())) {
                    sendKafkaResult(submission, "WRONG_ANSWER", "Expected: " + tc.getExpectedOutput() + "\nActual: " + result.getOutput());
                    return;
                }
            }

            // If we reach here, everything passed!
            sendKafkaResult(submission, "ACCEPTED", "All Test Cases Passed");

        } catch (Exception e) {
            // Safe Error Handling: Preserve user data if we successfully fetched it before the crash
            if (submission != null) {
                sendKafkaResult(submission, "SYSTEM_ERROR", e.getMessage());
            } else {
                // We crashed trying to talk to the DB itself. Create a dummy to avoid NullPointerExceptions.
                Submission errorSub = new Submission();
                errorSub.setId(submissionId);
                sendKafkaResult(errorSub, "SYSTEM_ERROR", "Failed to fetch submission: " + e.getMessage());
            }
        } finally {
            cleanup(tempDir);
        }
    }

    // --- HELPER: Formats the 9-Field Payload for the Frontend and Leaderboard ---
    private void sendKafkaResult(Submission submission, String internalStatus, String output) {
        
        // Map to Dharma's strict Codeforces Verdicts ("AC" or "WA")
        String verdict = internalStatus.equals("ACCEPTED") ? "AC" : "WA";
        
        // Format the timestamp to ISO-8601 safely
        String submittedAt = submission.getCreatedAt() != null 
                ? submission.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME) 
                : "";

        // Build the bridge payload
        ResultEvent event = new ResultEvent(
                String.valueOf(submission.getId()),
                submission.getUserId(),
                submission.getUsername(),
                submission.getContestId(),
                submission.getProblemId(),
                verdict,         // For Leaderboard ("AC"/"WA")
                internalStatus,  // For Frontend ("ACCEPTED", "COMPILE_ERROR", etc.)
                output,          // For Frontend (Stack traces, diffs)
                submittedAt
        );

        // Broadcast to the Event Bus
        kafkaProducer.send(resultTopic, event);
        
        System.out.println("🚀 Kafka Broadcast [Topic: " + resultTopic + "] -> " + 
                           "User: " + event.getUsername() + 
                           " | Verdict: " + event.getVerdict() + 
                           " | Status: " + event.getStatus());
    }

    // --- DOCKER SANDBOX EXECUTION ---
    private ExecutionResult runInDocker(Path tempDir, String imageName, String shellCommand, String input, int timeoutSeconds) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "run", "--rm", "-i",
                "--network", "none",
                "--memory=" + MEMORY_LIMIT,
                "-v", tempDir.toAbsolutePath() + ":/app",
                "-w", "/app",
                imageName,
                "sh", "-c", shellCommand
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        if (input != null && !input.isEmpty()) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(input.getBytes());
                os.flush();
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            return new ExecutionResult(1, "TLE", true);
        }

        String output = readProcessOutput(process);
        return new ExecutionResult(process.exitValue(), output, false);
    }

    // --- DYNAMIC RUNNER MAPPINGS ---
    private String getImageName(String lang) {
        return switch (lang) {
            case "JAVA" -> "java-runner";
            case "PYTHON" -> "python-runner";
            case "CPP", "C" -> "cpp-runner";
            default -> throw new IllegalArgumentException("No runner mapped for: " + lang);
        };
    }

    private String getCompileCmd(String lang) {
        return switch (lang) {
            case "JAVA" -> "javac Main.java";
            case "CPP" -> "g++ -O3 main.cpp -o main";
            case "C" -> "gcc -O3 main.c -o main";
            default -> "";
        };
    }

    private String getRunCmd(String lang) {
        return switch (lang) {
            case "JAVA" -> "java Main";
            case "PYTHON" -> "python3 main.py";
            case "CPP", "C" -> "./main";
            default -> "";
        };
    }

    private String getSourceFileName(String lang) {
        return switch (lang) {
            case "JAVA" -> "Main.java";
            case "PYTHON" -> "main.py";
            case "CPP" -> "main.cpp";
            case "C" -> "main.c";
            default -> "main.txt";
        };
    }

    private boolean isCompiledLanguage(String lang) {
        return List.of("JAVA", "CPP", "C").contains(lang);
    }

    private boolean compareOutputs(String expected, String actual) {
        if (expected == null || actual == null) return false;
        // Using trim() to safely ignore trailing newlines and carriage returns (\r)
        return expected.trim().equals(actual.trim());
    }

    private String readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

    private void cleanup(Path tempDir) {
        if (tempDir == null) return;
        try {
            Files.walk(tempDir)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {}
    }
}