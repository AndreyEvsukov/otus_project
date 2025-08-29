package ru.project_bot.finance.bot.service;

import java.util.List;

public interface DataService {
    List<String> getCurrencies();
    List<String> getStocks();
    Double getCurrencyRate(String currencyCode);
    Double getNormalizedCurrencyRate(String currencyCode);
    Double getStockPrice(String stockCode);
    boolean isCurrencyExists(String currencyCode);
    boolean isStockExists(String stockCode);
    String getCurrencyName(String currencyCode);
    String getStockName(String stockCode);
    List<String> getAllInstruments();
}
