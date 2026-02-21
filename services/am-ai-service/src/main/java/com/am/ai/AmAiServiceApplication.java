package com.am.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.am.ai", "com.am.market.client.config", "com.am.common"})
public class AmAiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmAiServiceApplication.class, args);
    }
}
