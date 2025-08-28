package ru.project_bot.finance.bot.service;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface TelegramSenderService {
    void execute(BotApiMethod<?> method);

    void setTelegramBot(TelegramLongPollingBot telegramBot);

    Message sendMessage(SendMessage message);
    void editMessageText(EditMessageText editMessage);
}
