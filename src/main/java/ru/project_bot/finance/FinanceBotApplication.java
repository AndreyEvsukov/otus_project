package ru.project_bot.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FinanceBotApplication {
	private static final Logger logger = LoggerFactory.getLogger(FinanceBotApplication.class);

	public static void main(String[] args) {
		logger.info("Starting Telegram Bot Application");
		SpringApplication.run(FinanceBotApplication.class, args);
		logger.info("Telegram Bot Application started successfully");
	}

}
