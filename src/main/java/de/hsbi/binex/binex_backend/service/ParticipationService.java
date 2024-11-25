package de.hsbi.binex.binex_backend.service;

import de.hsbi.binex.binex_backend.contracts.BinexNFT;
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

    // Read salt value from application.properties
    @Value("${app.hash.salt}")
    private String salt;

    // Read contract address from application.properties
    @Value("${app.contract.address}")
    private String contractAddress;

    @Value("${app.qblockchain.url}")
    private String blockchainUrl;

    @Value("${app.qblockchain.chainId}")
    private int chainId;

    public ParticipationService() {
    }
    public boolean processParticipation(String publicKey, String surveyId, String participantPoints) throws Exception {
        logger.info("Processing participation for publicKey: {}, surveyId: {}, participantPoints: {}",
                publicKey, surveyId, participantPoints);

        // Eingabevalidierung
        validateInputs(publicKey, surveyId, participantPoints);

        int points = Integer.parseInt(participantPoints);

        // Generiere den Hash-Wert
        String hashValue = generateHash(publicKey, surveyId);
        logger.info("Generated hash value: {}", hashValue);

        // Überprüfe, ob der Benutzer bereits teilgenommen hat
        boolean hasParticipated = checkIfParticipationExistsOnBlockchain(publicKey, hashValue);
        if (hasParticipated) {
            logger.warn("Participation already registered on blockchain for hash: {}", hashValue);
            return false;
        }

        // Mint NFT mit dem Hash-Wert als Token ID
        mintNFT(publicKey, points, hashValue);

        return true;
    }

    private void validateInputs(String publicKey, String surveyId, String participantPoints) {
        if (publicKey == null || publicKey.isEmpty() ||
                surveyId == null || surveyId.isEmpty() ||
                participantPoints == null || participantPoints.isEmpty()) {
            logger.error("Input validation failed: One or more parameters are empty.");
            throw new IllegalArgumentException("Public Key, Survey ID, and Participant Points must not be empty.");
        }

        try {
            int points = Integer.parseInt(participantPoints);
            if (points <= 0) {
                logger.error("Participant Points must be a positive number. Received: {}", participantPoints);
                throw new IllegalArgumentException("Participant Points must be a positive number.");
            }
        } catch (NumberFormatException e) {
            logger.error("Participant Points is not a valid number. Received: {}", participantPoints);
            throw new IllegalArgumentException("Participant Points must be a valid number.");
        }
    }

    private boolean checkIfParticipationExistsOnBlockchain(String publicKey, String hashValue) throws Exception {
        logger.info("Checking if participation exists on blockchain for publicKey: {}", publicKey);

        // Verbinde mit der Blockchain
        Web3j web3j = Web3j.build(new HttpService(blockchainUrl));

        // Lade den Smart Contract (Read-only Operation, Credentials sind nicht unbedingt erforderlich)
        BinexNFT contract = BinexNFT.load(
                contractAddress,
                web3j,
                Credentials.create("0x0"), // Dummy Credentials für Leseoperationen
                new StaticGasProvider(BigInteger.ZERO, BigInteger.ZERO)
        );

        // Konvertiere den Hash-Wert in BigInteger für die Token ID
        BigInteger tokenId = new BigInteger(hashValue, 16);

        // Überprüfe, ob der Token existiert
        boolean tokenExists = contract.exists(tokenId).send();
        if (tokenExists) {
            // Hole den Besitzer des Tokens
            String ownerAddress = contract.ownerOf(tokenId).send();
            if (ownerAddress.equalsIgnoreCase(publicKey)) {
                logger.info("User already owns the token with tokenId: {}", tokenId);
                return true;
            }
        }
        return false;
    }

    private String generateHash(String publicKey, String surveyId) throws Exception {
        String combinedString = salt + publicKey + surveyId;
        logger.debug("Combined string for hashing: {}", combinedString);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(combinedString.getBytes(StandardCharsets.UTF_8));

        // Konvertiere Hash in Hex-String
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void mintNFT(String publicKey, int participantPoints, String hashValue) throws Exception {
        logger.info("Starting NFT minting for publicKey: {}, participantPoints: {}", publicKey, participantPoints);

        // Verbinde mit der Blockchain
        Web3j web3j = Web3j.build(new HttpService(blockchainUrl));
        logger.info("Connected to Q-Blockchain at URL: {}", blockchainUrl);

        // Lade die Credentials
        String privateKey = System.getenv("PRIVATE_KEY");
        if (privateKey == null || privateKey.isEmpty()) {
            logger.error("PRIVATE_KEY environment variable is not set.");
            throw new IllegalStateException("PRIVATE_KEY environment variable is not set.");
        }
        Credentials credentials = Credentials.create(privateKey);
        logger.info("Credentials successfully loaded.");

        // Erstelle den TransactionManager mit Chain ID
        RawTransactionManager transactionManager = new RawTransactionManager(web3j, credentials, chainId);
        logger.info("TransactionManager created with Chain ID {}", chainId);

        // Setze Gaspreis und Gaslimit
        BigInteger networkGasPrice = web3j.ethGasPrice().send().getGasPrice();
        logger.info("Current network gas price: {} Wei", networkGasPrice);

        BigInteger gasPrice = networkGasPrice.multiply(BigInteger.valueOf(105)).divide(BigInteger.valueOf(100));
        BigInteger gasLimit = BigInteger.valueOf(400_000);
        logger.info("Gas price set to {} Wei, gas limit set to {}", gasPrice, gasLimit);

        // Erstelle den GasProvider
        StaticGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);

        // Lade den Smart Contract
        BinexNFT contract = BinexNFT.load(
                contractAddress,
                web3j,
                transactionManager,
                gasProvider
        );
        logger.info("Smart Contract loaded with address: {}", contractAddress);

        // Konvertiere den Hash-Wert in BigInteger für die Token ID
        BigInteger tokenId = new BigInteger(hashValue, 16);
        logger.info("TokenID generated from hash: {}", tokenId);

        // Bestimme den TokenURI basierend auf participantPoints
        String tokenURI = getTokenURIForPoints(participantPoints);
        logger.info("TokenURI determined: {}", tokenURI);

        // Mint NFT mit spezifischer Token ID
        try {
            TransactionReceipt receipt = contract.mintTo(publicKey, tokenId, tokenURI).send();
            logger.info("NFT successfully minted with tokenId: {}. Transaction Hash: {}", tokenId, receipt.getTransactionHash());
        } catch (Exception e) {
            logger.error("Error while sending transaction", e);
            throw new Exception("Error during NFT minting: " + e.getMessage(), e);
        }
    }

    private String getTokenURIForPoints(int participantPoints) {
        // Return the appropriate TokenURI based on points
        String tokenURI;
        switch (participantPoints) {
            case 1:
                tokenURI = "https://binex.hsbi.de/assets/blockchain-basisc-start-image.8aa28373.jpg";
                break;
            case 2:
                tokenURI = "https://deinserver.de/metadata/2.json";
                break;
            case 3:
                tokenURI = "https://deinserver.de/metadata/3.json";
                break;
            default:
                // Return a default TokenURI for other values
                tokenURI = "https://www.daab.de/fileadmin/_processed_/6/1/csm_10-punkte_6a136a1311.png";
                break;
        }
        logger.debug("TokenURI for participantPoints {}: {}", participantPoints, tokenURI);
        return tokenURI;
    }
}
