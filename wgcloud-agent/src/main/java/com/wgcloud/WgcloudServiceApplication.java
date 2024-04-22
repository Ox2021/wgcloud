package com.wgcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class WgcloudServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WgcloudServiceApplication.class, args);
    }

    // 定义一个RestTemplate Bean，用于进行HTTP请求
    @Bean
    public RestTemplate restTemplate() {
        // 创建一个支持UTF-8编码的StringHttpMessageConverter
        StringHttpMessageConverter m = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        // 使用RestTemplateBuilder创建RestTemplate实例，并添加自定义的消息转换器
        RestTemplate restTemplate = new RestTemplateBuilder().additionalMessageConverters(m).build();
        // 返回RestTemplate实例
        return restTemplate;
    }

    // 定义一个TaskScheduler Bean，用于调度任务
    @Bean
    public TaskScheduler taskScheduler() {
        // 创建一个ThreadPoolTaskScheduler实例，用于调度任务
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        // 设置线程池大小为10
        taskScheduler.setPoolSize(10);
        // 返回ThreadPoolTaskScheduler实例
        return taskScheduler;
    }

}
