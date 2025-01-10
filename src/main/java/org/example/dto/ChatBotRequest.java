package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;

@Data
@Builder
public class ChatBotRequest {
    private final Long chatId;
    private final BotApiMethod<?> method;

    public ChatBotRequest(Long chatId, BotApiMethod<?> method) {
        this.chatId = chatId;
        this.method = method;
    }
}