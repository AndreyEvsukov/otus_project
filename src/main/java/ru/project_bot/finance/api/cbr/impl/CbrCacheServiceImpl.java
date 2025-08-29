package ru.project_bot.finance.api.cbr.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.val;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.project_bot.finance.api.cbr.CbrApiClient;
import ru.project_bot.finance.api.cbr.CbrCacheService;
import ru.project_bot.finance.api.cbr.model.CurrencyInfo;
import ru.project_bot.finance.api.cbr.model.CurrencyRate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CbrCacheServiceImpl implements CbrCacheService {

    private static final Logger logger = LoggerFactory.getLogger(CbrCacheServiceImpl.class);
    private static final String CURRENCIES_CACHE_ALIAS = "currenciesCache";
    private static final String RATES_CACHE_ALIAS = "ratesCache";

    private final CbrApiClient cbrApiClient;
    private CacheManager cacheManager;
    private Cache<String, List<CurrencyInfo>> currenciesCache;
    private Cache<String, List<CurrencyRate>> ratesCache;

    // Форматтер для ключей даты (только дата)
    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    public CbrCacheServiceImpl(CbrApiClient cbrApiClient) {
        this.cbrApiClient = cbrApiClient;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        logger.info("Инициализация Ehcache для CbrCacheService");

        cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();

        // Создание кэша для валют
        currenciesCache = cacheManager.createCache(CURRENCIES_CACHE_ALIAS,
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                String.class,
                                (Class<List<CurrencyInfo>>) (Class<?>) List.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(2, EntryUnit.ENTRIES))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(24)))
        );

        // Создание кэша для курсов
        ratesCache = cacheManager.createCache(RATES_CACHE_ALIAS,
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                String.class,
                                (Class<List<CurrencyRate>>) (Class<?>) List.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(2, EntryUnit.ENTRIES))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(24)))
        );

        logger.info("Ehcache инициализирован.");
    }

    @PreDestroy
    public void cleanup() {
        if (cacheManager != null) {
            logger.info("Закрытие CacheManager Ehcache");
            cacheManager.close();
        }
    }

    /**
     * Получает список всех валют, используя кэш.
     * Кэш общий для всех запросов (ключ "all_currencies").
     */
    public List<CurrencyInfo> getAllCurrencies() {
        String key = "all_currencies";
        List<CurrencyInfo> currencies = currenciesCache.get(key);

        if (currencies == null) {
            logger.info("Кэш списка валют пуст. Загрузка из API...");
            currencies = cbrApiClient.fetchAllCurrencies();
            if (currencies != null) { // Проверка на null для безопасности
                currenciesCache.put(key, currencies);
                logger.info("Список валют загружен и закэширован. Размер: {}", currencies.size());
            } else {
                logger.warn("API вернул пустой список валют.");
            }
        } else {
            logger.debug("Список валют получен из кэша Ehcache.");
        }
        return currencies != null ? currencies : List.of(); // Возвращаем пустой список вместо null
    }

    /**
     * Получает курсы валют на сегодня
     * Ключ кэша - это дата в формате "yyyy-MM-dd".
     *
     * @return Список курсов валют.
     */
    public List<CurrencyRate> getAllRates() {
        val date = LocalDate.now();
        // Ключ кэша - это строковое представление даты без времени
        val key = date.format(DATE_KEY_FORMATTER);
        List<CurrencyRate> rates = ratesCache.get(key);

        if (rates == null) {
            logger.info("Кэш курсов на дату {} пуст. Загрузка из API...", key);
            // Передаем дату в API клиент
            rates = cbrApiClient.fetchAllRates();
            if (rates != null) { // Проверка на null для безопасности
                ratesCache.put(key, rates);
                logger.info("Курсы на дату {} загружены и закэшированы. Размер: {}", key, rates.size());
            } else {
                logger.warn("API вернул пустой список курсов на дату {}.", key);
            }
        } else {
            logger.debug("Курсы на дату {} получены из кэша Ehcache.", key);
        }
        return rates != null ? rates : List.of(); // Возвращаем пустой список вместо null
    }
}