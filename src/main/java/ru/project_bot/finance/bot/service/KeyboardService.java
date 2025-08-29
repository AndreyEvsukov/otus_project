package ru.project_bot.finance.bot.service;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.project_bot.finance.bot.model.SuggestionsData;

import java.util.List;

public interface KeyboardService {
    ReplyKeyboardMarkup createMainMenuKeyboard();

    InlineKeyboardMarkup createSuggestionsKeyboard(List<SuggestionsData> suggestions);

    InlineKeyboardMarkup createSearchPaginationKeyboard(String query, String searchType,
                                                        int currentPage, int totalPages);
}
