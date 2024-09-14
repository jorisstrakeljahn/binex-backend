package de.hsbi.binex.binex_backend.service;

import de.hsbi.binex.binex_backend.contracts.SimpleNFT;
import de.hsbi.binex.binex_backend.entity.Participation;
import de.hsbi.binex.binex_backend.repository.ParticipationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class ParticipationService {

    private static final Logger logger = LoggerFactory.getLogger(ParticipationService.class);

    private final ParticipationRepository participationRepository;

    // Salt-Wert aus application.properties einlesen
    @Value("${app.hash.salt}")
    private String salt;

    // Contract-Adresse aus application.properties einlesen
    @Value("${app.contract.address}")
    private String contractAddress;

    public ParticipationService(ParticipationRepository participationRepository) {
        this.participationRepository = participationRepository;
    }

    public boolean processParticipation(String publicKey, String surveyId, String participantPoints) throws Exception {
        logger.info("Verarbeite Teilnahme für publicKey: {}, surveyId: {}, participantPoints: {}",
                publicKey, surveyId, participantPoints);

        // Eingaben validieren
        if (publicKey == null || publicKey.isEmpty() ||
                surveyId == null || surveyId.isEmpty() ||
                participantPoints == null || participantPoints.isEmpty()) {
            logger.error("Eingabevalidierung fehlgeschlagen: Ein oder mehrere Parameter sind leer.");
            throw new IllegalArgumentException("Public Key, Survey ID und Participant Points dürfen nicht leer sein.");
        }

        // Überprüfe, ob participantPoints eine gültige Zahl ist
        int points;
        try {
            points = Integer.parseInt(participantPoints);
            if (points <= 0) {
                logger.error("Participant Points müssen eine positive Zahl sein. Erhalten: {}", participantPoints);
                throw new IllegalArgumentException("Participant Points müssen eine positive Zahl sein.");
            }
        } catch (NumberFormatException e) {
            logger.error("Participant Points sind keine gültige Zahl. Erhalten: {}", participantPoints);
            throw new IllegalArgumentException("Participant Points müssen eine gültige Zahl sein.");
        }

        // Kombiniere Salt, Public Key und Survey ID
        String combinedString = salt + publicKey + surveyId;
        logger.debug("Kombinierter String für Hashing: {}", combinedString);

        // Hash generieren
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(combinedString.getBytes(StandardCharsets.UTF_8));

        // Hash in Hex-String umwandeln
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String hashValue = sb.toString();
        logger.info("Generierter Hashwert: {}", hashValue);

        // Überprüfen, ob Hash bereits existiert
        if (participationRepository.existsByHashValue(hashValue)) {
            logger.warn("Teilnahme bereits registriert für Hash: {}", hashValue);
            return false;
        }

        // Neue Teilnahme speichern
        Participation participation = new Participation(hashValue, LocalDateTime.now(), points);
        participationRepository.save(participation);
        logger.info("Teilnahme erfolgreich gespeichert mit Hash: {}", hashValue);

        // NFT minten
        try {
            mintNFT(publicKey, points);
        } catch (Exception e) {
            logger.error("Fehler beim Minting-Prozess", e);
            throw e;
        }

        return true;
    }

    private void mintNFT(String publicKey, int participantPoints) throws Exception {
        logger.info("Starte NFT-Minting für publicKey: {}, participantPoints: {}", publicKey, participantPoints);

        // Verbindung zur Q-Blockchain herstellen
        Web3j web3j = Web3j.build(new HttpService("https://rpc.qtestnet.org"));
        logger.info("Verbindung zur Q-Blockchain hergestellt.");

        // Credentials laden
        String privateKey = System.getenv("PRIVATE_KEY");
        if (privateKey == null || privateKey.isEmpty()) {
            logger.error("PRIVATE_KEY Umgebungsvariable ist nicht gesetzt.");
            throw new IllegalStateException("PRIVATE_KEY Umgebungsvariable ist nicht gesetzt.");
        }
        Credentials credentials = Credentials.create(privateKey);
        logger.info("Credentials erfolgreich geladen.");

        // Chain ID für das Q-Testnet
        int chainId = 35443;

        // TransactionManager mit Chain ID erstellen
        RawTransactionManager transactionManager = new RawTransactionManager(web3j, credentials, chainId);
        logger.info("TransactionManager mit Chain ID {} erstellt.", chainId);

        // Gaspreis und Gaslimit festlegen
        BigInteger gasPrice = BigInteger.valueOf(20_000_000_000L); // 20 Gwei
        BigInteger gasLimit = BigInteger.valueOf(300_000); // Beispielwert, anpassen falls nötig
        logger.info("Gaspreis festgelegt auf {} Wei, Gaslimit auf {}", gasPrice, gasLimit);

        // Erstelle einen StaticGasProvider mit den festgelegten Werten
        StaticGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);

        // Smart Contract laden
        SimpleNFT contract = SimpleNFT.load(
                contractAddress,
                web3j,
                transactionManager,
                gasProvider
        );
        logger.info("Smart Contract geladen mit Adresse: {}", contractAddress);

        // TokenID generieren (z.B. basierend auf aktuellem Timestamp)
        BigInteger tokenId = BigInteger.valueOf(System.currentTimeMillis());
        logger.info("TokenID generiert: {}", tokenId);

        // TokenURI basierend auf participantPoints bestimmen
        String tokenURI = getTokenURIForPoints(participantPoints);
        logger.info("TokenURI bestimmt: {}", tokenURI);

        // NFT minten
        try {
            TransactionReceipt receipt = contract.mintTo(publicKey, tokenId, tokenURI).send();
            logger.info("NFT erfolgreich gemintet. Transaction Hash: {}", receipt.getTransactionHash());
        } catch (Exception e) {
            logger.error("Fehler beim Senden der Transaktion", e);
            throw new Exception("Fehler beim Minting des NFTs: " + e.getMessage(), e);
        }
    }

    private String getTokenURIForPoints(int participantPoints) {
        // Basierend auf den Punkten den entsprechenden TokenURI zurückgeben
        String tokenURI;
        switch (participantPoints) {
            case 1:
                tokenURI = "https://deinserver.de/metadata/1.json";
                break;
            case 2:
                tokenURI = "https://deinserver.de/metadata/2.json";
                break;
            case 3:
                tokenURI = "https://deinserver.de/metadata/3.json";
                break;
            default:
                // Für andere Werte einen Standard-TokenURI zurückgeben
                tokenURI = "https://www.daab.de/fileadmin/_processed_/6/1/csm_10-punkte_6a136a1311.png";
                break;
        }
        logger.debug("TokenURI für participantPoints {}: {}", participantPoints, tokenURI);
        return tokenURI;
    }
}
