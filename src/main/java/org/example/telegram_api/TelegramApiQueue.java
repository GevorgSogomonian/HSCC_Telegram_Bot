package org.example.telegram_api;

import org.example.dto.ChatBotRequest;
import org.example.dto.ChatBotResponse;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class TelegramApiQueue {

    private final BlockingQueue<ChatBotRequest> requestQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<ChatBotResponse> responseQueue = new LinkedBlockingQueue<>();

    public void addRequest(ChatBotRequest chatBotRequest) {
        requestQueue.offer(chatBotRequest);
    }

    public void addResponse(ChatBotResponse chatBotResponse) {
        responseQueue.offer(chatBotResponse);
    }

    public ChatBotRequest takeRequest() throws InterruptedException {
        return requestQueue.take();
    }

    public ChatBotResponse takeResponse() throws InterruptedException {
        return responseQueue.take();
    }
}