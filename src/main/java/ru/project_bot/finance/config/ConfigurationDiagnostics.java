package ru.project_bot.finance.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
public class ConfigurationDiagnostics {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationDiagnostics.class);

    @Value("${telegrambots.enabled:false}")
    private boolean telegramBotsEnabled;

    @Value("${telegrambots.webhook.enabled:true}")
    private boolean webhookEnabled;

    @Value("${telegrambots.long-polling.enabled:false}")
    private boolean longPollingEnabled;

    @Value("${telegrambots.long-polling.timeout:30}")
    private int longPollingTimeout;

    @Value("${bot.username:not_set}")
    private String botUsername;

    @Value("${bot.token:not_set}")
    private String botToken;

    @PostConstruct
    public void logConfiguration() {
        logger.info("=== CONFIGURATION DIAGNOSTICS ===");
        logger.info("telegrambots.enabled: {}", telegramBotsEnabled);
        logger.info("telegrambots.webhook.enabled: {}", webhookEnabled);
        logger.info("telegrambots.long-polling.enabled: {}", longPollingEnabled);
        logger.info("telegrambots.long-polling.timeout: {}", longPollingTimeout);
        logger.info("bot.username: {}", botUsername);
        logger.info("bot.token set: {}", !botToken.equals("not_set"));
        if (!botToken.equals("not_set")) {
            logger.info("bot.token length: {}", botToken.length());
        }
        logger.info("=== END CONFIGURATION ===");
    }

    @EventListener(ContextRefreshedEvent.class)
    public void logApplicationContextRefreshed() {
        logger.info("✅ Application context refreshed - all configurations loaded");
    }
}