package de.hsbi.binex.binex_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "participations")
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hash_value", nullable = false, unique = true)
    private String hashValue;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public Participation() {
    }

    public Participation(String hashValue, LocalDateTime timestamp) {
        this.hashValue = hashValue;
        this.timestamp = timestamp;
    }

    // Getter und Setter
    public Long getId() {
        return id;
    }

    public String getHashValue() {
        return hashValue;
    }

    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
