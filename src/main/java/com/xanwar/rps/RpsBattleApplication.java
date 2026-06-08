package com.xanwar.rps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RpsBattleApplication {
    public static void main(String[] args) {
        SpringApplication.run(RpsBattleApplication.class, args);
    }
}
