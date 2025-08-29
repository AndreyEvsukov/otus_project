package ru.project_bot.finance.api.moex.model;

import lombok.*;

@Data
@ToString
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ShareData {
    public MxdResponse marketData;
    public MxdResponse securityData;
}
