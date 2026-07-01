package ru.practicum.ewm.stats.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.ewm.stats.dto.EndpointHit;
import ru.practicum.ewm.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class StatsClientHttp implements StatsClient {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private final RestTemplate restTemplate;

    public StatsClientHttp(@Value("${stats-server.url:http://localhost:9090}") String serverUrl,
                           RestTemplateBuilder builder) {
        this.restTemplate = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .build();
    }

    @Override
    public void saveHit(EndpointHit hit) {
        restTemplate.postForEntity("/hit", hit, Object.class);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", formatDateTime(start));
        parameters.put("end", formatDateTime(end));

        StringBuilder path = new StringBuilder("/stats?start={start}&end={end}");

        if (uris != null && !uris.isEmpty()) {
            parameters.put("uris", String.join(",", uris));
            path.append("&uris={uris}");
        }

        parameters.put("unique", unique);
        path.append("&unique={unique}");

        ResponseEntity<Object> response = restTemplate.getForEntity(path.toString(), Object.class, parameters);

        if (response.getBody() == null) {
            return List.of();
        }

        return convertToViewStatsList(response.getBody());
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("dateTime cannot be null");
        }
        return dateTime.format(FORMATTER);
    }

    @SuppressWarnings("unchecked")
    private List<ViewStats> convertToViewStatsList(Object body) {
        if (!(body instanceof List)) {
            return List.of();
        }

        List<?> list = (List<?>) body;
        return list.stream()
                .map(item -> {
                    if (item instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) item;
                        ViewStats stats = new ViewStats();
                        stats.setApp((String) map.get("app"));
                        stats.setUri((String) map.get("uri"));
                        Object hits = map.get("hits");
                        if (hits instanceof Number) {
                            stats.setHits(((Number) hits).longValue());
                        }
                        return stats;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
