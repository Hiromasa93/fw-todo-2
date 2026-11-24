package com.example.todoapp.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HolidayClient {

    private static final String HOLIDAYS_API_URL = "https://holidays-jp.github.io/api/v1/date.json";
    private final RestClient restClient = RestClient.create();

    public Map<String, String> getHolidays() {
        return restClient.get()
                .uri(HOLIDAYS_API_URL)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<LinkedHashMap<String, String>>() {});
    }
}
