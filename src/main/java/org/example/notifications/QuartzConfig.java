//package org.example.notifications;
//
//import org.quartz.spi.JobFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.quartz.SchedulerFactoryBean;
//import org.springframework.scheduling.quartz.SpringBeanJobFactory;
//
//import javax.sql.DataSource;
//
//@Configuration
//public class QuartzConfig {
//
//    @Bean
//    public JobFactory jobFactory() {
//        // Здесь можно добавить любую пользовательскую логику, если нужно
//        return new SpringBeanJobFactory();
//    }
//
//    @Bean
//    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource, JobFactory jobFactory) {
//        SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
//        factoryBean.setDataSource(dataSource);
//        factoryBean.setJobFactory(jobFactory); // Устанавливаем кастомный JobFactory, если нужно
//        factoryBean.setOverwriteExistingJobs(true); // Перезаписывать задачи при рестарте
//        factoryBean.setAutoStartup(true); // Запускать автоматически
//        return factoryBean;
//    }
//}