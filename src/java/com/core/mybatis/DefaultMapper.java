package com.core.mybatis;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface DefaultMapper {
    @Select("${value}")
    Map<String, Object> getOne(String sql);

    @Select("${value}")
    List<Map<String, Object>> getList(String sql);

    @Update("${value}")
    int exec(String sql);
}
