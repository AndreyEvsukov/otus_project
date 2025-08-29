package ru.project_bot.finance.bot.model;

import lombok.*;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SuggestionsData {
    public String suggestion;
    public String type;
}
