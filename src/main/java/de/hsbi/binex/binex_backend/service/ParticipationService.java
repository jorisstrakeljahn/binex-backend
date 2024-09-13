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

    public boolean processParticipation(String publicKey, String surveyId) throws Exception {
        // Eingaben validieren
        if (publicKey == null || publicKey.isEmpty() || surveyId == null || surveyId.isEmpty()) {
            throw new IllegalArgumentException("Public Key und Survey ID dürfen nicht leer sein.");
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
        Participation participation = new Participation(hashValue, LocalDateTime.now());
        participationRepository.save(participation);

        // NFT minten (Funktion implementieren)
        mintNFT(publicKey);

        return true;
    }

    private void mintNFT(String publicKey) {
        // TODO: Implementiere die Logik zum Minten des NFTs über die Q-Blockchain-API

        System.err.println("NFT minten für Public Key: " + publicKey);

        // Implementiere die Logik zum Minten des NFTs
        // Dies kann die Verwendung der Q-Blockchain-SDK oder API-Aufrufe beinhalten
        // Beispiel (Pseudocode):

        // 1. Erstelle eine Verbindung zur Q-Blockchain
        // 2. Bereite die NFT-Daten vor
        // 3. Führe den Minting-Prozess durch
        // 4. Behandle Rückmeldungen und Fehler

    }
}

