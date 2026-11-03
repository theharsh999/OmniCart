package com.omnicart.assistant;

import com.omnicart.dto.Dtos.ChatRequest;
import com.omnicart.dto.Dtos.ChatResponse;
import com.omnicart.entity.User;
import com.omnicart.exception.ApiException;
import com.omnicart.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing the AI Assistant chatbot endpoint.
 */
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final AuthService authService;

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to chat with the assistant");
        }
        User currentUser = authService.currentUser(auth.getName());
        return assistantService.processChat(currentUser, request);
    }
}
