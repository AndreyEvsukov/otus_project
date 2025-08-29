package ru.project_bot.finance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.updates.GetWebhookInfo;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.WebhookInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.project_bot.finance.bot.TelegramBot;

@Component
public class BotRegistrationChecker {

    private static final Logger logger = LoggerFactory.getLogger(BotRegistrationChecker.class);

    @Autowired
    private TelegramBot telegramBot;

    @EventListener(ApplicationReadyEvent.class)
    public void checkBotRegistration() {
        logger.info("🔍 Checking bot registration and configuration...");

        try {
            // Проверяем, что бот может подключиться к Telegram API
            GetMe getMe = new GetMe();
            User botInfo = telegramBot.execute(getMe);
            logger.info("✅ Bot successfully connected to Telegram API");
            logger.info("🤖 Bot info - Username: {}, ID: {}",
                    botInfo.getUserName(), botInfo.getId());

            // Проверяем текущие вебхуки
            GetWebhookInfo getWebhookInfo = new GetWebhookInfo();
            WebhookInfo webhookInfo = telegramBot.execute(getWebhookInfo);

            if (webhookInfo != null && webhookInfo.getUrl() != null && !webhookInfo.getUrl().isEmpty()) {
                logger.warn("⚠️  Bot has active webhook: {}", webhookInfo.getUrl());
                logger.warn("💡 This may prevent long polling from working");
            } else {
                logger.info("✅ No active webhooks - ready for long polling");
            }

            // Проверяем режим работы
            logger.info("🔧 Bot configuration check completed");

        } catch (TelegramApiException e) {
            logger.error("❌ Failed to check bot registration", e);
            logger.error("💡 Possible causes:");
            logger.error("   - Invalid bot token");
            logger.error("   - Network connectivity issues");
            logger.error("   - Telegram API temporary unavailability");
        } catch (Exception e) {
            logger.error("❌ Unexpected error during bot registration check", e);
        }
    }
}