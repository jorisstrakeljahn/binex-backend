package de.hsbi.binex.binex_backend.controller;

import de.hsbi.binex.binex_backend.service.ParticipationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class ParticipationController {

    private static final Logger logger = LoggerFactory.getLogger(ParticipationController.class);

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
                return ResponseEntity.ok("NFT was successfully minted.");
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Participation has already been registered.");
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid input: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error during the minting process", e);
            // Return detailed error message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred: " + e.getMessage());
        }
    }
}
