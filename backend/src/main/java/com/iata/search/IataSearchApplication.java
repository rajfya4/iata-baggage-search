package com.iata.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IataSearchApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IataSearchApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(IataSearchApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("IATA Baggage Search started on port 8080");
    }
}
