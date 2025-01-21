//package org.example.notifications;
//
//import lombok.RequiredArgsConstructor;
//import org.quartz.Job;
//import org.quartz.JobExecutionContext;
//import org.quartz.JobExecutionException;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class NotificationJob implements Job {
//
//    @Override
//    public void execute(JobExecutionContext context) throws JobExecutionException {
//        String message = context.getJobDetail().getJobDataMap().getString("message");
//        Long userId = context.getJobDetail().getJobDataMap().getLong("userId");
//        // Реализуйте отправку уведомления пользователю
//        System.out.printf("Отправка уведомления пользователю с ID %d: %s%n", userId, message);
//    }
//}