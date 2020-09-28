package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class AlertRabbit {
    private static Properties properties = new Properties();

    public static void main(String[] args) throws InterruptedException {
        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(new File("./src/main/resources/rabbit.properties")))) {
            properties.load(bufferedInputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            List<Long> store = new ArrayList<>();
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("store", store);
            data.put("connection", getConnection());
            JobDetail job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(Integer.parseInt(properties.getProperty("rabbit.interval")))
                    .repeatForever();
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
            System.out.println(store);
        } catch (SchedulerException se) {
            se.printStackTrace();
        }
    }

    public static class Rabbit implements Job {
        public Rabbit() {
            System.out.println(hashCode());
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Rabbit runs here ...");
            List<Long> store = (List<Long>) context.getJobDetail().getJobDataMap().get("store");
            store.add(System.currentTimeMillis());
            JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
            insertTimeIntoDB((Connection) jobDataMap.get("connection"), new Timestamp(store.get(0)));
        }
    }

    private static Connection getConnection() {
        try {
            return DriverManager.getConnection(properties.getProperty("jdbc.url"), properties.getProperty("jdbc.username"), properties.getProperty("jdbc.password"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void insertTimeIntoDB(Connection connection, Timestamp time) {
        try(PreparedStatement preparedStatement = connection.prepareStatement("insert into rabbit (createdDate) values (?)")) {
            preparedStatement.setTimestamp(1, time);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
