package ru.practicum.ewm.main.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.ewm.main.server", "ru.practicum.ewm.stats.client"})
public class MainServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainServerApplication.class, args);
    }
}
