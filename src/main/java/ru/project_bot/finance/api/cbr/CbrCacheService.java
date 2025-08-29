package ru.project_bot.finance.api.cbr;

import ru.project_bot.finance.api.cbr.model.CurrencyInfo;
import ru.project_bot.finance.api.cbr.model.CurrencyRate;

import java.util.List;

public interface CbrCacheService {
    List<CurrencyInfo> getAllCurrencies();
    List<CurrencyRate> getAllRates();
}
