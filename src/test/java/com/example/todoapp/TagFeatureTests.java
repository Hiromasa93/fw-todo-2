package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TagFeatureTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TodoService todos;
    @Autowired TagService tags;

    private Long tag(String name) {
        jdbc.update("INSERT INTO tags (name) VALUES (?)", name);
        return jdbc.queryForObject("SELECT id FROM tags WHERE name = ?", Long.class, name);
    }

    private Todo todo() {
        Todo todo = new Todo();
        todo.setTitle("tag-test-" + UUID.randomUUID());
        todo.setCategory("資格");
        todo.setPriority(2);
        todo.setCompleted(false);
        return todo;
    }

    @Test
    void createTagValidatesNameAndReusesExistingTag() throws Exception {
        String name = "買い物-" + UUID.randomUUID();
        mvc.perform(post("/api/tags").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  " + name + "  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(name));
        Long id = jdbc.queryForObject("SELECT id FROM tags WHERE name = ?", Long.class, name);
        mvc.perform(post("/api/tags").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tags WHERE name = ?", Long.class, name)).isEqualTo(1);
        mvc.perform(get("/api/tags/search").param("q", name))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        for (String body : List.of("{}", "{\"name\":null}", "{\"name\":\"   \"}",
                "{\"name\":\"" + "x".repeat(101) + "\"}")) {
            mvc.perform(post("/api/tags").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        Todo todo = todo();
        todo.setTagIds(List.of(id));
        todos.create(todo);
        assertThat(todos.findById(todo.getId()).getTags()).extracting(Tag::getName).containsExactly(name);
    }

    @Test
    void searchReturnsJsonAndTreatsWildcardsAsLiteralCharacters() throws Exception {
        String prefix = UUID.randomUUID().toString();
        Long id = tag(prefix + "日本語%_!");
        tag(prefix + "other");
        mvc.perform(get("/api/tags/search").param("q", "  " + prefix + "日本語%_!  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].name").value(prefix + "日本語%_!"));
        for (String query : List.of("", "   ", "unknown-" + prefix, "x".repeat(101))) {
            mvc.perform(get("/api/tags/search").param("q", query))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }
        for (int i = 0; i < 22; i++) tag(prefix + "limit-" + i);
        mvc.perform(get("/api/tags/search").param("q", prefix + "limit-"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(20));
    }

    @Test
    void selectedTagsSurviveCreateConfirmReviseValidationAndEdit() throws Exception {
        String name = "日本語<script>" + UUID.randomUUID();
        Long id = tag(name);
        String tagId = id.toString();
        for (String path : List.of("/todos/confirm", "/todos/new")) {
            String html = mvc.perform(post(path).param("title", "タグ付きToDo")
                    .param("category", "資格").param("priority", "2").param("tagIds", tagId))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(html).contains("name=\"tagIds\"", "value=\"" + id + "\"", "&lt;script&gt;");
        }
        String invalid = mvc.perform(post("/todos/confirm").param("title", "")
                .param("tagIds", tagId)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(invalid).contains("name=\"tagIds\"", "value=\"" + id + "\"");
        String title = "form-tag-" + UUID.randomUUID();
        mvc.perform(post("/todos").param("title", title).param("category", "資格")
                .param("priority", "2").param("tagIds", tagId, tagId)).andExpect(status().is3xxRedirection());
        Long todoId = jdbc.queryForObject("SELECT id FROM todos WHERE title = ?", Long.class, title);
        assertThat(todos.findById(todoId).getTagIds()).containsExactly(id);
        for (String path : List.of("/todos/" + todoId + "/edit", "/todos/" + todoId + "/confirm")) {
            String html = mvc.perform(post(path).param("title", title).param("category", "資格")
                    .param("priority", "2").param("completed", "false").param("tagIds", tagId))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(html).contains("name=\"tagIds\"", "value=\"" + id + "\"");
        }
        String edit = mvc.perform(get("/todos/" + todoId + "/edit")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(edit).contains("value=\"" + id + "\"", "/js/tags.js");
        mvc.perform(post("/todos/" + todoId).param("title", title).param("category", "資格")
                .param("priority", "2").param("completed", "false").param("_tagIds", "on"))
                .andExpect(status().is3xxRedirection());
        assertThat(todos.findById(todoId).getTagIds()).isEmpty();
    }

    @Test
    void updatesReplaceTagsAndOmittingTagsPreservesExistingAssociations() {
        Long first = tag("first-" + UUID.randomUUID());
        Long second = tag("second-" + UUID.randomUUID());
        Todo todo = todo();
        todo.setTagIds(List.of(first, first));
        todos.create(todo);
        assertThat(todos.findById(todo.getId()).getTagIds()).containsExactly(first);
        todo.setTagIds(null);
        todos.update(todo);
        assertThat(todos.findById(todo.getId()).getTagIds()).containsExactly(first);
        todo.setTagIds(List.of(second));
        todos.update(todo);
        assertThat(todos.findById(todo.getId()).getTagIds()).containsExactly(second);
        todos.delete(todo.getId());
        todos.restore(todo.getId());
        assertThat(todos.findById(todo.getId()).getTagIds()).containsExactly(second);
    }

    @Test
    void invalidTagDoesNotPersistTodoAndInvalidFormReturnsBadRequest() throws Exception {
        Todo todo = todo();
        todo.setTagIds(List.of(Long.MAX_VALUE));
        assertThatThrownBy(() -> todos.create(todo)).isInstanceOf(ResponseStatusException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM todos WHERE title = ?", Long.class, todo.getTitle())).isZero();
        assertThatThrownBy(() -> tags.resolve(List.of(-1L))).isInstanceOf(ResponseStatusException.class);
        mvc.perform(post("/todos/confirm").param("title", "invalid").param("category", "資格")
                .param("priority", "2").param("tagIds", Long.toString(Long.MAX_VALUE)))
                .andExpect(status().isBadRequest());
    }
}
