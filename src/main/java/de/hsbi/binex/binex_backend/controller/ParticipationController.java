package de.hsbi.binex.binex_backend.controller;

import de.hsbi.binex.binex_backend.service.ParticipationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ParticipationController {

    private final ParticipationService participationService;

    public ParticipationController(ParticipationService participationService) {
        this.participationService = participationService;
    }

    @PostMapping("/mint-nft")
    public ResponseEntity<String> mintNFT(@RequestParam String publicKey,
                                          @RequestParam String surveyId,
                                          @RequestParam String participantPoints) {
        try {
            boolean isNewParticipation = participationService.processParticipation(publicKey, surveyId, participantPoints);
            if (isNewParticipation) {
                return ResponseEntity.ok("NFT wurde erfolgreich gemintet.");
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Teilnahme wurde bereits registriert.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Ungültige Eingaben: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Es ist ein Fehler aufgetreten.");
        }
    }
}

