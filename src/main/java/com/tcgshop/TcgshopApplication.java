package com.tcgshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TcgshopApplication {

	public static void main(String[] args) {
		SpringApplication.run(TcgshopApplication.class, args);
	}

}
