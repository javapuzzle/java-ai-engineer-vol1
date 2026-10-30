<div align="center">

# ☕ Java AI Engineer — Volume 1: AI Foundations & Spring AI
### *From Enterprise Java to Modern AI Engineering*

[![Volume](https://img.shields.io/badge/Volume-1%20of%206-blue?style=for-the-badge)](https://javapuzzle.com)
[![Java](https://img.shields.io/badge/Java-21+-orange?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4+-brightgreen?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)

**Part of the 6-volume [Java AI Engineer](https://javapuzzle.com) series by Java Puzzle**

</div>

---

## 📖 About This Volume

Sarah joins Acme Retail as a Java developer tasked with building a product API. What starts as a simple REST endpoint turns into a full systems transformation as she integrates modern Large Language Models into enterprise Java workflows. 

Across 20 rigorous, runnable chapters, Volume 1 bridges core Java engineering with Spring AI:
- **Core Systems Foundations**: Concurrency, Java 21 Virtual Threads, database persistence, resilience, and caching.
- **AI Foundations**: Tokenomics, inference latency and costs, neural architectures & transformers, structured prompting.
- **Spring AI Architecture**: Conversational memory, reactive response streaming, multimodal inputs, model portability, and guardrails.

---

## 🗺️ Chapter & Code Index

| Chapter | Title | Focus Area / Key Mechanism |
|:---:|---|---|
| **01** | `chapter-01` | **Sarah's First Day at Acme Retail** — Clean layering: Controller, Service, Repository |
| **02** | `chapter-02` | **It Works, But It's Slow** — High concurrency with Java 21 Virtual Threads & thread diagnostics |
| **03** | `chapter-03` | **When the Database Says No** — Caching strategies, write-through vs read-through, pool tuning |
| **04** | `chapter-04` | **Everyone Keeps Saying "AI"** — Deterministic vs probabilistic computing & AI engineering mindset |
| **05** | `chapter-05` | **It Didn't Start With ChatGPT** — Evolution from classical ML & feature engineering to Deep Learning |
| **06** | `chapter-06` | **Alex Opens the Black Box** — Neural networks, self-attention, and transformer mechanics |
| **07** | `chapter-07` | **Sarah Gets the AI Bill** — Token economics, context window management, and cost projections |
| **08** | `chapter-08` | **Words Are Code Now** — Prompt engineering as typed contracts, structured outputs, JSON schema |
| **09** | `chapter-09` | **Spring Gets an AI Accent** — Spring AI architecture, `ChatClient`, `ChatModel`, and abstraction layers |
| **10** | `chapter-10` | **Sarah Meets AI** — First end-to-end integration of Spring AI in the Acme Retail catalog service |
| **11** | `chapter-11` | **The AI Is Wrong** — Grounding model responses with catalog data to prevent hallucinations |
| **12** | `chapter-12` | **Customers Don't Want to Wait** — Reactive streaming with Server-Sent Events (SSE) & Flux |
| **13** | `chapter-13` | **Sarah Forgot the Customer** — Conversational state & session memory management |
| **14** | `chapter-14` | **Not Just Words Anymore** — Multimodal vision pipelines analyzing product images in Java |
| **15** | `chapter-15` | **Sarah Builds an Escape Hatch** — Model portability across OpenAI, Ollama, and Anthropic |
| **16** | `chapter-16` | **Production Doesn't Care About Your Demo** — Timeouts, retries, circuit breakers, and fallback gateways |
| **17** | `chapter-17` | **Something Is Wrong, But Nobody Knows What** — Observability, Micrometer AI metrics, and tracing |
| **18** | `chapter-18` | **The AI Should Never Have Seen That** — AI security, prompt injection defenses, input sanitization |
| **19** | `chapter-19` | **Can We Trust Our Tests?** — Automated evaluation pipelines, assertions on non-deterministic outputs |
| **20** | `chapter-20` | **Launch Day (Capstone)** — The complete, production-hardened Acme Retail shopping assistant |

---

## 🛠️ Tech Stack & Prerequisites

- **Java**: 21 LTS or newer
- **Framework**: Spring Boot 3.4+, Spring AI 1.0+
- **Build Tool**: Maven 3.9+
- **Containerization**: Docker Compose for local dependencies

## 🚀 Building & Running Chapter Code

Build all chapters from the root pom:
```bash
mvn clean test
```

Step into any individual chapter to run tests or launch the Spring Boot service:
```bash
cd chapter-01
mvn spring-boot:run
```

---

<div align="center">
<sub>© Java Puzzle. All rights reserved. Visit <a href="https://javapuzzle.com">javapuzzle.com</a> to explore the entire 6-volume series.</sub>
</div>
