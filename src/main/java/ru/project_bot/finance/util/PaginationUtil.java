package ru.project_bot.finance.util;

import java.util.List;

public class PaginationUtil {

    public static <T> List<T> getPage(List<T> list, int page, int pageSize) {
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, list.size());

        if (fromIndex > list.size()) {
            return List.of();
        }

        return list.subList(fromIndex, toIndex);
    }

    public static int getTotalPages(int totalItems, int pageSize) {
        return (int) Math.ceil((double) totalItems / pageSize);
    }
}