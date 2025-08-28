package ru.project_bot.finance.api.moex.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import ru.project_bot.finance.api.moex.MoexApiClient;
import ru.project_bot.finance.api.moex.MoexCacheService;
import ru.project_bot.finance.api.moex.model.MxdResponse;
import ru.project_bot.finance.api.moex.model.ShareData;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MoexCacheServiceImpl implements MoexCacheService {

    private static final Logger logger = LoggerFactory.getLogger(MoexCacheServiceImpl.class);

    // Время жизни кэша - 10 минут
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Autowired
    private MoexApiClient moexApiClient;
    private CacheManager cacheManager;

    // Кэш для MxdResponse (getSecurities)
    // Ключ: фиксированный ("securities"), Значение: MxdResponse
    private Cache<String, MxdResponse> securitiesCache;

    // Кэш для ShareData (getSharePrice)
    // Ключ: ticker (String), Значение: ShareData
    private Cache<String, ShareData> sharePriceCache;

    private static final String SECURITIES_CACHE_KEY = "securities"; // Константа для ключа списка ценных бумаг

    @PostConstruct
    public void init() {
        logger.info("Инициализация Ehcache для MoexDataCacheApiImpl с TTL {} минут", CACHE_TTL.toMinutes());

        cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();

        // Создание кэша для списка ценных бумаг (MxdResponse)
        securitiesCache = cacheManager.createCache("securitiesCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, MxdResponse.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(10, EntryUnit.ENTRIES))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(CACHE_TTL))
        );

        // Создание кэша для цен акций (ShareData)
        // Размер кэша зависит от количества уникальных запрашиваемых тикеров.
        // 1000 - разумный размер для старта, можно настроить.
        sharePriceCache = cacheManager.createCache("sharePriceCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, ShareData.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1000, EntryUnit.ENTRIES))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(CACHE_TTL))
        );

        logger.info("Ehcache для MoexDataCacheApiImpl инициализирован.");
    }

    @PreDestroy
    public void cleanup() {
        if (cacheManager != null) {
            logger.info("Закрытие CacheManager Ehcache для MoexDataCacheApiImpl");
            cacheManager.close();
            logger.info("CacheManager Ehcache для MoexDataCacheApiImpl закрыт.");
        }
    }

    /**
     * 1) Возвращает список бумаг List<String>.
     * Отбирает бумаги из getSecurities(), имеющие status='A', непустой isin и listlevel = '1'.
     * Результат: List<String> с полями secid.
     *
     * @return Список идентификаторов (secid) отфильтрованных бумаг.
     */
    @Override
    public List<String> getActiveSharesList() {
        logger.debug("Начало получения списка активных акций (secid)...");
        List<Map<String, String>> filteredData = getFilteredSecuritiesData();

        List<String> result = filteredData.stream()
                .map(row -> row.get("secid")) // извлекаем secid
                .filter(secid -> secid != null && !secid.trim().isEmpty()) // фильтруем пустые secid
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        logger.debug("Получен список активных акций. Количество: {}", result.size());
        return result;
    }

    @Override
    public Map<String, String> getActiveSharesMap() {
        logger.debug("Начало получения мапы активных акций (secid -> secname)...");

        List<Map<String, String>> filteredData = getFilteredSecuritiesData();

        Map<String, String> result = filteredData.stream()
                .filter(row -> {
                    String secid = row.get("secid");
                    return secid != null && !secid.trim().isEmpty(); // доп. проверка secid
                })
                .filter(row -> {
                    String secname = row.get("secname");
                    return secname != null; // secname может быть пустой строкой, но не null
                })
                .collect(Collectors.toMap(
                        row -> row.get("secid"),     // ключ - secid
                        row -> row.get("secname"),   // значение - secname
                        (existing, replacement) -> existing // разрешение коллизий
                ));

        logger.debug("Получена мапа активных акций. Количество: {}", result.size());
        return result;
    }

    @Override
    public Map<String, Double> getSharePrices() {
        logger.info("Начало получения цен для всех активных акций...");
        try {
            List<String> activeTickers = this.getActiveSharesList();
            logger.debug("Получен список активных тикеров. Количество: {}", activeTickers.size());

            // Используем цикл для лучшего контроля и обработки ошибок
            Map<String, Double> pricesMap = new HashMap<>();
            int successCount = 0;
            int errorCount = 0;

            for (String ticker : activeTickers) {
                try {
                    Double price = this.getSharePriceValue(ticker);
                    // Добавляем в мапу только если цена успешно получена
                    if (price != null) {
                        pricesMap.put(ticker, price);
                        successCount++;
                    } else {
                        logger.debug("Цена для тикера {} не получена (null)", ticker);
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.warn("Ошибка при получении цены для тикера {}: {}", ticker, e.getMessage());
                    // Не прерываем весь процесс из-за ошибки одной акции
                }
            }

            logger.info("Завершено получение цен. Успешно: {}, Ошибок: {}, В мапе: {}",
                    successCount, errorCount, pricesMap.size());
            return pricesMap;
        } catch (Exception ex) {
            logger.error("Ошибка в MoexDataCacheApi.getSharePrices()", ex);
            throw ex; // Пробрасываем исключение дальше
        }
    }

    private MxdResponse getSecurities() {
        // Попытка получить из кэша
        MxdResponse cachedResponse = securitiesCache.get(SECURITIES_CACHE_KEY);

        if (cachedResponse != null) {
            logger.debug("Возвращены закэшированные данные для списка ценных бумаг.");
            return cachedResponse;
        }

        // Кэш пуст или истёк
        logger.info("Кэш списка ценных бумаг устарел или пуст. Загрузка из API MOEX...");
        try {
            MxdResponse freshData = moexApiClient.getSecurities();
            if (freshData != null) {
                securitiesCache.put(SECURITIES_CACHE_KEY, freshData);
                logger.info("Список ценных бумаг успешно загружен и закэширован.");
            } else {
                logger.warn("MOEX API вернул null для списка ценных бумаг.");
            }
            return freshData;
        } catch (Exception e) {
            logger.error("Ошибка при загрузке списка ценных бумаг из API MOEX. Кэш пуст.", e);
            // В зависимости от требований, можно пробросить исключение или вернуть null/пустой объект
            throw e;
        }
    }

    /**
     * Получает и фильтрует данные о ценных бумагах.
     * Фильтрация происходит по следующим критериям:
     * - status = 'A' (активные)
     * - isin не пустой
     * - listlevel = '1' (уровень листинга 1)
     *
     * @return Список отфильтрованных записей о ценных бумагах.
     */
    private List<Map<String, String>> getFilteredSecuritiesData() {
        MxdResponse securities = this.getSecurities();

        if (securities == null || securities.data == null || securities.data.isEmpty()) {
            logger.warn("Данные о ценных бумагах отсутствуют или пусты.");
            return new ArrayList<>(); // Возвращаем пустой список
        }

        return securities.data.stream()
                .filter(Objects::nonNull) // Фильтруем null-строки
                .filter(row -> "A".equals(row.get("status"))) // status='A'
                .filter(row -> {
                    String isin = row.get("isin");
                    return isin != null && !isin.trim().isEmpty(); // isin не пустой
                })
                .filter(row -> "1".equals(row.get("listlevel"))) // listlevel = '1'
                .filter(row-> "SPEQ".equals(row.get("boardid")))
                .collect(Collectors.toList());
    }

    private ShareData getShareData(String ticker) {
        // Валидация входных данных
        if (ticker == null || ticker.isEmpty()) {
            throw new IllegalArgumentException("Ticker не может быть null или пустым");
        }

        // Попытка получить из кэша
        ShareData cachedData = sharePriceCache.get(ticker);

        if (cachedData != null) {
            logger.debug("Возвращены закэшированные данные для акции: {}", ticker);
            return cachedData;
        }

        // Кэш пуст или истёк
        logger.info("Кэш цены для акции {} устарел или пуст. Загрузка из API MOEX...", ticker);
        try {
            ShareData freshData = moexApiClient.getSharePrice(ticker);
            if (freshData != null) {
                sharePriceCache.put(ticker, freshData);
                logger.info("Цена для акции {} успешно загружена и закэширована.", ticker);
            } else {
                logger.warn("MOEX API вернул null для цены акции {}.", ticker);
            }
            return freshData;
        } catch (Exception e) {
            logger.error("Ошибка при загрузке цены для акции {} из API MOEX. Кэш пуст.", ticker, e);
            // В зависимости от требований, можно пробросить исключение или вернуть null/пустой объект
            throw e;
        }
    }

    private Double getSharePriceValue(String ticker) {
        if (ticker == null || ticker.isEmpty()) {
            throw new IllegalArgumentException("Ticker не может быть null или пустым");
        }

        ShareData shareData = getShareData(ticker); // Использем существующий метод
        String priceStr = getPrice(shareData.getMarketData()); // Использем существующий метод

        if (priceStr == null || priceStr.isEmpty() || "null".equalsIgnoreCase(priceStr)) {
            logger.warn("Не удалось получить цену для тикера {}. Цена будет null.", ticker);
            return null; // Или 0.0, в зависимости от логики
        }

        try {
            return Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            logger.warn("Не удалось преобразовать цену '{}' для тикера {} в число.", priceStr, ticker);
            return null; // Или 0.0
        }
    }

    private static String getPrice(MxdResponse marketData) {
        if (marketData == null || marketData.getData() == null || marketData.getData().isEmpty()) {
            return null;
        }
        val lastPrice = getDataByName(marketData, "last");
        if (lastPrice != null && !lastPrice.isEmpty() && !"null".equalsIgnoreCase(lastPrice)) {
            return lastPrice;
        }
        return getDataByName(marketData, "marketprice"); // fallback
    }

    private static String getDataByName(MxdResponse marketData, String name) {
        if (marketData == null || marketData.getData() == null || marketData.getData().isEmpty() || name == null) {
            return null;
        }
        val firstRow = marketData.getData().getFirst();
        return firstRow != null ? firstRow.get(name) : null;
    }

}