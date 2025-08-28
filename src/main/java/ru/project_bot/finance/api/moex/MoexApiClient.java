package ru.project_bot.finance.api.moex;

import ru.project_bot.finance.api.moex.model.MxdResponse;
import ru.project_bot.finance.api.moex.model.ShareData;

public interface MoexApiClient {
    MxdResponse getSecurities();
    ShareData getSharePrice(String ticker);
}
