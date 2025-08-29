package ru.project_bot.finance.api.moex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SharePrice {
    private String ticker;
    private Double priceValue;
    private LocalDateTime priceDate;
}
