package com.aspiresys.fp_micro_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FpMicroGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(FpMicroGatewayApplication.class, args);
	}

}
