# Real-Time Collaborative Whiteboard - Backend

**Student:** Srinidhi Vutkoori (X25173243)
**Module:** Cloud DevOpsSec (H9CDOS)

---

## Description

This is the backend service for the Real-Time Collaborative Whiteboard application. It provides a RESTful API and WebSocket endpoints that allow multiple users to simultaneously draw on a shared canvas, with full version history tracking so any past state of a whiteboard can be restored.

Key responsibilities of the backend:

- User registration and authentication via JWT-based stateless sessions
- Whiteboard CRUD operations persisted to a PostgreSQL database
- Real-time drawing event broadcast to all connected collaborators over WebSocket (STOMP protocol)
- Version history management - every saved state is stored and can be diffed or restored
- Statistical analysis of drawing activity using Apache Commons Math
- REST API documented interactively via Swagger / OpenAPI 3

---

## Tech Stack

| Component | Technology |
|---|---|
| Application framework | Spring Boot 3 |
| Language | Java 17 |
| Database | PostgreSQL |
| Authentication | Spring Security 6 + JWT (JJWT) |
| Real-time messaging | WebSocket with STOMP |
| Math / statistics | Apache Commons Math |
| Build tool | Maven |
| Static analysis | SpotBugs, PMD, JaCoCo |
| Security scanning | Semgrep |
| API documentation | SpringDoc OpenAPI (Swagger UI) |
| Infrastructure | AWS EC2 + RDS, provisioned with Terraform |
| CI/CD | GitHub Actions |

---

## Prerequisites

Before running the backend locally, ensure the following are installed:

- **Java 17** - Amazon Corretto 17 or Eclipse Temurin 17 recommended
- **Maven 3.8+** - used for building, testing, and running the application
- **PostgreSQL 15+** - the relational database for persisting all application data

---

## Database Setup

Connect to PostgreSQL as a superuser and run the following commands to create the database and user expected by the default configuration:

```sql
-- Create the application database
CREATE DATABASE whiteboard_db;

-- The application connects with the following credentials by default.
-- In production these should be supplied via environment variables,
-- not hard-coded in application.properties.
-- Default username: default
-- Default password: root
```

The application uses Spring Data JPA with `spring.jpa.hibernate.ddl-auto=update`, so Hibernate will create or update the schema automatically on first start.

---

## How to Run

### 1. Install dependencies and run all tests

```bash
mvn clean install
```

### 2. Start the application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

To run with a specific profile (for example, production settings):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## API Documentation

Swagger UI provides an interactive browser for all REST endpoints, request/response schemas, and authentication requirements.

**URL:** `http://localhost:8080/swagger-ui.html`

The raw OpenAPI 3 specification in JSON format is available at:

`http://localhost:8080/v3/api-docs`

---

## Static Analysis

All static analysis tools are bound to the Maven `verify` lifecycle phase and run automatically during `mvn clean install`.

To run analysis explicitly:

```bash
# Runs compilation, all unit tests, SpotBugs, PMD, and generates JaCoCo coverage
mvn verify

# Run SpotBugs bug-pattern analysis in isolation
mvn spotbugs:check

# Run PMD code-quality rules in isolation
mvn pmd:check

# Generate the HTML JaCoCo coverage report
mvn jacoco:report
```

Reports are written to `target/` after each run:

- SpotBugs XML: `target/spotbugsXml.xml`
- PMD XML: `target/pmd.xml`
- JaCoCo HTML report: `target/site/jacoco/index.html`

---

## Security Scanning

[Semgrep](https://semgrep.dev) is used for static application security testing (SAST). It scans the Java source code for known vulnerability patterns mapped to the OWASP Top Ten and other security rule sets.

### Run Semgrep locally

Install Semgrep (requires Python 3.8+):

```bash
pip install semgrep
```

Run the Java and security audit rule packs against the source tree:

```bash
semgrep --config=p/java --config=p/security-audit --config=p/owasp-top-ten src/
```

Semgrep also runs automatically in the CI pipeline on every push and pull request (see `.github/workflows/ci-cd.yml`).

---

## Project Structure

```
backend/
  src/
    main/
      java/com/whiteboard/
        config/          # Spring Security, WebSocket, OpenAPI configuration beans
        controller/      # REST controllers for auth, whiteboards, and version history
        dto/             # Request and response data transfer objects
        entity/          # JPA entity classes mapped to PostgreSQL tables
        repository/      # Spring Data JPA repository interfaces
        security/        # JWT utility, filter, and UserDetailsService implementation
        service/         # Business logic layer - whiteboards, versions, statistics
        websocket/       # STOMP message handlers and drawing event models
      resources/
        application.properties       # Default (local) configuration
        application-prod.properties  # Production configuration (reads env vars)
    test/
      java/com/whiteboard/          # JUnit 5 unit and integration tests
  terraform/
    main.tf         # AWS provider, VPC, subnets, internet gateway
    ec2.tf          # EC2 instance and security group
    rds.tf          # PostgreSQL RDS instance and security group
    s3.tf           # S3 bucket for artifacts and backups
    variables.tf    # Input variables with defaults
    outputs.tf      # Output values (IPs, endpoints, URLs)
  .github/
    workflows/
      ci-cd.yml     # GitHub Actions CI/CD pipeline
  pom.xml           # Maven build, dependency, and plugin configuration
  README.md         # This file
  .gitignore        # VCS exclusion rules
```

---

## CI/CD Pipeline

The GitHub Actions pipeline defined in `.github/workflows/ci-cd.yml` has two clearly separated stages:

### Continuous Integration (CI)

Triggered on every push and every pull request targeting `main`.

1. Check out source code
2. Set up Java 17 (Eclipse Temurin)
3. Build the project and run all unit tests (`mvn clean verify`)
4. Run SpotBugs static bug analysis
5. Run PMD code quality analysis
6. Generate JaCoCo code coverage report
7. Run Semgrep security vulnerability scan (Java, security-audit, OWASP Top Ten rule packs)
8. Upload SpotBugs, PMD, JaCoCo, and Surefire reports as downloadable artifacts

### Continuous Deployment (CD)

Triggered only on a push to the `main` branch, and only after the CI job passes.

1. Build the deployment JAR (`mvn clean package -DskipTests`)
2. Copy the JAR to the AWS EC2 instance via SCP
3. SSH into the EC2 instance, stop the old service, replace the JAR, and restart
4. Perform a smoke test against the `/actuator/health` endpoint to confirm the deployment succeeded

Infrastructure is provisioned separately using Terraform (`terraform/`) before the pipeline first runs.
