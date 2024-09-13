package de.hsbi.binex.binex_backend.service;

import de.hsbi.binex.binex_backend.entity.Participation;
import de.hsbi.binex.binex_backend.repository.ParticipationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class ParticipationService {

    private final ParticipationRepository participationRepository;

    // Salt-Wert aus application.properties einlesen
    @Value("${app.hash.salt}")
    private String salt;

    public ParticipationService(ParticipationRepository participationRepository) {
        this.participationRepository = participationRepository;
    }

    public boolean processParticipation(String publicKey, String surveyId, String participantPoints) throws Exception {
        // Eingaben validieren
        if (publicKey == null || publicKey.isEmpty() ||
                surveyId == null || surveyId.isEmpty() ||
                participantPoints == null || participantPoints.isEmpty()) {
            throw new IllegalArgumentException("Public Key, Survey ID und Participant Points dürfen nicht leer sein.");
        }

        // Optional: Überprüfe, ob participantPoints eine gültige Zahl ist
        int points;
        try {
            points = Integer.parseInt(participantPoints);
            if (points <= 0) {
                throw new IllegalArgumentException("Participant Points müssen eine positive Zahl sein.");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Participant Points müssen eine gültige Zahl sein.");
        }

        // Kombiniere Salt, Public Key und Survey ID
        String combinedString = salt + publicKey + surveyId;

        // Hash generieren
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(combinedString.getBytes(StandardCharsets.UTF_8));

        // Hash in Hex-String umwandeln
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String hashValue = sb.toString();

        // Überprüfen, ob Hash bereits existiert
        if (participationRepository.existsByHashValue(hashValue)) {
            return false; // Teilnahme bereits registriert
        }

        // Neue Teilnahme speichern
        Participation participation = new Participation(hashValue, LocalDateTime.now(), points);
        participationRepository.save(participation);

        // NFT minten (Funktion implementieren)
        mintNFT(publicKey, points);

        return true;
    }

    private void mintNFT(String publicKey, int participantPoints) {
        String nftType;
        if (participantPoints == 1) {
            nftType = "NFT_Typ_1_Punkt";
        } else if (participantPoints == 2) {
            nftType = "NFT_Typ_2_Punkte";
        } else if (participantPoints == 3) {
            nftType = "NFT_Typ_3_Punkte";
        } else {
            // Default: 3 Punkte
            nftType = "NFT_Typ_3_Punkte";
        }

        System.err.println("NFT minten für Public Key: " + publicKey + ", Punkte: " + participantPoints + ", NFT-Typ: " + nftType);

        // Beispiel (Pseudocode):

        // 1. Erstelle eine Verbindung zur Q-Blockchain
        // 2. Wähle das richtige NFT basierend auf participantPoints
        // 3. Führe den Minting-Prozess durch
        // 4. Behandle Rückmeldungen und Fehler
    }
}


