// src/main/java/ru/project_bot/finance/bot/handler/impl/BotCommandHandlerImpl.java
package ru.project_bot.finance.bot.handler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.project_bot.finance.bot.handler.BotCommandHandler;
import ru.project_bot.finance.bot.service.MessageService;

/**
 * Реализация {@link BotCommandHandler} для обработки команд бота.
 */
@Service
public class BotCommandHandlerImpl implements BotCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(BotCommandHandlerImpl.class);

    @Autowired
    private MessageService messageService;

    @Override
    public void handleCommand(long chatId, String command) {
        command = command.toLowerCase().trim();

        // Обработка специализированных команд поиска (валюты и акции)
        if (command.startsWith("/search_currency")) {
            String query = command.substring("/search_currency".length()).trim();
            if (!query.isEmpty()) {
                messageService.sendSearchResults(chatId, query, "currency", 0);
            } else {
                messageService.sendSearchInstructions(chatId);
            }
            return;
        }

        if (command.startsWith("/search_stock")) {
            String query = command.substring("/search_stock".length()).trim();
            if (!query.isEmpty()) {
                messageService.sendSearchResults(chatId, query, "stock", 0);
            } else {
                messageService.sendSearchInstructions(chatId);
            }
            return;
        }

        // Обработка поиска с параметрами
        if (command.startsWith("/search")) {
            // /search USD или /search SBER
            String query = command.substring("/search".length()).trim();
            if (!query.isEmpty()) {
                // Если параметр задан - вызываем постраничный поиск
                messageService.sendSearchResults(chatId, query, "all", 0);
            } else {
                // Если параметр пустой, показываем инструкции
                messageService.sendSearchInstructions(chatId);
            }
            return;
        }

        // Обработка команд без параметров (выдаём инструкции)
        switch (command) {
            case "/start":
                messageService.sendMainMenu(chatId);
                break;
            case "/rate":
                messageService.sendCurrencyInstructions(chatId);
                break;
            case "/price":
                messageService.sendStockInstructions(chatId);
                break;
            case "/help":
                messageService.sendHelpMessage(chatId);
                break;
            default:
                messageService.sendMessage(chatId, "Неизвестная команда. Используйте /help для справки.");
                logger.debug("Получена неизвестная команда: {}", command);
        }
    }
}