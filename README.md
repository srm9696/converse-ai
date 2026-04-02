# converse-ai

Spring Boot service that accepts authenticated chat requests over HTTP, simulates an LLM with configurable latency and failures, stores full conversation history in H2 (JPA), and protects duplicate submissions with `Idempotency-Key` semantics.

## Prerequisites

- Java 17+ (the project targets Java 17 in the Maven `pom.xml`)
- Maven 3.9+

## How to run

From the project root:

```bash
mvn spring-boot:run
```

The API listens on port `8080` by default.

### Seeded evaluator credentials

For local runs the database is seeded with:

- **API key (plaintext):** `test-api-key-001`  
  Send as header: `X-Api-Key: test-api-key-001`

Seeded rows come from `src/main/resources/data.sql`, with a programmatic fallback in `DatabaseSeeder` if SQL init is skipped.

### H2 console (optional)

- Console path: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:converseai`
- Username: `sa` (default)
- Password: *(leave empty)*

### Example requests

#### Postman setup (recommended)

For **every POST request** in Postman (create conversation + append message), generate a fresh `Idempotency-Key` so you don’t accidentally replay an older stored response.

Add this **Pre-request Script** to each POST request:

```javascript
pm.environment.set("IDEMPOTENCY_KEY", pm.variables.replaceIn("{{$guid}}"));
```

Then add these headers to the request:

- `X-Api-Key`: `test-api-key-001`
- `Idempotency-Key`: `{{IDEMPOTENCY_KEY}}`

Create a conversation (returns `201 Created`):

```bash
curl -sS -X POST 'http://localhost:8080/api/conversations' \
  --header 'Content-Type: application/json' \
  --header 'X-Api-Key: test-api-key-001' \
  --header 'Idempotency-Key: {{IDEMPOTENCY_KEY}}' \
  --data '{"message":"Hello"}'
```

Append a message:

```bash
curl -sS -X POST "http://localhost:8080/api/conversations/${CONVERSATION_ID}/messages" \
  --header 'Content-Type: application/json' \
  --header 'X-Api-Key: test-api-key-001' \
  --header 'Idempotency-Key: {{IDEMPOTENCY_KEY}}' \
  --data '{"message":"Follow-up"}'
```
Note: use a fresh `Idempotency-Key` value per request, otherwise the server may replay an earlier assistant message.

Fetch history (messages ordered oldest → newest):

```bash
curl -sS -X GET "http://localhost:8080/api/conversations/${CONVERSATION_ID}/messages" \
  --header 'X-Api-Key: test-api-key-001'
```

## Project layout (layered)

| Package | Role |
| --- | --- |
| `com.sachin.converse_ai.controller` | HTTP adapters / REST controllers |
| `com.sachin.converse_ai.dto` | Request and response payloads |
| `com.sachin.converse_ai.service` | Application services (use-case orchestration) |
| `com.sachin.converse_ai.dao` | JPA entities (persistence model) |
| `com.sachin.converse_ai.repository` | Spring Data repositories |
| `com.sachin.converse_ai.client` | `LlmClient` integration (mock + resilience wrapper) |
| `com.sachin.converse_ai.security` | API key authentication filter + security configuration |
| `com.sachin.converse_ai.exception` | Centralized `@RestControllerAdvice` error handling |
| `com.sachin.converse_ai.logging` | Request-scoped diagnostic MDC filter |

## Design decisions

1. **API key now, JWT later:** Spring Security authenticates `X-Api-Key` against bcrypt hashes in the `api_key` table and sets the authenticated principal to the user UUID. Replacing this with JWT is intended to be an infrastructure swap (resource-server configuration + different authentication filter) without rewriting application services.

2. **Idempotency:** `Idempotency-Key` is scoped per user and stored in `idempotent_request`. Successful responses are replayed for duplicates; in-flight duplicates return `409 Conflict`; keys in `FAILED` state **do not** automatically retry work.

   **A FAILED idempotency key is not retried automatically. The client must submit a new `Idempotency-Key` to retry.** (See `IdempotencyService` for the source-of-truth comment.)

3. **Reliability:** `ResilientLlmClient` composes Resilience4j **retry**, **time limiter**, and **circuit breaker** around the mock LLM. Timeouts surface as `504 Gateway Timeout`, an open circuit as `503 Service Unavailable`, and other failures as `502 Bad Gateway` where appropriate.

4. **ORM boundaries:** Controllers translate HTTP concerns; `ConversationApplicationService` owns a single transaction per use-case; idempotency mutations that must survive business failures use `REQUIRES_NEW` boundaries so rolled-back chat rows do not erase durable idempotency state.

5. **CSRF:** Disabled because this service is a JSON-only API authenticated via API keys, not browser cookie sessions.

## Assumptions

- `userId` is a UUID primary key and is trusted once the API key resolves to a row.
- The LLM is mocked in-process; external HTTP latency/back-pressure is not simulated beyond the configured sleep + failure rate.
- H2 in-memory storage is sufficient for the assignment; swapping to PostgreSQL is mostly configuration + dialect tuning.

## Running tests

```bash
mvn test
```
