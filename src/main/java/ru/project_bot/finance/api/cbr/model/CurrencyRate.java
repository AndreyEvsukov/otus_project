package ru.project_bot.finance.api.cbr.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Модель для курса валюты к рублю.
 */
@Data
public class CurrencyRate {
    private String charCode;    // Символьный код валюты (VchCode)
    private int numCode;        // Цифровой код валюты (Vcode)
    private int nominal;        // Номинал (Vnom)
    private BigDecimal rate;    // Курс (Vcurs)
    private String name;        // Название валюты (Vname)
    private LocalDate date;     // Дата, на которую предоставлен курс
    private BigDecimal normalizeRate; // Курс за рубль (VunitRate)
}