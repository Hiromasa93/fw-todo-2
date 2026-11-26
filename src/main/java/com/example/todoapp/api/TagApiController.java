package com.example.todoapp.api;

import java.util.List;
import com.example.todoapp.Tag;
import com.example.todoapp.TagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TagApiController {
    private final TagService tagService;

    public TagApiController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/api/tags/search")
    public List<Tag> search(@RequestParam(defaultValue = "") String q) {
        return tagService.search(q);
    }

    @PostMapping("/api/tags")
    public Tag create(@RequestBody CreateTagRequest request) {
        return tagService.create(request.name());
    }

    public record CreateTagRequest(String name) {
    }
}
