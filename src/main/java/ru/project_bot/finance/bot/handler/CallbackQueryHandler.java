// src/main/java/ru/project_bot/finance/bot/handler/CallbackQueryHandler.java
package ru.project_bot.finance.bot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Интерфейс для обработки callback-запросов от inline-кнопок.
 */
public interface CallbackQueryHandler {
    /**
     * Обрабатывает callback-запрос.
     *
     * @param chatId       Идентификатор чата.
     * @param messageId    Идентификатор сообщения, к которому прикреплена клавиатура.
     * @param callbackData Данные callback-запроса.
     * @param update       Полный объект Update (может понадобиться для AnswerCallbackQuery).
     */
    void handleCallbackQuery(long chatId, int messageId, String callbackData, Update update);
}