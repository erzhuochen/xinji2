package com.xinji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinji.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {
    
    /**
     * 根据手机号哈希查询用户
     */
    @Select("SELECT * FROM t_user WHERE phone_hash = #{phoneHash} AND deleted = 0")
    User findByPhoneHash(@Param("phoneHash") String phoneHash);
    
    /**
     * 统计用户日记数量
     */
    @Select("SELECT COUNT(*) FROM t_diary WHERE user_id = #{userId} AND deleted = 0")
    Integer countDiaries(@Param("userId") String userId);
}
