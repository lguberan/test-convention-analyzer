# test-convention-analyzer

Static analyzer for test naming conventions, patterns and metrics in Java/JUnit codebases.

## Overview

test-convention-analyzer scans Java test sources and extracts measurable statistics about:

- naming conventions (`testXxx`, camelCase, snake_case)
- BDD-style patterns (`given / when / then / should`)
- keyword usage (`throws`, `exception`, `null`, `empty`, etc.)
- most frequent tokens
- dominant naming patterns
- overall consistency across modules

The goal is to **quantify test readability and conventions**, not just lint them.

---

## Built-in preset repositories

The analyzer includes several well-known open-source projects as presets for benchmarking and experimentation:

- Spring Framework  
  https://github.com/spring-projects/spring-framework

- Spring Boot  
  https://github.com/spring-projects/spring-boot

- JUnit 5  
  https://github.com/junit-team/junit5

- Google Guava  
  https://github.com/google/guava

- Apache Commons Lang  
  https://github.com/apache/commons-lang

These projects are useful because they:

- contain thousands of tests
- use different naming styles
- mix classical and BDD-style conventions
- represent real-world enterprise Java codebases

---

## Note about OpenJDK

OpenJDK is intentionally not included.

OpenJDK:

- does not follow the standard Maven layout (`src/test/java`)
- does not use JUnit
- uses its own test harness (jtreg)

Because this tool targets Maven/JUnit-style projects, OpenJDK is not directly compatible.

---

## Requirements

- Java 17+ (21 recommended)
- Maven 3.9+

---

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/test-convention-analyzer-*.jar <path-to-project>
```

Example:

```bash
java -jar target/test-convention-analyzer-*.jar ~/dev/spring-framework
```

### Typical output

The analyzer produces:

- metrics tables
- pattern frequency reports
- top tokens
- percentages
- statistics usable in console or GUI (e.g., JTable)
- exportable data (CSV/JSON if enabled)

### Why

Test naming conventions strongly impact:

- readability
- maintainability
- onboarding speed
- long-term consistency

This tool makes those conventions **measurable and comparable across projects**.