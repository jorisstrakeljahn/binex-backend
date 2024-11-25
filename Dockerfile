# Verwende ein leichtgewichtiges Java Image
FROM eclipse-temurin:17-jre-alpine

# Erstelle ein Verzeichnis für die App
WORKDIR /app

# Kopiere das gebaute JAR-File in das Image und benenne es um
COPY target/*.jar app.jar

# Exponiere den Port 8080
EXPOSE 8080

# Setze den Startbefehl
CMD ["java", "-jar", "app.jar"]

