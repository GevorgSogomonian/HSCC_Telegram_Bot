package org.example.data_classes.dto;

import lombok.Data;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;

@Data
public class ChatBotRequest {
    private final Long chatId;
    private final BotApiMethod<?> method;

    public ChatBotRequest(Long chatId, BotApiMethod<?> method) {
        this.chatId = chatId;
        this.method = method;
    }
}