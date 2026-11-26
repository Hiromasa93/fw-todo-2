package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShortcutFeatureTests {
    @Autowired MockMvc mvc;
    @Autowired TodoService todos;

    @Test
    void helpApiAndSharedControlsAreAvailable() throws Exception {
        mvc.perform(get("/api/shortcuts")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].keys").value("Alt+N"))
                .andExpect(jsonPath("$[1].keys").value("Ctrl+S"))
                .andExpect(jsonPath("$[6].keys").value("Escape"));
        for (String path : new String[]{"/", "/todos", "/todos/new", "/calendar"}) {
            String html = mvc.perform(get(path)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(html).contains("/js/shortcuts.js", "id=\"shortcut-help\"", "data-api-url=\"/api/shortcuts\"");
        }
    }

    @Test
    void taskRowsLinkToEscapedDetailsAndMissingTasksReturn404() throws Exception {
        Todo todo = new Todo();
        todo.setTitle("shortcut-<script>alert(1)</script>");
        todo.setDetail("メモ<script>alert(2)</script>");
        todo.setCategory("資格");
        todo.setPriority(2);
        todo.setCompleted(false);
        todos.create(todo);
        String list = mvc.perform(get("/todos").param("keyword", "shortcut-"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(list).contains("data-shortcut-task", "tabindex=\"0\"", "data-shortcut-detail",
                "href=\"/todos/" + todo.getId() + "\"", "data-shortcut-search");
        String detail = mvc.perform(get("/todos/" + todo.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(detail).contains("メモ&lt;script&gt;", "shortcut-&lt;script&gt;").doesNotContain("<script>alert");
        String edit = mvc.perform(get("/todos/" + todo.getId() + "/edit")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(edit).contains("data-shortcut-save", "data-shortcut-submit");
        mvc.perform(get("/todos/9223372036854775807")).andExpect(status().isNotFound());
    }
}
