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

@Service
public class ParticipationService {

    private static final Logger logger = LoggerFactory.getLogger(ParticipationService.class);

    @Value("${app.hash.salt}")
    private String salt;

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

        // Generiere den Hash-Wert
        String hashValue = generateHash(publicKey, surveyId);
        logger.info("Generated hash value: {}", hashValue);

        // Überprüfe, ob der Benutzer bereits teilgenommen hat
        boolean hasParticipated = checkIfParticipationExistsOnBlockchain(publicKey, hashValue);
        if (hasParticipated) {
            logger.warn("Participation already registered on blockchain for hash: {}", hashValue);
            return false;
        }

        // Bestimme den TokenURI basierend auf surveyId
        String tokenURI = getTokenURIForSurveyId(surveyId);
        logger.info("TokenURI determined: {}", tokenURI);

        // Mint NFT mit dem Hash-Wert als Token ID
        mintNFT(publicKey, hashValue, tokenURI);

        return true;
    }

    private void validateInputs(String publicKey, String surveyId, String participantPoints) {
        if (publicKey == null || publicKey.isEmpty() ||
                surveyId == null || surveyId.isEmpty() ||
                participantPoints == null || participantPoints.isEmpty()) {
            logger.error("Input validation failed: One or more parameters are empty.");
            throw new IllegalArgumentException("Public Key, Survey ID, and Participant Points must not be empty.");
        }

        // Überprüfe, ob surveyId gültig ist
        if (!surveyId.equals("cashlink-nft-event") &&
                !surveyId.equals("hsbi-logo-nft") &&
                !surveyId.equals("test-nft-blockchain") &&
                !surveyId.equals("vpp-nft-1") &&
                !surveyId.equals("vpp-nft-2") &&
                !surveyId.equals("vpp-nft-3")) {
            logger.error("Invalid Survey ID: {}", surveyId);
            throw new IllegalArgumentException("Invalid Survey ID.");
        }

        // participantPoints wird ignoriert, kann aber auf Nicht-Leerheit geprüft werden
    }

    private boolean checkIfParticipationExistsOnBlockchain(String publicKey, String hashValue) throws Exception {
        logger.info("Checking if participation exists on blockchain for publicKey: {}", publicKey);

        Web3j web3j = Web3j.build(new HttpService(blockchainUrl));

        BinexNFT contract = BinexNFT.load(
                contractAddress,
                web3j,
                Credentials.create("0x0"),
                new StaticGasProvider(BigInteger.ZERO, BigInteger.ZERO)
        );

        BigInteger tokenId = new BigInteger(hashValue, 16);

        boolean tokenExists = contract.exists(tokenId).send();
        if (tokenExists) {
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

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void mintNFT(String publicKey, String hashValue, String tokenURI) throws Exception {
        logger.info("Starting NFT minting for publicKey: {}", publicKey);

        Web3j web3j = Web3j.build(new HttpService(blockchainUrl));
        logger.info("Connected to Q-Blockchain at URL: {}", blockchainUrl);

        String privateKey = System.getenv("PRIVATE_KEY");
        if (privateKey == null || privateKey.isEmpty()) {
            logger.error("PRIVATE_KEY environment variable is not set.");
            throw new IllegalStateException("PRIVATE_KEY environment variable is not set.");
        }
        Credentials credentials = Credentials.create(privateKey);
        logger.info("Credentials successfully loaded.");

        RawTransactionManager transactionManager = new RawTransactionManager(web3j, credentials, chainId);
        logger.info("TransactionManager created with Chain ID {}", chainId);

        BigInteger networkGasPrice = web3j.ethGasPrice().send().getGasPrice();
        logger.info("Current network gas price: {} Wei", networkGasPrice);

        BigInteger gasPrice = networkGasPrice.multiply(BigInteger.valueOf(105)).divide(BigInteger.valueOf(100));
        BigInteger gasLimit = BigInteger.valueOf(400_000);
        logger.info("Gas price set to {} Wei, gas limit set to {}", gasPrice, gasLimit);

        StaticGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);

        BinexNFT contract = BinexNFT.load(
                contractAddress,
                web3j,
                transactionManager,
                gasProvider
        );
        logger.info("Smart Contract loaded with address: {}", contractAddress);

        BigInteger tokenId = new BigInteger(hashValue, 16);
        logger.info("TokenID generated from hash: {}", tokenId);

        try {
            TransactionReceipt receipt = contract.mintTo(publicKey, tokenId, tokenURI).send();
            logger.info("NFT successfully minted with tokenId: {}. Transaction Hash: {}", tokenId, receipt.getTransactionHash());
        } catch (Exception e) {
            logger.error("Error while sending transaction", e);
            throw new Exception("Error during NFT minting: " + e.getMessage(), e);
        }
    }

    private String getTokenURIForSurveyId(String surveyId) {
        String tokenURI = switch (surveyId) {
            case "cashlink-nft-event" ->
                    "https://binex.hsbi.de/assets/1-binex-nft-event-blockchain-trifft-finance.ce070184.jpg";
            case "hsbi-logo-nft" -> "https://www.designtagebuch.de/wp-content/uploads/mediathek/2023/04/hsbi-logo.jpg";
            case "test-nft-blockchain" ->
                    "https://www.hsbi.de/multimedia/Hochschulverwaltung/HSK/Bilder+Berichterstattung/Fachbereiche/FB+5/Veranstaltungen/2022_09_20+Kryprow%C3%A4hrung+Blockchain+Peer_to_Peer/Slider/220920_FH_Kryptowaehrung_slider_10-height-635-width-1270-p-163374.jpg";
            case "vpp-nft-1" -> "https://binex.hsbi.de/assets/VPP%20-%20NFT%201.d6cdab25.jpg";
            case "vpp-nft-2" -> "https://binex.hsbi.de/assets/VPP%20-%20NFT%202.bee565e9.jpg";
            case "vpp-nft-3" -> "https://binex.hsbi.de/assets/VPP%20-%20NFT%203.688f1fbb.jpg";
            default -> {
                logger.error("Invalid Survey ID: {}", surveyId);
                throw new IllegalArgumentException("Invalid Survey ID.");
            }
        };
        logger.debug("TokenURI for surveyId {}: {}", surveyId, tokenURI);
        return tokenURI;
    }
}
