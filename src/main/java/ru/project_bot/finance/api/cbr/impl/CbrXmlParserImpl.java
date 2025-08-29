package ru.project_bot.finance.api.cbr.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import ru.project_bot.finance.api.cbr.CbrXmlParser;
import ru.project_bot.finance.api.cbr.model.CurrencyInfo;
import ru.project_bot.finance.api.cbr.model.CurrencyRate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class CbrXmlParserImpl implements CbrXmlParser {

    private static final Logger logger = LoggerFactory.getLogger(CbrXmlParserImpl.class);

    /**
     * Парсит XML справочника валют из XML_valFull.asp
     * Структура: <Valuta><Item ID="R01010"><Name>...</Name>...<ISO_Num_Code>36</ISO_Num_Code><ISO_Char_Code>AUD</ISO_Char_Code></Item>...</Valuta>
     */
    public List<CurrencyInfo> parseCurrencyDictionary(String xmlContent) {
        List<CurrencyInfo> currencies = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

            NodeList itemNodes = document.getElementsByTagName("Item");
            logger.debug("Найдено {} элементов Item в XML справочника", itemNodes.getLength());

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Element itemElement = (Element) itemNodes.item(i);
                CurrencyInfo info = new CurrencyInfo();

                // ID из атрибута
                info.setCode(itemElement.getAttribute("ID"));

                // Поля из тегов
                info.setName(getTextContent(itemElement, "Name"));
                info.setEngName(getTextContent(itemElement, "EngName"));
                info.setNominal(parseInt(getTextContent(itemElement, "Nominal")));

                info.setNumCode(parseInt(getTextContent(itemElement, "ISO_Num_Code")));
                info.setCharCode(getTextContent(itemElement, "ISO_Char_Code"));

                currencies.add(info);

                // Логируем первые несколько для отладки
                if (i < 3) {
                    logger.debug("Справочник: код={}, название={}, CharCode={}, NumCode={}",
                            info.getCode(), info.getName(), info.getCharCode(), info.getNumCode());
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing currency dictionary XML", e);
            // Логируем начало XML для диагностики
            if (xmlContent != null && !xmlContent.isEmpty()) {
                logger.error("XML content sample: {}", xmlContent.substring(0, Math.min(1000, xmlContent.length())));
            }
        }
        return currencies;
    }

    /**
     * Парсит XML курсов валют из XML_daily.asp
     * Структура: <ValCurs Date="22.08.2025"> <Valute ID="R01010"><NumCode>036</NumCode><CharCode>AUD</CharCode>...</Valute>...</ValCurs>
     */
    public List<CurrencyRate> parseCurrencyRates(String xmlContent) {
        List<CurrencyRate> rates = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

            String dateStr = document.getDocumentElement().getAttribute("Date");
            logger.debug("Дата из XML курсов: {}", dateStr);

            // Проверка на пустую дату
            if (dateStr.isEmpty()) {
                logger.warn("Атрибут Date отсутствует в корневом элементе XML курсов");
                // Можно попробовать получить дату из другого места или использовать текущую
                dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                logger.warn("Используем текущую дату: {}", dateStr);
            }

            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            NodeList valuteNodes = document.getElementsByTagName("Valute");
            logger.debug("Найдено {} валют в XML курсов", valuteNodes.getLength());

            for (int i = 0; i < valuteNodes.getLength(); i++) {
                Element valuteElement = (Element) valuteNodes.item(i);
                CurrencyRate rate = new CurrencyRate();

                rate.setCharCode(getTextContent(valuteElement, "CharCode"));
                rate.setNumCode(parseInt(getTextContent(valuteElement, "NumCode")));
                rate.setNominal(parseInt(getTextContent(valuteElement, "Nominal")));
                rate.setName(getTextContent(valuteElement, "Name"));
                String valueRate = getTextContent(valuteElement, "Value");
                if (!valueRate.isEmpty()) {
                    valueRate = valueRate.replace(",", ".");
                    rate.setRate(new BigDecimal(valueRate));
                } else {
                    logger.warn("Пустое значение курса для валюты {}", getTextContent(valuteElement, "CharCode"));
                    rate.setRate(BigDecimal.ZERO);
                }
                rate.setDate(date);

                String normalizeValueRate = getTextContent(valuteElement, "VunitRate");
                if (!normalizeValueRate.isEmpty()) {
                    normalizeValueRate = normalizeValueRate.replace(",", ".");
                    rate.setNormalizeRate(new BigDecimal(normalizeValueRate));
                } else {
                    logger.warn("Пустое значение нормализованного курса для валюты {}", getTextContent(valuteElement, "CharCode"));
                    rate.setNormalizeRate(BigDecimal.ZERO);
                }

                rates.add(rate);

                // Логируем первые несколько для отладки
                if (i < 3) {
                    logger.debug("Курс: {}={} за {}", rate.getCharCode(), rate.getRate(), rate.getNominal());
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing currency rates XML", e);
            if (xmlContent != null && !xmlContent.isEmpty()) {
                logger.error("XML content sample: {}", xmlContent.substring(0, Math.min(1000, xmlContent.length())));
            }
        }
        return rates;
    }

    private static String getTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            String content = nodeList.item(0).getTextContent();
            return content != null ? content.trim() : "";
        }
        return "";
    }

    private static int parseInt(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Не удалось преобразовать '{}' в число: {}", value, e.getMessage());
            return 0;
        }
    }
}