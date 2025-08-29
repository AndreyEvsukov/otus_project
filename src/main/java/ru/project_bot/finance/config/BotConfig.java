package ru.project_bot.finance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class BotConfig {
    private static final Logger logger = LoggerFactory.getLogger(BotConfig.class);

    @Bean
    public SetMyCommands botCommands() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "Главное меню"));
        commands.add(new BotCommand("/rate", "Курс валют"));
        commands.add(new BotCommand("/price", "Цена акций"));
        commands.add(new BotCommand("/search", "Поиск инструмента"));
        // ✅ ДОБАВЛЯЕМ специализированные команды поиска
        commands.add(new BotCommand("/search_currency", "Поиск валют"));
        commands.add(new BotCommand("/search_stock", "Поиск акций"));
        commands.add(new BotCommand("/help", "Помощь"));

        SetMyCommands setMyCommands = new SetMyCommands(commands, new BotCommandScopeDefault(), null);
        logger.info("Bot commands configured: {}", commands.size());
        return setMyCommands;
    }
}
