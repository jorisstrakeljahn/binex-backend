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

    @Column(name = "participant_points", nullable = false)
    private int participantPoints;

    public Participation() {
    }

    public Participation(String hashValue, LocalDateTime timestamp, int participantPoints) {
        this.hashValue = hashValue;
        this.timestamp = timestamp;
        this.participantPoints = participantPoints;
    }

    // getter and setter
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

    public int getParticipantPoints() {
        return participantPoints;
    }

    public void setParticipantPoints(int participantPoints) {
        this.participantPoints = participantPoints;
    }
}
