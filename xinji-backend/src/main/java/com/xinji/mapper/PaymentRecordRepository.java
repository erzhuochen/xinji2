package com.xinji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinji.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付流水数据访问层
 */
@Mapper
public interface PaymentRecordRepository extends BaseMapper<PaymentRecord> {
}
