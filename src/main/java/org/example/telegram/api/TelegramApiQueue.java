package org.example.telegram.api;

import org.example.dto.ChatBotRequest;
import org.example.dto.ChatBotResponse;
import org.springframework.stereotype.Component;

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