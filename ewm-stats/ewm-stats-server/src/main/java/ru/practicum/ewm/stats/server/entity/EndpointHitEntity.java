package ru.practicum.ewm.stats.server.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Setter;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "endpoint_hits")
@Getter
@Setter
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