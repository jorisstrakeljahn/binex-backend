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

    public ParticipationService(ParticipationRepository participationRepository) {
        this.participationRepository = participationRepository;
    }

    public boolean processParticipation(String publicKey, String surveyId, String participantPoints) throws Exception {
        logger.info("Processing participation for publicKey: {}, surveyId: {}, participantPoints: {}",
                publicKey, surveyId, participantPoints);

        // Validate inputs
        if (publicKey == null || publicKey.isEmpty() ||
                surveyId == null || surveyId.isEmpty() ||
                participantPoints == null || participantPoints.isEmpty()) {
            logger.error("Input validation failed: One or more parameters are empty.");
            throw new IllegalArgumentException("Public Key, Survey ID, and Participant Points must not be empty.");
        }

        // Check if participantPoints is a valid number
        int points;
        try {
            points = Integer.parseInt(participantPoints);
            if (points <= 0) {
                logger.error("Participant Points must be a positive number. Received: {}", participantPoints);
                throw new IllegalArgumentException("Participant Points must be a positive number.");
            }
        } catch (NumberFormatException e) {
            logger.error("Participant Points is not a valid number. Received: {}", participantPoints);
            throw new IllegalArgumentException("Participant Points must be a valid number.");
        }

        // Combine salt, publicKey, and surveyId
        String combinedString = salt + publicKey + surveyId;
        logger.debug("Combined string for hashing: {}", combinedString);

        // Generate hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(combinedString.getBytes(StandardCharsets.UTF_8));

        // Convert hash to hex string
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String hashValue = sb.toString();
        logger.info("Generated hash value: {}", hashValue);

        // Check if hash already exists
        if (participationRepository.existsByHashValue(hashValue)) {
            logger.warn("Participation already registered for hash: {}", hashValue);
            return false;
        }

        // Mint NFT
        try {
            mintNFT(publicKey, points);
            logger.info("NFT successfully minted, proceeding to save participation...");

            // Save participation after successful minting
            Participation participation = new Participation(hashValue, LocalDateTime.now(), points);
            participationRepository.save(participation);
            logger.info("Participation successfully saved with hash: {}", hashValue);

        } catch (Exception e) {
            logger.error("Error during minting process, participation will not be saved", e);
            throw e;
        }

        return true;
    }

    private void mintNFT(String publicKey, int participantPoints) throws Exception {
        logger.info("Starting NFT minting for publicKey: {}, participantPoints: {}", publicKey, participantPoints);

        // Connect to the Q-Blockchain using the URL from application.properties
        Web3j web3j = Web3j.build(new HttpService(blockchainUrl));
        logger.info("Connected to Q-Blockchain at URL: {}", blockchainUrl);

        // Load credentials
        String privateKey = System.getenv("PRIVATE_KEY");
        if (privateKey == null || privateKey.isEmpty()) {
            logger.error("PRIVATE_KEY environment variable is not set.");
            throw new IllegalStateException("PRIVATE_KEY environment variable is not set.");
        }
        Credentials credentials = Credentials.create(privateKey);
        logger.info("Credentials successfully loaded.");

        // Create TransactionManager with Chain ID
        RawTransactionManager transactionManager = new RawTransactionManager(web3j, credentials, chainId);
        logger.info("TransactionManager created with Chain ID {}", chainId);

        // Set gas price and gas limit
        BigInteger networkGasPrice = web3j.ethGasPrice().send().getGasPrice();
        logger.info("Current network gas price: {} Wei", networkGasPrice);

        BigInteger gasPrice = networkGasPrice.multiply(BigInteger.valueOf(105)).divide(BigInteger.valueOf(100));
        BigInteger gasLimit = BigInteger.valueOf(400_000);
        logger.info("Gas price set to {} Wei, gas limit set to {}", gasPrice, gasLimit);

        // Create StaticGasProvider with defined values
        StaticGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);

        // Load Smart Contract
        SimpleNFT contract = SimpleNFT.load(
                contractAddress,
                web3j,
                transactionManager,
                gasProvider
        );
        logger.info("Smart Contract loaded with address: {}", contractAddress);

        // Generate TokenID (e.g., based on current timestamp)
        BigInteger tokenId = BigInteger.valueOf(System.currentTimeMillis());
        logger.info("TokenID generated: {}", tokenId);

        // Determine TokenURI based on participantPoints
        String tokenURI = getTokenURIForPoints(participantPoints);
        logger.info("TokenURI determined: {}", tokenURI);

        // Mint NFT
        try {
            TransactionReceipt receipt = contract.mintTo(publicKey, tokenId, tokenURI).send();
            logger.info("NFT successfully minted. Transaction Hash: {}", receipt.getTransactionHash());
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
                tokenURI = "https://deinserver.de/metadata/1.json";
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
