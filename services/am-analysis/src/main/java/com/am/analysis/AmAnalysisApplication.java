package com.am.analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@EnableMongoRepositories(basePackages = "com.am.analysis.adapter.repository")
@EnableFeignClients
@ComponentScan(basePackages = {
        "com.am.analysis",
        "com.am.market.client",
        "com.am.trade.client",
        "com.am.feign"
})
public class AmAnalysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(AmAnalysisApplication.class, args);
    }
}
