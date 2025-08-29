package ru.project_bot.finance.bot.service;

import java.util.List;

public interface MessageService {
    void sendMainMenu(long chatId);

    void sendCurrencyInstructions(long chatId);

    void sendStockInstructions(long chatId);

    void sendSearchInstructions(long chatId);

    void sendMessage(long chatId, String text);

    void handleInstrumentInput(long chatId, String input);

    void sendCurrencyRate(long chatId, String currencyCode);

    void sendStockPrice(long chatId, String stockCode);

    void sendSuggestions(long chatId, List<String> suggestions, String query);

    void sendSearchResults(long chatId, String query, String searchType, int page);

    void sendHelpMessage(long chatId);

    void removeInlineKeyboard(long chatId, int messageId);
}
