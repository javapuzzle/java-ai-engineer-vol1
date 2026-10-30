# Chapter 14 — Not Just Words Anymore

*The input side just got wider: a photo now joins the text in the same prompt stream, handled by the same model the same way it always handled words.*

Companion code for **Chapter 14** of *Java AI Engineer — Sarah's Journey at Acme Retail*.

## What this module demonstrates

A customer uploads a photo and asks "do you have this?" — a question a text-only assistant cannot answer because it has no way to look at the picture. This module adds a `/api/visual-search` endpoint that attaches the uploaded image directly to the prompt alongside the customer's text question, using Spring AI's multimodal `UserMessage` builder (`.text(...)` plus `.media(MimeType, Resource)`). The image does not go through a separate vision pipeline; it is packaged as `Media` and sent in the same `ChatClient` call that Chapter 9 introduced for text-only prompts, to the same vision-capable model. The response is grounded against Acme Retail's real category and color vocabulary (`CatalogVisualContextProvider`) rather than letting the model invent a plausible-sounding match, the same discipline Chapter 11 applied to text grounding. The result is deliberately descriptive only — no SKU, price, or stock confirmation — because a vision-capable model can describe what it sees but cannot see Acme Retail's inventory system.

## Prerequisites

- Java 21
- Maven 3.9+ (or the wrapper, if added to this module)
- `org.springframework.ai:spring-ai-starter-model-openai` — the OpenAI chat starter, configured with a vision-capable model (`gpt-4o` in `application.yml`); a non-vision model such as `gpt-3.5-turbo` would accept the request but the model itself would not be able to use the attached image
- An `OPENAI_API_KEY` environment variable set to a valid OpenAI API key

## Run it

```bash
export OPENAI_API_KEY=sk-...
cd volume1_2/code/chapter-14
mvn spring-boot:run
```

The application starts on port `8080`. The image is submitted as a base64-encoded byte array inside the JSON request body (the `imageBytes` field is a Java `byte[]`, which Jackson serializes/deserializes as a base64 string) — not as a multipart file upload and not as an image URL:

```bash
IMAGE_B64=$(base64 -i shoe-photo.jpg)

curl -X POST http://localhost:8080/api/visual-search \
  -H "Content-Type: application/json" \
  -d "{
    \"question\": \"Do you have this in another color?\",
    \"imageMimeType\": \"image/jpeg\",
    \"imageBytes\": \"${IMAGE_B64}\"
  }"
```

## Project structure

```
src/main/java/com/acme/retail/ch14/
├── Chapter14Application.java                    # Spring Boot entry point
└── visualsearch/
    ├── CatalogVisualContextProvider.java         # Real category/color vocabulary handed to the model
    ├── VisualMatchResult.java                    # Structured response record (description, category, color, suggestion)
    ├── VisualSearchChatClientConfig.java         # Builds the shared multimodal-capable ChatClient bean
    ├── VisualSearchController.java               # POST /api/visual-search -- attaches image + text to one prompt
    └── VisualSearchRequest.java                  # Request record: question, imageBytes, imageMimeType
```

## Examples in this module

### com.acme.retail.ch14.Chapter14Application

Standard `@SpringBootApplication` entry point. No chapter-specific logic; it exists so the module boots as a self-contained Spring Boot application, independent of every other chapter's build.

### com.acme.retail.ch14.visualsearch.VisualSearchChatClientConfig

Declares the `ChatClient` bean used for visual search:

```java
@Bean
ChatClient visualSearchChatClient(ChatClient.Builder chatClientBuilder) {
    return chatClientBuilder.build();
}
```

There is no special "vision mode" to switch on here — it is the same builder-derived `ChatClient` shape used for text-only prompts since Chapter 9. Multimodal capability comes entirely from (a) the model configured in `application.yml` being vision-capable, and (b) the caller attaching a `Media` object to the user message at call time — the client itself is unaware in advance whether a given call will include an image.

### com.acme.retail.ch14.visualsearch.VisualSearchController

The REST endpoint that receives the customer's question and photo, builds the multimodal prompt, and returns a structured match. The image is wrapped as a `ByteArrayResource` and attached via `.media(MimeType, Resource)` on the same `UserSpec` that carries the text:

```java
MimeType mimeType = MimeTypeUtils.parseMimeType(request.imageMimeType());
ByteArrayResource image = new ByteArrayResource(request.imageBytes());

return chatClient.prompt()
        .system(systemPrompt)
        .user(user -> user
                .text(request.question())
                .media(mimeType, image))
        .call()
        .entity(VisualMatchResult.class);
```

The system prompt is built by interpolating `CatalogVisualContextProvider.summary()` into a template that instructs the model to describe only what it can see, to stay within Acme Retail's real categories/colors, and to admit uncertainty rather than guess. `.entity(VisualMatchResult.class)` then maps the model's answer onto the structured response record.

### com.acme.retail.ch14.visualsearch.CatalogVisualContextProvider

Supplies the fixed list of categories (Running Shoes, Hiking Boots, Backpacks, Accessories) and colors (Black, Grey, Navy, Forest Green, Red, White) that Acme Retail actually carries, plus an instruction to say so honestly when a photo doesn't clearly match any of them. `VisualSearchController` interpolates this text directly into the system prompt string, so it sits in the same message as the model's instructions — the image is matched against this catalog vocabulary rather than against the model's own general-purpose visual knowledge, mirroring the grounding pattern `InventoryLookupService` established for text in Chapter 11.

### com.acme.retail.ch14.visualsearch.VisualSearchRequest

The inbound request record: `question` (the customer's text), `imageBytes` (raw photo bytes, serialized as base64 in JSON), and `imageMimeType` (e.g. `"image/jpeg"`). This is the exact shape `VisualSearchController` deserializes from the POST body.

### com.acme.retail.ch14.visualsearch.VisualMatchResult

The outbound response record: `visualDescription`, `likelyCategory`, `likelyColor`, and `suggestionForCustomer`. Deliberately excludes SKU, price, and stock count — this endpoint reports what the model can see in the photo, not what Acme Retail's inventory system currently holds.

## API Endpoints

| Method | Path | Request | Response |
|---|---|---|---|
| POST | `/api/visual-search` | JSON body: `{ "question": string, "imageBytes": string (base64), "imageMimeType": string }` | JSON `VisualMatchResult`: `{ "visualDescription": string, "likelyCategory": string, "likelyColor": string, "suggestionForCustomer": string }` |

```bash
IMAGE_B64=$(base64 -i shoe-photo.jpg)

curl -X POST http://localhost:8080/api/visual-search \
  -H "Content-Type: application/json" \
  -d "{
    \"question\": \"Do you have this in another color?\",
    \"imageMimeType\": \"image/jpeg\",
    \"imageBytes\": \"${IMAGE_B64}\"
  }"
```

## Tests

- `CatalogVisualContextProviderTest` — verifies that `summary()` lists Acme Retail's real categories (Running Shoes, Hiking Boots, Backpacks, Accessories) and colors (Black, Navy), and that it instructs the model to say so honestly rather than guess when a photo doesn't match.

```bash
mvn test
```

## Configuration

From `src/main/resources/application.yml`:

- `spring.ai.openai.api-key: ${OPENAI_API_KEY:demo-key}` — reads the OpenAI key from the `OPENAI_API_KEY` environment variable at startup; without a real key, calls to the visual search endpoint will fail against the live API.
- `spring.ai.openai.chat.options.model: gpt-4o` — pins the chat model to a vision-capable model; this is not optional here, since attaching `.media(...)` to a prompt for a non-vision model would send image data the model cannot interpret.
- `server.port: 8080` — the HTTP port the controller listens on.
- `logging.level.org.springframework.ai: INFO` — keeps Spring AI's request/response logging at a readable level for following what gets sent to the model during development.
