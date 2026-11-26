package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuickTodoFeatureTests {
    @Autowired MockMvc mvc;
    @Autowired TodoService todos;

    @Test
    void requiresCsrfAndPersistsTitleWithDefaults() throws Exception {
        mvc.perform(post("/api/todo").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"test\"}"))
                .andExpect(status().isForbidden());
        var page = mvc.perform(get("/todos")).andExpect(status().isOk()).andReturn();
        CsrfToken token = (CsrfToken) page.getRequest().getAttribute("_csrf");
        MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
        assertThat(page.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("name=\"_csrf\"", "name=\"_csrf_header\"", "data-quick-input", "data-quick-row");
        mvc.perform(post("/api/todo").session(session).header(token.getHeaderName(), "wrong")
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"test\"}"))
                .andExpect(status().isForbidden());
        var created = mvc.perform(post("/api/todo").session(session).header(token.getHeaderName(), token.getToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"  クイック<script>  \"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("クイック<script>"))
                .andExpect(jsonPath("$.category").value("未分類")).andExpect(jsonPath("$.priority").value(2))
                .andExpect(jsonPath("$.completed").value(false)).andReturn();
        String location = created.getResponse().getHeader("Location");
        Long id = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
        Todo stored = todos.findById(id);
        assertThat(stored.getTitle()).isEqualTo("クイック<script>");
        assertThat(stored.getDueDate()).isNull();
        String edit = mvc.perform(get("/todos/" + id + "/edit")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(edit).contains("value=\"未分類\" selected=\"selected\"");
    }

    @Test
    void validatesTitleAndSupportsInitiallyEmptyList() throws Exception {
        var page = mvc.perform(get("/todos").param("keyword", "quick-empty-list-no-matches-823719"))
                .andExpect(status().isOk()).andReturn();
        CsrfToken token = (CsrfToken) page.getRequest().getAttribute("_csrf");
        MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
        assertThat(page.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("id=\"bulk-delete-form\"", "class=\"todo-table\"", "data-empty-todos", "hidden=\"hidden\"");
        for (String body : new String[]{"{}", "{\"title\":null}", "{\"title\":\"   \"}",
                "{\"title\":\"" + "x".repeat(256) + "\"}"}) {
            mvc.perform(post("/api/todo").session(session).header(token.getHeaderName(), token.getToken())
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("title"))
                    .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
        }
        String trash = mvc.perform(get("/todos").param("trash", "1")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(trash).doesNotContain("data-quick-input", "data-quick-row");
    }
}
