package ru.project_bot.finance.bot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.project_bot.finance.bot.service.DataService;
import ru.project_bot.finance.bot.service.SearchService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private DataService dataService;

    @Override
    public List<String> getAutocompleteSuggestions(String input, int maxResults) {
        if (input == null || input.trim().isEmpty()) {
            // Возвращаем популярные инструменты
            return getPopularInstruments().subList(0, Math.min(maxResults, 10));
        }

        String normalizedInput = input.toLowerCase().trim();
        List<String> allInstruments = dataService.getAllInstruments();

        return allInstruments.stream()
                .filter(item -> matchesSearch(item, normalizedInput))
                .sorted((a, b) -> compareByRelevance(a, b, normalizedInput))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> searchAll(String query) {
        return searchInstruments(dataService.getAllInstruments(), query);
    }

    @Override
    public List<String> searchCurrencies(String query) {
        return searchInstruments(dataService.getCurrencies(), query);
    }

    @Override
    public List<String> searchStocks(String query) {
        return searchInstruments(dataService.getStocks(), query);
    }

    private List<String> searchInstruments(List<String> instruments, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(instruments);
        }

        String normalizedQuery = query.toLowerCase().trim();
        return instruments.stream()
                .filter(item -> matchesSearch(item, normalizedQuery))
                .sorted((a, b) -> compareByRelevance(a, b, normalizedQuery))
                .collect(Collectors.toList());
    }

    private boolean matchesSearch(String item, String query) {
        if (query.isEmpty()) return true;

        String itemLower = item.toLowerCase();

        // Точное совпадение
        if (itemLower.equals(query)) return true;

        // Начинается с запроса
        if (itemLower.startsWith(query)) return true;

        // Содержит запрос
        if (itemLower.contains(query)) return true;

        // Поиск по названию (если есть)
        String name = getItemName(item);
        return name != null && name.toLowerCase().contains(query);
    }

    private int compareByRelevance(String a, String b, String query) {
        String aLower = a.toLowerCase();
        String bLower = b.toLowerCase();
        String queryLower = query.toLowerCase();

        // Точные совпадения первые
        if (aLower.equals(queryLower) && !bLower.equals(queryLower)) return -1;
        if (!aLower.equals(queryLower) && bLower.equals(queryLower)) return 1;

        // Начинающиеся с запроса
        boolean aStartsWith = aLower.startsWith(queryLower);
        boolean bStartsWith = bLower.startsWith(queryLower);
        if (aStartsWith && !bStartsWith) return -1;
        if (!aStartsWith && bStartsWith) return 1;

        // По длине (более короткие первые)
        int lengthDiff = a.length() - b.length();
        if (lengthDiff != 0) return lengthDiff;

        // По алфавиту
        return a.compareTo(b);
    }

    private String getItemName(String item) {
        if (dataService.isCurrencyExists(item)) {
            return dataService.getCurrencyName(item);
        } else if (dataService.isStockExists(item)) {
            return dataService.getStockName(item);
        }
        return null;
    }

    private List<String> getPopularInstruments() {
        // Возвращаем популярные инструменты
        return Arrays.asList(
                "USD", "EUR", "CNY", "JPY", "GBP",
                "SBER", "GAZP", "LKOH", "GMKN", "ROSNEFT",
                "VTBR", "TATN", "CHMF", "NLMK", "MAGN"
        );
    }
}
