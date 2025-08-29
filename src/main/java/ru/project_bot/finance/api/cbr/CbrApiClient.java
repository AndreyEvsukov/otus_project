package ru.project_bot.finance.api.cbr;

import ru.project_bot.finance.api.cbr.model.CurrencyInfo;
import ru.project_bot.finance.api.cbr.model.CurrencyRate;

import java.util.List;

/**
 * Интерфейс для взаимодействия с API Центрального Банка РФ.
 */
public interface CbrApiClient {

    /**
     * Получает список всех ежедневных валют от ЦБ РФ.
     *
     * @return Список объектов {@link CurrencyInfo}.
     */
    List<CurrencyInfo> fetchAllCurrencies();

    /**
     * Получает курсы валют на сегодня.
     *
     * @return Список объектов {@link CurrencyRate}.
     */
    List<CurrencyRate> fetchAllRates();
}
