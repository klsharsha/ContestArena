
# CompileEngine (Online Compiler System) (Backend + Frontend)

A secure, asynchronous **online compiler system** that executes untrusted user code inside **isolated Docker containers**, enforcing **time and memory limits**, and supporting **multiple programming languages**.

This project focuses on **backend system design, sandboxing, and async processing**, rather than deployment or UI polish.

---

## 🚀 Features

* ✅ Multi-language support
  * Java
  * Python
  * C
  * C++
  * JavaScript
* 🔄 Asynchronous execution using RabbitMQ and Kafka
* 🐳 Secure Docker-based sandbox per execution
* ⏱️ Time Limit Enforcement (TLE)
* 💾 Memory Limit Enforcement (MLE)
* 📥 STDIN / STDOUT support
* ❌ Compile-time & runtime error classification
* 🗄️ Persistent submission storage using MySQL

---

## 🏗️ High-Level Architecture

CompileEngine is built on an **Event-Driven Microservices Architecture** to decouple the API responsibilities from the heavy lifting of code execution.

1. **Client (React)** submits code via REST API.
2. **Submission Service** saves the submission to **MySQL** with status `PENDING`.
3. **Task Queue (RabbitMQ)** receives the Job ID to buffer execution requests.
4. **Execution Service** picks up the job and runs the code in a **Docker Sandbox** (language-specific runner).
5. **Event Bus (Kafka)** broadcasts the execution result (`SUCCESS`, `ERROR`, `TLE`, `MLE`).
6. **Submission Service** listens to Kafka and updates the database.
7. **Client** polls submission status asynchronously until completion.

Execution is **fully asynchronous** to ensure API responsiveness and system stability under load.

---

## 🔐 Security & Sandboxing

Each code execution runs inside a **short-lived Docker container** with strict isolation:

* `--network none` → no internet access
* `--memory` & `--memory-swap` → memory limits
* `--pids-limit` → process limit
* Temporary filesystem (auto-cleaned)
* No host access except mounted working directory

This design prevents:

* Infinite loops
* Fork bombs
* Network abuse
* Host filesystem access

---

## ⚙️ Tech Stack

### Backend
* Java 17
* Spring Boot
* Spring Data JPA
* RabbitMQ (Task Queue)
* Apache Kafka (Event Streaming)
* MySQL (Persistence)
* Docker (Sandboxing)

### Frontend
* React
* Fetch API (polling-based async updates)

---

## 📁 Repository Structure

```text
online-compiler/
├── submission-service/     # REST API, DB Management, Kafka Consumer
├── execution-service/      # RabbitMQ Consumer, Docker Management, Kafka Producer
├── frontend/               # React UI
├── dockercompilers/
│   ├── java-runner/
│   ├── python-runner/
│   ├── c-runner/
│   ├── cpp-runner/
│   └── javascript-runner/
├── infra/                  # Docker-compose (RabbitMQ, Kafka, Zookeeper, MySQL)
├── docs/
│   ├── execution-flow.md
│   └── security.md
└── README.md
```

---

## 🧪 How It Works (Execution Flow)

1. Client submits code via REST API.
2. Submission is saved to MySQL with status `PENDING`.
3. Job ID is pushed to RabbitMQ.
4. Consumer picks job and runs code in Docker.
5. Status transitions:
   `PENDING` → `RUNNING` → `SUCCESS` / `ERROR` / `TLE` / `MLE`
6. Execution Service publishes result to Kafka.
7. Submission Service consumes result and updates DB.
8. Client polls submission status asynchronously.

---

## ▶️ Running the Project Locally

### Prerequisites

* Java 17
* Maven
* Node.js
* Docker
* Local MySQL installed and running (or via Docker)

---

### 1️⃣ Start Docker Infrastructure (RabbitMQ + Kafka + Zookeeper)

```bash
cd infra
docker-compose up -d
```

* RabbitMQ UI: [http://localhost:15672](http://localhost:15672) (guest/guest)
* Kafka: `localhost:9092`

---

### 2️⃣ Configure MySQL (Local)

```sql
CREATE DATABASE compiler;
CREATE USER 'compiler'@'localhost' IDENTIFIED BY 'compiler';
GRANT ALL PRIVILEGES ON compiler.* TO 'compiler'@'localhost';
FLUSH PRIVILEGES;
```

---

### 3️⃣ Build Docker Runners

```bash
cd dockercompilers
docker build -t java-runner -f java-runner/Dockerfile .
docker build -t python-runner -f python-runner/Dockerfile .
```

---

### 4️⃣ Run Backend Services

Start the services in your IDE or via Maven:

```bash
# Terminal 1: Submission Service
cd submission-service
mvn spring-boot:run

# Terminal 2: Execution Service
cd execution-service
mvn spring-boot:run
```

Backend runs at: [http://localhost:8080](http://localhost:8080)

---

### 5️⃣ Run Frontend

```bash
cd frontend
npm install
npm start
```

Frontend runs at: [http://localhost:3000](http://localhost:3000)

---

## 🧪 Sample API Request

```json
POST /api/submit
{
  "language": "JAVA",
  "problemId": 1,
  "code": "public class Main { public static void main(String[] args){ System.out.println(\"Hello World\"); }}",
  "input": ""
}
```

---

## 🎥 Demo Video

A full walkthrough video demonstrating:

* Architecture
* Async execution
* Input handling
* Time & memory limits
* Error classification

📺 Demo Video Link: [[Youtube Link](https://youtu.be/3-FFXjSXU_I)]

---

## ❓ Why No Public Deployment?

This system executes **untrusted user code** and requires **Docker-level sandboxing**, which is not supported on most free PaaS platforms.

Instead, the project is designed to be:

* Fully reproducible locally
* Secure by default
* Easy to evaluate via source code and demo video

This is a **conscious architectural decision**, not a limitation.

---

## 📌 Future Improvements

* WebSocket-based live output streaming
* Per-language configurable limits
* Authentication & rate limiting
* Kubernetes-based runner scaling
* Leaderboard Service listening to Kafka events

---

## 🏁 Conclusion

This project demonstrates:

* Real-world distributed backend system design
* Secure sandboxed execution
* Event-driven architecture
* Docker & message broker integration

It is intended as a **learning-focused** rather than a hosted product.

---

### 👨‍💻 Author
KLSHarsha
