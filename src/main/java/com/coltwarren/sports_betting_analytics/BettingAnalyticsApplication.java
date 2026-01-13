package com.coltwarren.sports_betting_analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BettingAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BettingAnalyticsApplication.class, args);
    }

}
