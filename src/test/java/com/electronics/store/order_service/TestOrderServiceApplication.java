package com.electronics.store.order_service;

import org.springframework.boot.SpringApplication;

public class TestOrderServiceApplication {

	static void main(String[] args) {
		SpringApplication.from(OrderServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
