package com.example.todoapp;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GamificationMapper {
    int addPoints(@Param("points") int points);
    int insertHistory(@Param("points") int points, @Param("action") String action, @Param("todoId") Long todoId);
    int totalPoints();
    List<PointHistory> findHistory();
}
