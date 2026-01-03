package com.xinji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinji.entity.CheerQuote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CheerQuoteMapper extends BaseMapper<CheerQuote> {

    @Select("SELECT * FROM t_cheer_quote ORDER BY RAND() LIMIT 1")
    CheerQuote selectRandomOne();
}





