package org.example.dto;

import lombok.Builder;
import lombok.Data;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;

@Data
@Builder
public class ChatBotResponse {
    private final Long chatId;
    private final PartialBotApiMethod<?> method;

    public ChatBotResponse(Long chatId, PartialBotApiMethod<?> method) {
        this.chatId = chatId;
        this.method = method;
    }
}