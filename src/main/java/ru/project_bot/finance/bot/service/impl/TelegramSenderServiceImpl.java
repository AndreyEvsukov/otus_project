package ru.project_bot.finance.bot.service.impl;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.project_bot.finance.bot.service.TelegramSenderService;

@Setter
@Service
public class TelegramSenderServiceImpl implements TelegramSenderService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramSenderServiceImpl.class);

    private TelegramLongPollingBot telegramBot;

    @Override
    public void execute(BotApiMethod<?> method) {
        try {
            if (telegramBot != null && method != null) {
                telegramBot.execute(method);
            } else {
                logger.warn("Cannot execute method: telegramBot={} method={}",
                        telegramBot, method);
            }
        } catch (TelegramApiException e) {
            logger.error("Telegram API error while executing method: {}",
                    method.getClass().getSimpleName(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while executing method: {}",
                    method != null ? method.getClass().getSimpleName() : "null", e);
        }
    }

    @Override
    public Message sendMessage(SendMessage message) {
        try {
            if (telegramBot != null && message != null) {
                return telegramBot.execute(message);
            } else {
                logger.warn("Cannot send message: telegramBot={} message={}", telegramBot, message);
            }
        } catch (TelegramApiException e) {
            logger.error("Telegram API error while sending message: {}", message, e);
        }
        return null;
    }

    @Override
    public void editMessageText(EditMessageText editMessage) {
        try {
            if (telegramBot != null && editMessage != null) {
                telegramBot.execute(editMessage);
            } else {
                logger.warn("Cannot edit message: telegramBot={} editMessage={}", telegramBot, editMessage);
            }
        } catch (TelegramApiException e) {
            logger.error("Telegram API error while editing message: {}", editMessage, e);
        } catch (Exception e) {
            logger.error("Unexpected error while editing message: {}", editMessage, e);
        }
    }
}
