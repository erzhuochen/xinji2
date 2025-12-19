package com.xinji.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinji.entity.Diary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 日记数据访问层
 */
@Mapper
public interface DiaryRepository extends BaseMapper<Diary> {
    
    /**
     * 分页查询用户日记
     */
    @Select("<script>" +
            "SELECT * FROM t_diary WHERE user_id = #{userId} AND deleted = 0 " +
            "<if test='startDate != null'>AND diary_date &gt;= #{startDate} </if>" +
            "<if test='endDate != null'>AND diary_date &lt;= #{endDate} </if>" +
            "ORDER BY diary_date DESC, create_time DESC" +
            "</script>")
    IPage<Diary> findByUserIdPage(Page<Diary> page, 
                                   @Param("userId") String userId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
    
    /**
     * 查询指定日期范围内的日记(用于周报)
     */
    @Select("SELECT * FROM t_diary WHERE user_id = #{userId} " +
            "AND diary_date >= #{startDate} AND diary_date <= #{endDate} " +
            "AND deleted = 0 ORDER BY diary_date ASC")
    List<Diary> findByUserIdAndDateRange(@Param("userId") String userId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
    
    /**
     * 查询用户最近的日记(用于风险检测)
     */
    @Select("SELECT * FROM t_diary WHERE user_id = #{userId} AND analyzed = 1 " +
            "AND deleted = 0 ORDER BY diary_date DESC LIMIT #{limit}")
    List<Diary> findRecentAnalyzed(@Param("userId") String userId, @Param("limit") int limit);
}
