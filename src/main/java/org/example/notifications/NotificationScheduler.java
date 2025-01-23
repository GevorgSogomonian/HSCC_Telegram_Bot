//package org.example.notifications;
//
//import lombok.RequiredArgsConstructor;
//import org.quartz.*;
//import org.quartz.impl.StdSchedulerFactory;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.Date;
//
//@Service
//@RequiredArgsConstructor
//public class NotificationScheduler {
//
//    private final Scheduler scheduler;
//
//    public NotificationScheduler() throws SchedulerException {
//        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
//        this.scheduler.start();
//    }
//
//    public void scheduleNotification(String jobName, String groupName, LocalDateTime scheduleTime, Class<? extends Job> jobClass, JobDataMap jobDataMap) throws SchedulerException {
//        // Преобразование LocalDateTime в Date
//        Date triggerStartTime = Date.from(scheduleTime.atZone(ZoneId.systemDefault()).toInstant());
//
//        JobDetail jobDetail = JobBuilder.newJob(jobClass)
//                .withIdentity(jobName, groupName)
//                .usingJobData(jobDataMap)
//                .build();
//
//        Trigger trigger = TriggerBuilder.newTrigger()
//                .withIdentity(jobName + "Trigger", groupName)
//                .startAt(triggerStartTime)
//                .build();
//
//        scheduler.scheduleJob(jobDetail, trigger);
//    }
//
//    public void cancelNotification(String jobName, String groupName) throws SchedulerException {
//        JobKey jobKey = new JobKey(jobName, groupName);
//        if (scheduler.checkExists(jobKey)) {
//            scheduler.deleteJob(jobKey);
//        }
//    }
//
//    public void shutdown() throws SchedulerException {
//        if (!scheduler.isShutdown()) {
//            scheduler.shutdown();
//        }
//    }
//}