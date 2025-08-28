package ru.project_bot.finance.bot.service.impl;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.project_bot.finance.bot.model.CallbackData;
import ru.project_bot.finance.bot.model.SuggestionsData;
import ru.project_bot.finance.bot.service.*;
import ru.project_bot.finance.util.PaginationUtil;

import java.util.ArrayList;
import java.util.List;

@Service
    public class MessageServiceImpl implements MessageService {

    @Autowired
    private TelegramSenderService telegramSenderService;
    @Autowired
    private KeyboardService keyboardService;
    @Autowired
    private DataService dataService;
    @Autowired
    private SearchService searchService;

    @Override
    public void sendMainMenu(long chatId) {
        val message = createSimpleMessage(chatId,"Добро пожаловать! Выберите действие:", keyboardService.createMainMenuKeyboard());
        telegramSenderService.execute(message);
    }

    @Override
    public void sendCurrencyInstructions(long chatId) {
        val messageText = """
                Введите код валюты (например, USD, EUR, CNY) или используйте /search для поиска.
                
                Примеры популярных валют:
                • USD - Доллар США
                • EUR - Евро
                • CNY - Китайский юань
                • JPY - Японская иена
                • GBP - Фунт стерлингов""";
        sendMessage(chatId, messageText);
    }

    @Override
    public void sendStockInstructions(long chatId) {
        val messageText = """
                Введите код акции или используйте /search для поиска.
                
                Примеры акций:
                • SBER - Сбербанк
                • GAZP - Газпром
                • LKOH - Лукойл
                • GMKN - Норильский никель
                • ROSN - Роснефть""";
        sendMessage(chatId, messageText);
    }

    @Override
    public void sendSearchInstructions(long chatId) {
        val messageText = """
                Введите поисковый запрос для поиска валют или акций.
                
                Примеры:
                • "доллар" - для поиска валют
                • "сбер" - для поиска акций
                • "eur" - для поиска по коду
                
                Или используйте:
                /search_currency [запрос] - поиск только по валютам
                /search_stock [запрос] - поиск только по акциям""";
        sendMessage(chatId, messageText);
    }

    @Override
    public void sendMessage(long chatId, String text) {
        val message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        telegramSenderService.execute(message);
    }

    @Override
    public void handleInstrumentInput(long chatId, String input) {
        val code = input.trim().toUpperCase();

        // Проверяем валюту
        if (dataService.isCurrencyExists(code)) {
            sendCurrencyRate(chatId, code);
            return;
        }

        // Проверяем акцию
        if (dataService.isStockExists(code)) {
            sendStockPrice(chatId, code);
            return;
        }

        // Предлагаем подсказки
        List<String> suggestions = searchService.getAutocompleteSuggestions(code, 5);
        if (!suggestions.isEmpty()) {
            sendSuggestions(chatId, suggestions, code);
        } else {
            sendMessage(chatId,
                    "Инструмент не найден. Попробуйте другой код или используйте /search для поиска.");
        }
    }

    @Override
    public void sendCurrencyRate(long chatId, String currencyCode) {
        Message tempMessage = telegramSenderService.sendMessage(
                createSimpleMessage(chatId, "🔍 Ищу курс валюты " + currencyCode + "...")
        );

        val rate = dataService.getNormalizedCurrencyRate(currencyCode);
        String response;

        if (rate != null) {
            response = String.format("💰 Курс валюты %s: %.4f RUB", currencyCode, rate);
        } else {
            val currencyName = dataService.getCurrencyName(currencyCode);
            response = String.format("Курс для валюты %s (%s) не найден.", currencyCode , currencyName);
        }

        if (tempMessage != null) {
            simpleEditMessage(chatId, tempMessage.getMessageId(), response);
        }
        else {
            sendMessage(chatId, response);
        }
    }

    @Override
    public void sendStockPrice(long chatId, String stockCode) {
        Message tempMessage = telegramSenderService.sendMessage(
                createSimpleMessage(chatId, "🔍 Ищу цену акции " + stockCode + "...")
        );

        val price = dataService.getStockPrice(stockCode);
        String response;

        if (price != null) {
            response = String.format("📈 Цена акции %s: %.4f RUB", stockCode, price);
        } else {
            val stockName = dataService.getStockName(stockCode);
            response = String.format("Цена для акции %s (%s)  не найдена.", stockCode, stockName );
        }

        if (tempMessage != null) {
            simpleEditMessage(chatId, tempMessage.getMessageId(), response);
        }
        else {
            sendMessage(chatId, response);
        }
    }

    @Override
    public void sendSuggestions(long chatId, List<String> suggestions, String query) {
        val suggestionsData = new ArrayList<SuggestionsData>(suggestions.size());
        val message = new StringBuilder();
        message.append("🔍 Найдено по запросу '").append(query).append("':\n\n");

        for (String suggestion : suggestions) {
            if (dataService.isCurrencyExists(suggestion)) {
                message.append("💰 ").append(suggestion);
                val currencyName = dataService.getCurrencyName(suggestion);
                if (currencyName != null) {
                    message.append(" (")
                           .append(currencyName)
                           .append(" )");
                }
                suggestionsData.add(SuggestionsData.builder()
                        .suggestion(suggestion)
                        .type(CallbackData.CURRENCY_PREFIX).build()
                );
            } else if (dataService.isStockExists(suggestion)) {
                message.append("📊 ").append(suggestion);
                val stockName = dataService.getStockName(suggestion);
                if (stockName != null) {
                    message.append(" (")
                           .append(stockName)
                           .append(" )");
                }
                suggestionsData.add(SuggestionsData.builder()
                        .suggestion(suggestion)
                        .type(CallbackData.STOCK_PREFIX).build()
                );
            }
            message.append("\n");
        }

        message.append("\nНажмите на код выше или введите точный код из списка.");

        // Добавляем кнопки для первых 5 результатов
        InlineKeyboardMarkup markup = keyboardService.createSuggestionsKeyboard(suggestionsData);
        val sendMessage = createSimpleMessage(chatId, message.toString(), markup);
        telegramSenderService.execute(sendMessage);
    }

    @Override
    public void sendSearchResults(long chatId, String query, String searchType, int page) {
        List<String> results;
        val pageSize = 10;

        if ("currency".equals(searchType)) {
            results = searchService.searchCurrencies(query);
        } else if ("stock".equals(searchType)) {
            results = searchService.searchStocks(query);
        } else {
            results = searchService.searchAll(query);
        }

        if (results.isEmpty()) {
            sendMessage(chatId, "По запросу '" + query + "' ничего не найдено.");
            return;
        }

        val pageResults = PaginationUtil.getPage(results, page, pageSize);
        val totalPages = PaginationUtil.getTotalPages(results.size(), pageSize);

        val message = new StringBuilder();
        message.append("🔍 Результаты поиска для: '").append(query).append("':\n");
        message.append("Страница ").append(page + 1).append(" из ").append(totalPages).append("\n\n");

        for (String result : pageResults) {
            if (dataService.isCurrencyExists(result)) {
                message.append("💰 ").append(result);
                val currencyName = dataService.getCurrencyName(result);
                if (currencyName != null) {
                    message.append(" (")
                            .append(currencyName)
                            .append(" )");
                }
            } else if (dataService.isStockExists(result)) {
                message.append("📊 ").append(result);
                val stockName = dataService.getStockName(result);
                if (stockName != null) {
                    message.append(" (")
                            .append(stockName)
                            .append(" )");
                }
            }
            message.append("\n");
        }

        InlineKeyboardMarkup markup = keyboardService.createSearchPaginationKeyboard(
                query, searchType, page, totalPages);
        val sendMessage = createSimpleMessage(chatId, message.toString(), markup);

        telegramSenderService.execute(sendMessage);
    }

    @Override
    public void sendHelpMessage(long chatId) {
        String helpText = """
                🤖 Справка по боту:
                
                Команды:
                /start - Главное меню
                /rate - Курс валют
                /price - Цена акций
                /search - Поиск инструментов
                /help - Эта справка
                
                Как пользоваться:
                1. Введите код валюты или акции напрямую
                2. Используйте /search для поиска
                3. Получайте подсказки при вводе""";

        sendMessage(chatId, helpText);
    }

    @Override
    public void removeInlineKeyboard(long chatId, int messageId) {
        simpleEditMessage(chatId, messageId, "Выбор сделан ✅");
    }

    private SendMessage createSimpleMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        return message;
    }

    private SendMessage createSimpleMessage(long chatId, String text, ReplyKeyboard markup) {
        val message = createSimpleMessage(chatId, text);
        message.setReplyMarkup(markup);
        return message;
    }

    private void simpleEditMessage(long chatId, int messageId, String messageText) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        editMessage.setText(messageText);

        telegramSenderService.editMessageText(editMessage);
    }
}