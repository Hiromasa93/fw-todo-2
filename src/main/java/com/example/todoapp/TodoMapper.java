package com.example.todoapp;

import java.util.List;
import java.time.LocalDate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TodoMapper {

    List<Todo> search(@Param("keyword") String keyword, @Param("category") String category,
            @Param("order") String order, @Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("includeCompleted") boolean includeCompleted, @Param("trash") boolean trash);

    long count(@Param("keyword") String keyword, @Param("category") String category,
            @Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("includeCompleted") boolean includeCompleted, @Param("trash") boolean trash);

    List<Todo> searchPage(@Param("keyword") String keyword, @Param("category") String category,
            @Param("order") String order, @Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("includeCompleted") boolean includeCompleted, @Param("trash") boolean trash,
            @Param("limit") int limit, @Param("offset") long offset);

    Todo findById(Long id);

    int insert(Todo todo);

    int update(Todo todo);

    int togglePinned(Long id);

    int deleteById(Long id);

    int deleteByIds(@Param("ids") List<Long> ids);

    int restoreById(Long id);
}
