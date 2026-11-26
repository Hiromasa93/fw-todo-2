package com.example.todoapp;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TagMapper {
    int insertIfAbsent(@Param("name") String name);
    Tag findByName(@Param("name") String name);
    List<Tag> search(@Param("pattern") String pattern);
    List<Tag> findByIds(@Param("ids") List<Long> ids);
    List<Tag> findByTodoId(Long todoId);
    int deleteTodoTags(Long todoId);
    int insertTodoTags(@Param("todoId") Long todoId, @Param("ids") List<Long> ids);
}
