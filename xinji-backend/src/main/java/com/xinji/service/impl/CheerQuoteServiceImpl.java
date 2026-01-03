package com.xinji.service.impl;

import com.xinji.entity.CheerQuote;
import com.xinji.mapper.CheerQuoteMapper;
import com.xinji.service.CheerQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheerQuoteServiceImpl implements CheerQuoteService {

    private final CheerQuoteMapper cheerQuoteMapper;

    @Override
    public CheerQuote getRandomQuote() {
        return cheerQuoteMapper.selectRandomOne();
    }

    @Override
    public void addQuote(String content) {
        CheerQuote cheerQuote = new CheerQuote();
        cheerQuote.setContent(content);
        cheerQuoteMapper.insert(cheerQuote);
    }
}
