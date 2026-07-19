package com.hyeonpyo.wallpadcontroller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableScheduling
public class WallpadcontrollerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WallpadcontrollerApplication.class, args);
	}

}
