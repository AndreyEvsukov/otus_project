package ru.project_bot.finance.api.moex.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MxdResponse {
    public Map<String,String> metadata;
    public List<String> columns;
    public List<Map<String,String>> data;
}
