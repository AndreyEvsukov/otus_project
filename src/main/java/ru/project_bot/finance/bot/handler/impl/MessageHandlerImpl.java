package ru.project_bot.finance.bot.handler.impl;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.project_bot.finance.bot.handler.BotCommandHandler;
import ru.project_bot.finance.bot.handler.MessageHandler;
import ru.project_bot.finance.bot.service.MessageService;

/**
 * Реализация {@link MessageHandler} для обработки пользовательского ввода
 */
@Service
public class MessageHandlerImpl implements MessageHandler {

    @Autowired
    private BotCommandHandler botCommandHandler;
    @Autowired
    private MessageService messageService;

    @Override
    public void handleMessage(long chatId, String userMessage) {
        val userInput = userMessage.trim();

        // Проверяем, не является ли сообщение текстом с кнопки, содержащей команду в скобках
        val extractedCommand = extractCommandFromButton(userInput);
        if (extractedCommand != null) {
            botCommandHandler.handleCommand(chatId, extractedCommand);
        }  else if (userInput.startsWith("/")) {
            botCommandHandler.handleCommand(chatId, userInput);
        } else {
            // Считаем, что пользователь ввел код валюты или акции
            messageService.handleInstrumentInput(chatId, userInput);
        }
    }

    /**
     * Извлекает команду из текста кнопки, если она указана в скобках в конце.
     * Например, из "Курс валют (/rate)" извлекает "/rate".
     * @param buttonText Текст кнопки.
     * @return Извлеченная команда или null, если не найдена.
     */
    private String extractCommandFromButton(String buttonText) {
        // Регулярное выражение для поиска команды в скобках в конце строки
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*\\((/[^)]+)\\)$");
        java.util.regex.Matcher matcher = pattern.matcher(buttonText);
        if (matcher.matches()) {
            return matcher.group(1); // Возвращаем найденную команду
        }
        return null; // Команда не найдена
    }
}