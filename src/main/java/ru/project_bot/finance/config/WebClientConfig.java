package ru.project_bot.finance.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Конфигурация WebClient бинов для внешних API к бирже и к ЦБ.
 */
@Configuration
public class WebClientConfig {
    // Константа для базового адреса API биржи
    public static final String MOEX_BASE_URL = "http://iss.moex.com/iss/";

    /**
     * Создает WebClient, сконфигурированный для работы с API ЦБ
     *
     * @return Сконфигурированный экземпляр WebClient.
     */
    @Bean
    @Qualifier("cbrWebClient")
    public WebClient cbrWebClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .followRedirect(true)
                         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                         .responseTimeout(Duration.ofSeconds(30))
                ))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024) // 1MB (с запасом)
                )
                .build();
    }

    /**
     * Создает WebClient, сконфигурированный для работы с API биржи
     *
     * @return Сконфигурированный экземпляр WebClient
     */
    @Bean
    @Qualifier("moexWebClient")
    public WebClient moexWebClient() {
        return WebClient.builder()
                .baseUrl(MOEX_BASE_URL)  // Устанавливаем базовый URL для MOEX API
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .followRedirect(true)
                ))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024) // 16MB (с запасом)
                )
                .defaultHeader("Content-type", "application/json")
                .build();
    }

}
