package ru.project_bot.finance.bot.service.impl;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.project_bot.finance.bot.model.CallbackData;
import ru.project_bot.finance.bot.model.SuggestionsData;
import ru.project_bot.finance.bot.service.KeyboardService;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeyboardServiceImpl implements KeyboardService {

    @Override
    public ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Курс валют (/rate)");
        row1.add("Цена акций (/price)");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Поиск (/search)");
        row2.add("Помощь (/help)");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        return keyboardMarkup;
    }

    @Override
    public InlineKeyboardMarkup createSuggestionsKeyboard(List<SuggestionsData> suggestions) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (SuggestionsData suggestion : suggestions) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(suggestion.getSuggestion());

            // ✅ ИСПРАВЛЕНО: Используем методы CallbackData вместо прямой конкатенации
            if (CallbackData.CURRENCY_PREFIX.equals(suggestion.getType())) {
                button.setCallbackData(CallbackData.createCurrencyCallback(suggestion.getSuggestion()));
            } else if (CallbackData.STOCK_PREFIX.equals(suggestion.getType())) {
                button.setCallbackData(CallbackData.createStockCallback(suggestion.getSuggestion()));
            } else {
                // fallback для неизвестных типов
                button.setCallbackData(suggestion.getType() + suggestion.getSuggestion());
            }

            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        // ✅ ИСПРАВЛЕНО: Используем CallbackData.createCloseCallback() вместо "close"
        List<InlineKeyboardButton> closeRow = new ArrayList<>();
        InlineKeyboardButton closeButton = new InlineKeyboardButton();
        closeButton.setText("❌ Закрыть");
        closeButton.setCallbackData(CallbackData.createCloseCallback());
        closeRow.add(closeButton);
        rowsInline.add(closeRow);

        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    @Override
    public InlineKeyboardMarkup createSearchPaginationKeyboard(String query, String searchType,
                                                               int currentPage, int totalPages) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> navigationRow = new ArrayList<>();

        // Кнопка "Назад"
        if (currentPage > 0) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton();
            prevButton.setText("⬅️ Назад");
            // ✅ Используем обновленный метод с query
            prevButton.setCallbackData(CallbackData.createSearchPageCallback(searchType, currentPage - 1, query));
            navigationRow.add(prevButton);
        }

        // Информация о странице
        InlineKeyboardButton pageInfoButton = new InlineKeyboardButton();
        pageInfoButton.setText((currentPage + 1) + "/" + totalPages);
        pageInfoButton.setCallbackData("page_info");
        navigationRow.add(pageInfoButton);

        // Кнопка "Вперед"
        if (currentPage < totalPages - 1) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("Вперед ➡️");
            // ✅ ИСПользуем обновленный метод с query
            nextButton.setCallbackData(CallbackData.createSearchPageCallback(searchType, currentPage + 1, query));
            navigationRow.add(nextButton);
        }

        // Добавляем информацию о странице только, если есть кнопки навигации
        if ((currentPage > 0) || (currentPage < totalPages - 1)) {
            rowsInline.add(navigationRow);
        }

        // Кнопка закрытия
        List<InlineKeyboardButton> closeRow = new ArrayList<>();
        InlineKeyboardButton closeButton = new InlineKeyboardButton();
        closeButton.setText("❌ Закрыть");
        closeButton.setCallbackData(CallbackData.createCloseCallback());
        closeRow.add(closeButton);
        rowsInline.add(closeRow);

        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }
}