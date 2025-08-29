package ru.project_bot.finance.api.cbr.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.project_bot.finance.api.cbr.CbrApiClient;
import ru.project_bot.finance.api.cbr.CbrXmlParser;
import ru.project_bot.finance.api.cbr.model.CurrencyInfo;
import ru.project_bot.finance.api.cbr.model.CurrencyRate;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Реализация {@link CbrApiClient} для работы с API ЦБ РФ через HTTP-запросы.
 */
@Service
public class CbrApiClientImpl implements CbrApiClient {

    private static final Logger logger = LoggerFactory.getLogger(CbrApiClientImpl.class);

    private static final String DAILY_URL = "https://www.cbr.ru/scripts/XML_daily.asp";
    private static final String ENUM_FULL_URL = "https://www.cbr.ru/scripts/XML_valFull.asp";

    // Используем windows-1251, так как ЦБ РФ отдает XML именно в этой кодировке
    private static final Charset CB_CHARSET = Charset.forName("windows-1251");

    @Autowired
    private CbrXmlParser cbrXmlParser;

    @Autowired
    @Qualifier("cbrWebClient")
    private WebClient cbrWebClient;

    @Override
    public List<CurrencyInfo> fetchAllCurrencies() {
        try {
            String xmlContent = sendGetRequest(ENUM_FULL_URL);
            if (xmlContent != null) {
                logger.debug("Получен XML полного справочника валют (первые 500 символов): {}", xmlContent.substring(0, Math.min(500, xmlContent.length())));
                return cbrXmlParser.parseCurrencyDictionary(xmlContent);
            } else {
                logger.warn("Получен пустой ответ при запросе справочника валют.");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("Ошибка при получении списка валют от ЦБ", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<CurrencyRate> fetchAllRates() {
        try {
            String xmlContent = sendGetRequest(DAILY_URL);
            if (xmlContent != null) {
                logger.debug("Получен XML курсов валют (первые 500 символов): {}", xmlContent.substring(0, Math.min(500, xmlContent.length())));
                return cbrXmlParser.parseCurrencyRates(xmlContent);
            } else {
                logger.warn("Получен пустой ответ при запросе курсов валют.");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("Ошибка при получении курсов от ЦБ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Отправляет GET-запрос к указанному URL и возвращает тело ответа как строку.
     * Использует WebClient.
     *
     * @param urlString URL для запроса.
     * @return Тело ответа в виде строки или null в случае ошибки.
     * @throws Exception В случае сетевых ошибок, таймаутов или не-200 HTTP кодов.
     */
    private String sendGetRequest(String urlString) throws Exception {
        logger.debug("Отправка запроса к: {}", urlString);

        try {
            // Используем retrieve() для синхронного получения тела.
            // acceptCharset указывает желаемую кодировку для Accept-Charset header (может не повлиять на сервер)
            Mono<String> responseMono = cbrWebClient.get()
                    .uri(urlString)
                    .acceptCharset(CB_CHARSET, StandardCharsets.UTF_8) // Предпочтение windows-1251
                    .retrieve()
                    // Обрабатываем ошибки HTTP
                    .onStatus(status -> !status.is2xxSuccessful(),
                            response -> {
                                logger.warn("Получен неуспешный HTTP статус {} для URL: {}", response.statusCode(), urlString);
                                return Mono.error(new WebClientResponseException(
                                        response.statusCode().value(),
                                        "HTTP Error " + response.statusCode(),
                                        response.headers().asHttpHeaders(),
                                        new byte[0], // empty body for status exception
                                        CB_CHARSET
                                ));
                            })
                    .bodyToMono(String.class)
                    // Добавляем retry логику на случай временных сетевых проблем
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof TimeoutException ||
                                    throwable instanceof java.net.ConnectException ||
                                    throwable instanceof java.net.SocketTimeoutException))
                    .timeout(Duration.ofSeconds(30)); // Таймаут на весь запрос

            // Блокируем, чтобы получить результат синхронно, как в оригинале
            String responseBody = responseMono.block();
            logger.debug("Успешно получен ответ от {}", urlString);
            return responseBody;

        } catch (WebClientResponseException e) {
            logger.error("Ошибка HTTP при запросе {}: Status={}, Headers={}", urlString, e.getStatusCode(), e.getHeaders(), e);
            throw new Exception("HTTP Error: " + e.getStatusCode() + " for URL: " + urlString, e);
        } catch (Exception e) {
            logger.error("Неизвестная ошибка при запросе к {}", urlString, e);
            throw e; // Пробрасываем исключение дальше
        }
    }
}