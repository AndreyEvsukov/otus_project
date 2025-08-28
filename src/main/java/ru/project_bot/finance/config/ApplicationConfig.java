package ru.project_bot.finance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.project_bot.finance.api.moex.MoexApiClient;
import ru.project_bot.finance.api.moex.impl.MoexApiClientImpl;

@Configuration
public class ApplicationConfig {
    @Bean
    public ObjectMapper mapper() {
        return new ObjectMapper();
    }

    @Bean
    public MoexApiClient moexDataApi() {
        return new MoexApiClientImpl();
    }
}
