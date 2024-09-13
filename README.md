# Binex Backend

Dies ist das Backend-Projekt für das Binex-System, das dazu dient, Versuchspersonenpunkte über die Q-Blockchain als NFTs zu vergeben. Das System ermöglicht es Studierenden, durch die Teilnahme an Umfragen Versuchspersonenpunkte zu sammeln, die in Form von NFTs ausgestellt werden.

## **Inhaltsverzeichnis**

- [Projektübersicht](#projektübersicht)
- [Aktueller Funktionsumfang](#aktueller-funktionsumfang)
- [Zukünftige Funktionen](#zukünftige-funktionen)
- [Installation und Einrichtung](#installation-und-einrichtung)
- [Verwendung des SoSciSurvey-Codes](#verwendung-des-soscisurvey-codes)
- [API-Endpunkte](#api-endpunkte)
- [Beiträge](#beiträge)
- [Lizenz](#lizenz)

---

## **Projektübersicht**

Das Binex Backend ist eine Spring Boot Anwendung, die folgende Hauptfunktionen bietet:

- **Verarbeitung von Teilnehmerdaten**: Empfang von Public Keys und Umfrage-IDs von Umfrageteilnehmern.
- **Hashing und Speicherung**: Hashing von Teilnehmerdaten mit einem Salt und Speicherung in einer PostgreSQL-Datenbank, um Mehrfachbelohnungen zu verhindern.
- **NFT-Minting** (zukünftig): Automatisches Minten von NFTs auf der Q-Blockchain, basierend auf der Anzahl der erhaltenen Versuchspersonenpunkte.

## **Aktueller Funktionsumfang**

- **API-Endpunkt** `/api/mint-nft`:

    - **Methode**: `POST`
    - **Beschreibung**: Empfängt den Public Key eines Teilnehmers, die Umfrage-ID und die Anzahl der Versuchspersonenpunkte.
    - **Funktion**:
        - Validiert die Eingaben.
        - Generiert einen Hash aus Public Key, Umfrage-ID und einem geheimen Salt.
        - Speichert den Hash in der Datenbank, um doppelte Einreichungen zu verhindern.
        - Ruft die `mintNFT` Methode auf (derzeit Platzhalter), um das NFT zu minten.

- **Datenbankintegration**:

    - Verwendung von **PostgreSQL** zur Speicherung der Teilnahmeinformationen.
    - Entity-Klasse `Participation` repräsentiert die gespeicherten Daten.

- **Hashing und Sicherheit**:

    - Hashing der sensiblen Daten mit SHA-256 und einem geheimen Salt, um Datenschutz zu gewährleisten.

- **Beispielcode für SoSciSurvey**:

    - Bereitstellung eines HTML/JavaScript-Codes, den Umfrageersteller in ihre Umfragen einbinden können, um den Prozess zu automatisieren.

## **Zukünftige Funktionen**

- **Implementierung der `mintNFT` Methode**:

    - Integration mit der Q-Blockchain-API, um NFTs tatsächlich zu minten.
    - Auswahl des richtigen NFT basierend auf der Anzahl der Versuchspersonenpunkte.

- **Sicherheit und Verschlüsselung**:

    - Umstellung auf HTTPS für sichere Datenübertragung.
    - Erweiterte Sicherheitsmaßnahmen, um das System vor Angriffen zu schützen.

- **Erweiterte API-Funktionen**:

    - Hinzufügen von Endpunkten für Administratoren zur Verwaltung von Umfragen und Teilnehmern.
    - Bereitstellung von Statistiken und Berichten.

- **Dokumentation und Benutzerfreundlichkeit**:

    - Ausführliche Anleitungen für Umfrageersteller und Teilnehmer.
    - Verbesserte Fehlermeldungen und Feedbackmechanismen.

## **Installation und Einrichtung**

### **Voraussetzungen**

- **Java Development Kit (JDK) 17 oder höher**
- **Maven** für das Build-Management
- **PostgreSQL** als Datenbank
- **Git** für die Versionskontrolle

### **Schritte**

1. **Repository klonen**

   ```bash
   git clone https://github.com/jorisstrakeljahn/binex-backend.git
   cd binex-backend
