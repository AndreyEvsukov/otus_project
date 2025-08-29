package ru.project_bot.finance.api.moex.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.project_bot.finance.api.moex.MoexApiClient;
import ru.project_bot.finance.api.moex.model.MxdResponse;
import ru.project_bot.finance.api.moex.model.ShareData;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class MoexApiClientImpl implements MoexApiClient {

    private static final String URI_TO_SECURITIES = "engines/stock/markets/shares/securities.json";
    private static final String URI_TO_SHARES_PRICES = "engines/stock/markets/shares/boards/TQBR/securities/";

    @Autowired
    @Setter
    private ObjectMapper mapper;

    @Autowired
    @Qualifier("moexWebClient")
    private WebClient moexWebClient;

    @Override
    public MxdResponse getSecurities() {
        try {
            val response = moexWebClient.get().uri(URI_TO_SECURITIES)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return processData(response, "securities");
        }
        catch (Exception ex) {
            log.error("MoexDataApi error get securities", ex);
            throw ex;
        }
    }

    @Override
    public ShareData getSharePrice(String ticker) {
        val uriToSharesPrices = URI_TO_SHARES_PRICES + ticker + ".json";
        try {
            val response = moexWebClient.get().uri(uriToSharesPrices)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            val marketData = processData(response, "marketdata");
            val securityData = processData(response, "securities");
            return ShareData.builder()
                    .marketData(marketData)
                    .securityData(securityData)
                    .build();
        }
        catch (Exception ex) {
            log.error("MoexDataApi error get prices for share: {}", ticker, ex);
            throw ex;
        }
    }

    @SneakyThrows
    MxdResponse processData(String serviceResponse, String rootPath) {
        val documentTree = mapper.readTree(serviceResponse);
        val data = documentTree.path(rootPath).path("data");
        val metadata = (ObjectNode) documentTree.path(rootPath).path("metadata");
        val columns = documentTree.path(rootPath).path("columns");
        val columnsList = mapper.convertValue(columns, new TypeReference<List<String>>() {});

        val metaMap = StreamSupport.stream(Spliterators.spliteratorUnknownSize(metadata.properties().iterator(), Spliterator.ORDERED), false)
                .map(fld -> {
                    val fieldType = ((ObjectNode) fld.getValue()).get("type").asText();
                    return Tuple.of(fld.getKey(), fieldType);
                }).collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));

        val columnsData = StreamSupport.stream(Spliterators.spliteratorUnknownSize(data.iterator(), Spliterator.ORDERED), false)
                .map(elem -> {
                    Map<String, String> ret = new HashMap<>();
                    val n = (ArrayNode) elem;
                    for (int i = 0; i < n.size() && i < columnsList.size(); i++) {
                        ret.put(columnsList.get(i).toLowerCase(), n.get(i).asText());
                    }
                    return ret;
                }).collect(Collectors.toList());

        return new MxdResponse(metaMap, columnsList, columnsData);
    }
}
