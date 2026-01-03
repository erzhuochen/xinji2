package com.xinji.service;

import com.xinji.entity.CheerQuote;

public interface CheerQuoteService {

    CheerQuote getRandomQuote();

    void addQuote(String content);
}





