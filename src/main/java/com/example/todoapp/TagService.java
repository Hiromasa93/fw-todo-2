package com.example.todoapp;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TagService {
    private final TagMapper tagMapper;

    public TagService(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Transactional
    public Tag create(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "タグ名は1〜100文字で入力してください");
        }
        String normalized = name.strip();
        tagMapper.insertIfAbsent(normalized);
        return tagMapper.findByName(normalized);
    }

    public List<Tag> search(String query) {
        if (query == null || query.isBlank() || query.strip().length() > 100) {
            return List.of();
        }
        String escaped = query.strip().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return tagMapper.search("%" + escaped + "%");
    }

    public List<Tag> resolve(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> distinct = ids.stream().distinct().toList();
        if (distinct.size() > 100 || distinct.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "タグの指定が不正です");
        }
        List<Tag> tags = tagMapper.findByIds(distinct);
        if (tags.size() != distinct.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "存在しないタグが指定されています");
        }
        return tags;
    }
}
