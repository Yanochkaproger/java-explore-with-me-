package ru.practicum.ewm.stats.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "endpoint_hits")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EndpointHitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "app")
    private String app;

    @Column(nullable = false, name = "uri", length = 512)
    private String uri;

    @Column(nullable = false, name = "ip", length = 45)
    private String ip;

    @Column(nullable = false, name = "created_on")
    private LocalDateTime timestamp;
}