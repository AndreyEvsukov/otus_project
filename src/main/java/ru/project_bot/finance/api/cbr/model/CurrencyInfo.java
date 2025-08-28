package ru.project_bot.finance.api.cbr.model;

import lombok.Data;

/**
 * Модель для справочника валют ЦБ РФ.
 */
@Data
public class CurrencyInfo {
    private String code;        // Внутренний код валюты (Vcode)
    private String name;        // Название валюты (Vname)
    private String engName;     // Английское название (VEngname)
    private int nominal;        // Номинал (Vnom)
    private int numCode;        // Цифровой код ISO (VnumCode)
    private String charCode;    // Символьный код ISO (VcharCode)
}