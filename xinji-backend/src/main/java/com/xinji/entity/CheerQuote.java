package com.xinji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_cheer_quote")
public class CheerQuote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    private LocalDateTime createTime;
}





