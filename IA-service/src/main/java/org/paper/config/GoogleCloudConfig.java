package org.paper.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.aiplatform.v1.EndpointServiceClient;
import com.google.cloud.aiplatform.v1.EndpointServiceSettings;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Configuración para Google Cloud Vertex AI (Imagen 3)
 *
 * IMPORTANTE: Requiere service account JSON configurado
 */
@Slf4j
@Configuration
public class GoogleCloudConfig {

    @Value("${google.cloud.project-id}")
    private String projectId;

    @Value("${google.cloud.location:us-central1}")
    private String location;

    @Value("${google.cloud.credentials.path:#{null}}")
    private String credentialsPath;

    /**
     * Configura las credenciales de Google Cloud
     */
    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        log.info("Inicializando Google Cloud credentials...");

        GoogleCredentials credentials;

        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            // Desde archivo JSON
            log.info("Usando credenciales desde archivo: {}", credentialsPath);
            try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
                credentials = GoogleCredentials.fromStream(serviceAccountStream)
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
            }
        } else {
            // Desde Application Default Credentials (en producción con GKE)
            log.info("Usando Application Default Credentials");
            credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
        }

        log.info("✅ Google Cloud credentials inicializadas correctamente");
        return credentials;
    }

    /**
     * Cliente para predicciones (generación de imágenes)
     */
    @Bean
    public PredictionServiceClient predictionServiceClient(GoogleCredentials credentials) throws IOException {
        log.info("Inicializando PredictionServiceClient para location: {}", location);

        String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);

        PredictionServiceSettings settings = PredictionServiceSettings.newBuilder()
                .setEndpoint(endpoint)
                .setCredentialsProvider(() -> credentials)
                .build();

        PredictionServiceClient client = PredictionServiceClient.create(settings);

        log.info("✅ PredictionServiceClient inicializado correctamente");
        return client;
    }

    @Bean
    public String googleCloudProjectId() {
        return projectId;
    }

    @Bean
    public String googleCloudLocation() {
        return location;
    }
}