package org.example.data_classes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ChatBotResponse {
    private final Long chatId;
    private final PartialBotApiMethod<?> method;

    //указывается только для SendPhoto
    private Long eventId;
}