package com.dxc.dxc_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DxcPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(DxcPlatformApplication.class, args);
	}

}


