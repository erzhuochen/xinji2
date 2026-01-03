package com.xinji.dto.response;

import lombok.Data;

import java.util.List;

/**
 * AI生成的周报总结
 */
@Data
public class AiWeeklySummary {
    private String summary;
    private List<String> suggestions;
    private List<String> actionPoints;
}

