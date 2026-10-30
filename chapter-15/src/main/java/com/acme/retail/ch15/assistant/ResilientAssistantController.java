package com.acme.retail.ch15.assistant;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resilient-assistant")
public class ResilientAssistantController {

    private final ResilientAssistantService assistantService;

    public ResilientAssistantController(ResilientAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/ask")
    public AssistantAnswer ask(@RequestBody AskRequest request) {
        return assistantService.ask(request.question());
    }

    public record AskRequest(@NotBlank String question) {
    }
}
