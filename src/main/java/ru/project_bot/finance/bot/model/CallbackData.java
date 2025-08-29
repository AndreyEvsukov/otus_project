package ru.project_bot.finance.bot.model;

import lombok.Getter;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Getter
public class CallbackData {
    public static final String CURRENCY_PREFIX = "currency_";
    public static final String STOCK_PREFIX = "stock_";
    public static final String SEARCH_PAGE_PREFIX = "search_page_";
    public static final String CLOSE = "close";

    private final String type;
    private final String value;
    private String additionalData; // Для сложных callback

    public CallbackData(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public CallbackData(String type, String value, String additionalData) {
        this.type = type;
        this.value = value;
        this.additionalData = additionalData;
    }

    // Создание callback данных
    public static String createCurrencyCallback(String currencyCode) {
        return CURRENCY_PREFIX + currencyCode;
    }

    public static String createStockCallback(String stockCode) {
        return STOCK_PREFIX + stockCode;
    }

    // Обновленный метод с query
    public static String createSearchPageCallback(String searchType, int page, String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            return SEARCH_PAGE_PREFIX + searchType + "_" + page + "_" + encodedQuery;
        } catch (Exception e) {
            return SEARCH_PAGE_PREFIX + searchType + "_" + page + "_error";
        }
    }

    public static String createCloseCallback() {
        return CLOSE;
    }

    // Парсинг callback данных
    public static CallbackData parse(String callbackData) {
        if (callbackData == null) return null;

        if (callbackData.equals(CLOSE)) {
            return new CallbackData("close", "");
        }

        if (callbackData.startsWith(CURRENCY_PREFIX)) {
            String currencyCode = callbackData.substring(CURRENCY_PREFIX.length());
            return new CallbackData("currency", currencyCode);
        }

        if (callbackData.startsWith(STOCK_PREFIX)) {
            String stockCode = callbackData.substring(STOCK_PREFIX.length());
            return new CallbackData("stock", stockCode);
        }

        if (callbackData.startsWith(SEARCH_PAGE_PREFIX)) {
            // Парсим: search_page_{type}_{page}_{query}
            String data = callbackData.substring(SEARCH_PAGE_PREFIX.length());
            String[] parts = data.split("_", 3); // Разделяем максимум на 3 части

            if (parts.length >= 3) {
                String searchType = parts[0];
                String page = parts[1];
                try {
                    String query = URLDecoder.decode(parts[2], StandardCharsets.UTF_8);
                    return new CallbackData("search_page", searchType, page + "_" + query);
                } catch (Exception e) {
                    // Если декодирование не удалось, возвращаем без декодирования
                    return new CallbackData("search_page", searchType, page + "_" + parts[2]);
                }
            } else if (parts.length == 2) {
                return new CallbackData("search_page", parts[0], parts[1]);
            }
        }

        return new CallbackData("unknown", callbackData);
    }

    // Вспомогательные методы для извлечения данных из search_page
    public String getSearchType() {
        if ("search_page".equals(this.type)) {
            return this.value; // В value хранится searchType
        }
        return null;
    }

    public int getPage() {
        if ("search_page".equals(this.type) && this.additionalData != null) {
            String[] parts = this.additionalData.split("_", 2);
            try {
                return Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    public String getQuery() {
        if ("search_page".equals(this.type) && this.additionalData != null) {
            String[] parts = this.additionalData.split("_", 2);
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return "";
    }
}