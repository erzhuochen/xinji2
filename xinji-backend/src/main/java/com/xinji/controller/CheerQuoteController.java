package com.xinji.controller;

import com.xinji.dto.request.AddQuoteRequest;
import com.xinji.dto.response.ApiResponse;
import com.xinji.entity.CheerQuote;
import com.xinji.service.CheerQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cheer-quotes")
@RequiredArgsConstructor
public class CheerQuoteController {

    private final CheerQuoteService cheerQuoteService;

    @GetMapping("/random")
    public ApiResponse<CheerQuote> getRandomQuote() {
        return ApiResponse.success(cheerQuoteService.getRandomQuote());
    }

    @PostMapping
    public ApiResponse<Void> addQuote(@RequestBody AddQuoteRequest request) {
        cheerQuoteService.addQuote(request.getContent());
        return ApiResponse.success("添加成功");
    }
}
