package ru.project_bot.finance.bot.handler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.project_bot.finance.bot.handler.CallbackQueryHandler;
import ru.project_bot.finance.bot.model.CallbackData;
import ru.project_bot.finance.bot.service.MessageService;
import ru.project_bot.finance.bot.service.TelegramSenderService;

/**
 * Реализация {@link CallbackQueryHandler} для обработки callback-запросов.
 */
@Service
public class CallbackQueryHandlerImpl implements CallbackQueryHandler {

    private static final Logger logger = LoggerFactory.getLogger(CallbackQueryHandlerImpl.class);

    @Autowired
    private MessageService messageService;
    @Autowired
    private TelegramSenderService telegramSenderService;

    @Override
    public void handleCallbackQuery(long chatId, int messageId, String callbackData, Update update) {
        try {
            // Обработка специального случая: кнопка информации о странице
            if ("page_info".equals(callbackData)) {
                // Отправляем ответ на callback query, чтобы кнопка "перестала мигать"
                AnswerCallbackQuery answer = new AnswerCallbackQuery();
                answer.setCallbackQueryId(update.getCallbackQuery().getId());
                answer.setText("Это информация о странице");
                answer.setShowAlert(false);
                telegramSenderService.execute(answer); // Используем TelegramSenderService
                logger.debug("Обработан callback 'page_info'");
                return; // Важно: выходим из метода
            }

            CallbackData parsedData = CallbackData.parse(callbackData);

            if (parsedData == null) {
                messageService.sendMessage(chatId, "Ошибка обработки запроса.");
                logger.warn("Не удалось распарсить callback data: {}", callbackData);
                return;
            }

            switch (parsedData.getType()) {
                case "currency":
                    messageService.sendCurrencyRate(chatId, parsedData.getValue());
                    messageService.removeInlineKeyboard(chatId, messageId);
                    break;

                case "stock":
                    messageService.sendStockPrice(chatId, parsedData.getValue());
                    messageService.removeInlineKeyboard(chatId, messageId);
                    break;

                case "search_page":
                    String searchType = parsedData.getSearchType();
                    int page = parsedData.getPage();
                    String query = parsedData.getQuery();

                    messageService.sendSearchResults(chatId, query, searchType, page);
                    // Не удаляем клавиатуру, так как пользователь может листать страницы
                    break;

                case "close":
                    messageService.removeInlineKeyboard(chatId, messageId);
                    break;

                default:
                    messageService.sendMessage(chatId, "Неизвестный тип запроса.");
                    logger.debug("Получен неизвестный тип callback-запроса: {}", parsedData.getType());
                    break;
            }
        } catch (Exception e) {
            logger.error("❌ Error handling callback query: {}", callbackData, e);
            messageService.sendMessage(chatId, "Ошибка обработки запроса.");
        }
    }
}