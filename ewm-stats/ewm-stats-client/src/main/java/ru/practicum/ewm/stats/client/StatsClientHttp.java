package ru.practicum.ewm.stats.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.stats.dto.EndpointHit;
import ru.practicum.ewm.stats.dto.ViewStats;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class StatsClientHttp implements StatsClient {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private final RestTemplate restTemplate;
    private final String serverUrl;

    public StatsClientHttp(@Value("${stats-server.url:http://localhost:9090}") String serverUrl,
                           RestTemplateBuilder builder) {
        this.serverUrl = serverUrl;
        this.restTemplate = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .build();
    }

    @Override
    public void saveHit(EndpointHit hit) {
        restTemplate.postForEntity("/hit", hit, Void.class);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl + "/stats")
                .queryParam("start", URLEncoder.encode(start.format(FORMATTER), StandardCharsets.UTF_8))
                .queryParam("end", URLEncoder.encode(end.format(FORMATTER), StandardCharsets.UTF_8))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
            }
        }

        String url = builder.build().toUriString();

        ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(url, ViewStats[].class);

        return Arrays.asList(response.getBody());
    }
}

