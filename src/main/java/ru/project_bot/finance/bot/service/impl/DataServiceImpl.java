package ru.project_bot.finance.bot.service.impl;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.project_bot.finance.api.cbr.CbrCacheService;
import ru.project_bot.finance.api.cbr.model.CurrencyInfo;
import ru.project_bot.finance.api.cbr.model.CurrencyRate;
import ru.project_bot.finance.api.moex.MoexCacheService;
import ru.project_bot.finance.bot.service.DataService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataServiceImpl implements DataService {

    @Autowired
    private CbrCacheService cbrCacheService;

    @Autowired
    private MoexCacheService moexCacheService;

    @Override
    public List<String> getCurrencies() {
        val curr =  cbrCacheService.getAllCurrencies();
        return curr.stream().map(CurrencyInfo::getCharCode) // Получаем символьный код
                .filter(code -> code != null && !code.isEmpty()) // Фильтруем пустые/null
                .distinct() // Оставляем только уникальные значения
                .sorted() // Сортируем по алфавиту
                .collect(Collectors.toList());
    }
    @Override
    public List<String> getStocks() {
        return moexCacheService.getActiveSharesList();
    }

    @Override
    public Double getCurrencyRate(String currencyCode) {
        val rateList = cbrCacheService.getAllRates();
        Map<String, Double> currencyRates = rateList.stream()
                .filter(rate -> rate.getCharCode() != null && !rate.getCharCode().isEmpty()) // Фильтр: charCode не null и не пустой
                .collect(Collectors.toMap(
                        CurrencyRate::getCharCode,  // Ключ: символьный код валюты
                        rate -> rate.getRate().doubleValue(), // Значение: курс как double
                        (existing, replacement) -> existing // Разрешение коллизий: оставляем существующее значение
                ));
        return currencyRates.get(currencyCode);
    }

    @Override
    public Double getNormalizedCurrencyRate(String currencyCode) {
        val rateList = cbrCacheService.getAllRates();
        Map<String, Double> currencyRates = rateList.stream()
                .filter(rate -> rate.getCharCode() != null && !rate.getCharCode().isEmpty()) // Фильтр: charCode не null и не пустой
                .collect(Collectors.toMap(
                        CurrencyRate::getCharCode,  // Ключ: символьный код валюты
                        rate -> rate.getNormalizeRate().doubleValue(), // Значение: курс как double
                        (existing, replacement) -> existing // Разрешение коллизий: оставляем существующее значение
                ));
        return currencyRates.get(currencyCode);
    }


    @Override
    public Double getStockPrice(String stockCode) {
        val stockPrices = moexCacheService.getSharePrices();
        return stockPrices.get(stockCode);
    }

    @Override
    public boolean isCurrencyExists(String currencyCode) {
        val currencies =  getCurrencies();
        return currencies.contains(currencyCode);
    }
    @Override
    public boolean isStockExists(String stockCode) {
        val stocks = getStocks();
        return stocks.contains(stockCode);
    }

    @Override
    public String getCurrencyName(String currencyCode) {
        val curr =  cbrCacheService.getAllCurrencies();
        Map<String, String> currencyNames = curr.stream()
                .filter(info -> info.getCharCode() != null && !info.getCharCode().isEmpty()) // Фильтр по ключу
                .collect(Collectors.toMap(
                        CurrencyInfo::getCharCode, // Ключ: символьный код
                        CurrencyInfo::getName,     // Значение: русское название
                        (existing, replacement) -> existing // В случае дубликата ключа, оставляем первый
                ));
        return currencyNames.getOrDefault(currencyCode, currencyCode);
    }

    @Override
    public String getStockName(String stockCode) {
        val stockNames = moexCacheService.getActiveSharesMap();
        return stockNames.getOrDefault(stockCode, stockCode);
    }

    @Override
    public List<String> getAllInstruments() {
        List<String> all = new ArrayList<>(getCurrencies());
        all.addAll(getStocks());
        return all;
    }
}
