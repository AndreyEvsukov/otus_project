package ru.project_bot.finance.bot.handler;

/**
 * Интерфейс для обработки сообщений пользователя
 */
public interface MessageHandler {
    /**
     * Обрабатывает текстовое сообщение от пользователя.
     *
     * @param chatId Идентификатор чата.
     * @param userMessage   Текст сообщения.
     */
    void handleMessage(long chatId, String userMessage);
}