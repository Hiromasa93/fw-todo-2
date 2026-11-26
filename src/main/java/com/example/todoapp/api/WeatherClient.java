package com.example.todoapp.api;

import java.net.http.HttpClient;
import java.net.URI;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WeatherClient {

    private final RestClient restClient;

    @Autowired
    public WeatherClient() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public WeatherClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public WeatherResult getWeather() {
        try {
            Forecast forecast = restClient.get()
                    .uri(URI.create("https://api.open-meteo.com/v1/forecast?latitude=35.6812&longitude=139.7671"
                            + "&daily=weather_code&timezone=Asia%2FTokyo&forecast_days=16"))
                    .retrieve().body(Forecast.class);
            if (forecast == null || forecast.daily() == null || forecast.daily().time() == null
                    || forecast.daily().weather_code() == null
                    || forecast.daily().time().size() != forecast.daily().weather_code().size()) {
                return new WeatherResult(Map.of(), true);
            }
            Map<String, String> weather = new LinkedHashMap<>();
            Daily daily = forecast.daily();
            for (int i = 0; i < daily.time().size(); i++) {
                String date = daily.time().get(i);
                if (date == null) {
                    return new WeatherResult(Map.of(), true);
                }
                LocalDate.parse(date);
                String description = describe(daily.weather_code().get(i));
                if (description != null) {
                    weather.put(date, description);
                }
            }
            return new WeatherResult(weather, false);
        } catch (RestClientException | IllegalArgumentException | DateTimeException exception) {
            return new WeatherResult(Map.of(), true);
        }
    }

    private String describe(Integer code) {
        if (code == null) return null;
        return switch (code) {
            case 0, 1 -> "晴れ";
            case 2 -> "晴れ時々くもり";
            case 3 -> "くもり";
            case 45, 48 -> "霧";
            case 51, 53, 55 -> "霧雨";
            case 56, 57 -> "凍る霧雨";
            case 61, 63, 65 -> "雨";
            case 66, 67 -> "凍る雨";
            case 71, 73, 75, 77 -> "雪";
            case 80, 81, 82 -> "にわか雨";
            case 85, 86 -> "にわか雪";
            case 95, 97 -> "雷雨";
            case 96, 99 -> "ひょうを伴う雷雨";
            default -> null;
        };
    }

    public record Forecast(Daily daily) {
    }

    public record Daily(List<String> time, List<Integer> weather_code) {
    }

    public record WeatherResult(Map<String, String> weather, boolean unavailable) {
    }
}
