package com.example.todoapp.api;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HolidayClient {

    private static final String HOLIDAYS_API_URL = "https://holidays-jp.github.io/api/v1/date.json";
    private final RestClient restClient;

    public HolidayClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public HolidayResult getHolidays() {
        try {
            Map<String, String> holidays = restClient.get()
                    .uri(HOLIDAYS_API_URL)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<LinkedHashMap<String, String>>() {
                    });
            return new HolidayResult(holidays == null ? new LinkedHashMap<>() : holidays, false);
        } catch (RestClientException exception) {
            return new HolidayResult(new LinkedHashMap<>(), true);
        }
    }

    public record HolidayResult(Map<String, String> holidays, boolean unavailable) {
    }
}
