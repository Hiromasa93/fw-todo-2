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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DateFeatureTests {
    @Autowired MockMvc mvc;

    @Test
    void apiReturnsIsoDatesAndStructuredErrors() throws Exception {
        mvc.perform(get("/api/date/parse").param("input", "２０２８年２月２９日"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.date").value("2028-02-29"));
        for (String input : new String[]{"", "2月30日", "<script>"}) {
            mvc.perform(get("/api/date/parse").param("input", input))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }
        mvc.perform(get("/api/date/parse")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void formRendersPickerAndConfirmedDateUsesExistingBinding() throws Exception {
        String form = mvc.perform(get("/todos/new")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(form).contains("data-smart-date", "data-parse-url=\"/api/date/parse\"", "/js/smart-date.js", "name=\"dueDate\"");
        String confirm = mvc.perform(post("/todos/confirm").param("title", "自然言語の期限")
                .param("category", "資格").param("priority", "2").param("dueDate", "2028-02-29"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(confirm).contains("2028-02-29", "name=\"dueDate\"");
    }
}
