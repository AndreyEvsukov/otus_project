package ru.project_bot.finance.api.moex;

import java.util.List;
import java.util.Map;

public interface MoexCacheService {
    List<String> getActiveSharesList();
    Map<String, String> getActiveSharesMap();
    Map<String, Double> getSharePrices();
}
