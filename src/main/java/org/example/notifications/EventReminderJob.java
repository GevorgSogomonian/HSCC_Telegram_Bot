//package org.example.notifications;
//
//import lombok.RequiredArgsConstructor;
//import org.example.telegram.api.TelegramSender;
//import org.quartz.Job;
//import org.quartz.JobExecutionContext;
//import org.quartz.JobExecutionException;
//import org.springframework.stereotype.Service;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//
//import java.time.LocalDateTime;
//
//@Service
//@RequiredArgsConstructor
//public class EventReminderJob implements Job {
//    private final TelegramSender telegramSender;
//
//    @Override
//    public void execute(JobExecutionContext context) throws JobExecutionException {
//        Long chatId = Long.parseLong(context.getJobDetail().getJobDataMap().getString("message"));
//        telegramSender.sendText(chatId, SendMessage.builder()
//                        .chatId(chatId)
//                        .text("Уведомление: " + chatId + " | Время: " + LocalDateTime.now())
//                .build());
//        System.out.println("Уведомление: " + chatId + " | Время: " + LocalDateTime.now());
//    }
//}