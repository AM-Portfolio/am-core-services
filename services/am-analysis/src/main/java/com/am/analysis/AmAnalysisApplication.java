package com.am.analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@EnableMongoRepositories(basePackages = "com.am.analysis.adapter.repository")
@ComponentScan(basePackages = {
        "com.am.analysis",
        "am.trade"
})
public class AmAnalysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(AmAnalysisApplication.class, args);
    }
}
