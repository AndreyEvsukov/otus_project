package ru.project_bot.finance.bot.handler;

/**
 * Интерфейс для обработки команд бота
 */
public interface BotCommandHandler {
    /**
     * Обрабатывает команду бота.
     *
     * @param chatId  Идентификатор чата.
     * @param command Текст команды (например, "/start", "/rate USD").
     */
    void handleCommand(long chatId, String command);
}