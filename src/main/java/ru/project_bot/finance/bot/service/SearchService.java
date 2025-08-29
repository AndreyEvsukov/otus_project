package ru.project_bot.finance.bot.service;

import java.util.List;

public interface SearchService {
    List<String> getAutocompleteSuggestions(String input, int maxResults);

    List<String> searchAll(String query);

    List<String> searchCurrencies(String query);

    List<String> searchStocks(String query);
}
