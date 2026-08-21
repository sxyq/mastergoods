package com.zhihuiji.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZhihuijiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhihuijiBackendApplication.class, args);
    }
}
