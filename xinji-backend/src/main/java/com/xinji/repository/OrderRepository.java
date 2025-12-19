package com.xinji.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinji.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单数据访问层
 */
@Mapper
public interface OrderRepository extends BaseMapper<Order> {
    
    /**
     * 分页查询用户订单
     */
    @Select("<script>" +
            "SELECT * FROM t_order WHERE user_id = #{userId} " +
            "<if test='status != null'>AND status = #{status} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    IPage<Order> findByUserIdPage(Page<Order> page, 
                                   @Param("userId") String userId,
                                   @Param("status") String status);
    
    /**
     * 查询超时未支付订单
     */
    @Select("SELECT * FROM t_order WHERE status = 'PENDING' AND expire_at < #{now}")
    List<Order> findExpiredOrders(@Param("now") LocalDateTime now);
    
    /**
     * 批量更新订单状态为已过期
     */
    @Update("<script>" +
            "UPDATE t_order SET status = 'EXPIRED', update_time = NOW() " +
            "WHERE order_id IN " +
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateExpired(@Param("orderIds") List<String> orderIds);
}
