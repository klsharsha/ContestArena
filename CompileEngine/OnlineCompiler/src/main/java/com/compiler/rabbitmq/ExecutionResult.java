package com.compiler.rabbitmq;

public class ExecutionResult {

    private String output;
    private boolean tle;
    private int exitCode;

    // Standard constructor for general results
    public ExecutionResult(int exitCode, String output, boolean tle) {
        this.exitCode = exitCode;
        this.output = output;
        this.tle = tle;
    }

    // Static helper for a quick TLE result
    public static ExecutionResult tle() {
        return new ExecutionResult(1, "Time Limit Exceeded", true);
    }

    // Static helper for success (Exit code 0)
    public static ExecutionResult success(String output) {
        return new ExecutionResult(0, output, false);
    }

    // Static helper for runtime/compilation errors (Exit code 1)
    public static ExecutionResult error(String output) {
        return new ExecutionResult(1, output, false);
    }

    // --- Getters ---

    public String getOutput() {
        return output;
    }

    public boolean isTle() {
        return tle;
    }

    public int getExitCode() {
        return exitCode;
    }

    /**
     * Helper to check if a runtime error occurred.
     * In most systems, a non-zero exit code (that isn't a TLE) is an error.
     */
    public boolean isRuntimeError() {
        return exitCode != 0 && !tle;
    }
}