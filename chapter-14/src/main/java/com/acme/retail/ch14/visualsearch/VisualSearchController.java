package com.acme.retail.ch14.visualsearch;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Acme Retail's visual search endpoint.
 *
 * <p>A text-only assistant (Chapters 10-13) has no way to act on a customer
 * photo -- it can only ask the customer to describe the item in words,
 * which loses information a picture conveys instantly. This controller
 * attaches the uploaded image directly to the prompt, alongside the
 * customer's question, using Spring AI's multimodal support -- the same
 * portable {@code ChatClient} from Chapter 09, now handling a second kind
 * of input.
 */
@RestController
@RequestMapping("/api/visual-search")
public class VisualSearchController {

    private static final String SYSTEM_PROMPT = """
            You are Acme Retail's visual shopping assistant. A customer has
            uploaded a photo and asked a question about it.

            %s

            Describe only what you can actually see in the photo. Do not
            claim to know current stock or price -- you cannot see Acme
            Retail's inventory system. If you cannot confidently match the
            item to one of the categories or colors above, say so.
            """;

    private final ChatClient chatClient;
    private final CatalogVisualContextProvider catalogContext;

    public VisualSearchController(ChatClient visualSearchChatClient,
                                   CatalogVisualContextProvider catalogContext) {
        this.chatClient = visualSearchChatClient;
        this.catalogContext = catalogContext;
    }

    @PostMapping
    public VisualMatchResult search(@RequestBody VisualSearchRequest request) {
        String systemPrompt = SYSTEM_PROMPT.formatted(catalogContext.summary());
        MimeType mimeType = MimeTypeUtils.parseMimeType(request.imageMimeType());
        ByteArrayResource image = new ByteArrayResource(request.imageBytes());

        return chatClient.prompt()
                .system(systemPrompt)
                .user(user -> user
                        .text(request.question())
                        .media(mimeType, image))
                .call()
                .entity(VisualMatchResult.class);
    }
}
