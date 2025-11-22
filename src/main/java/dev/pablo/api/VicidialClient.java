package dev.pablo.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import io.github.cdimascio.dotenv.Dotenv;

public class VicidialClient{

    private final HttpClient client;
    private final String baseUrl;
    // Configuración de credenciales (se cargan desde .env o variables de entorno)
    private final String apiUser;
    private final String apiPass;
    private final String source = "java";

    public VicidialClient(HttpClient client) {
        // Configuramos el cliente con un timeout para evitar bloqueos infinitos.
        this.client = client;

        // Cargamos variables desde .env si existe, y si no, usamos las variables de entorno del sistema.
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();

        String envBase = dotenv.get("BASE_URL");
        String envUser = dotenv.get("API_USER");
        String envPass = dotenv.get("API_PASSWORD");

        String sysBase = System.getenv("BASE_URL");
        String sysUser = System.getenv("API_USER");
        String sysPass = System.getenv("API_PASSWORD");

        this.baseUrl = (envBase != null && !envBase.isBlank()) ? envBase : sysBase;
        this.apiUser = (envUser != null && !envUser.isBlank()) ? envUser : sysUser;
        this.apiPass = (envPass != null && !envPass.isBlank()) ? envPass : sysPass;

        if (this.baseUrl == null || this.apiUser == null || this.apiPass == null) {
            throw new IllegalStateException("Faltan credenciales: define BASE_URL, API_USER y API_PASSWORD en .env o en variables de entorno.");
        }
    }

    private String buildApiUrl(String functionName) {
        // Construye la URL con la función get_campaign_list
        return new StringBuilder()
            .append(baseUrl)
            .append("?source=").append(source)
            .append("&user=").append(apiUser)
            .append("&pass=").append(apiPass)
            .append("&function=").append(functionName)
            .toString();
    }
    
    /**
     * Realiza una petición GET SÍNCRONA para obtener todas las campañas.
     * * @return El cuerpo de la respuesta de la API (JSON/XML) como String.
     * @throws IOException Si ocurre un error de entrada/salida (red).
     * @throws InterruptedException Si el hilo es interrumpido durante la espera.
     */
    public String getCampaignsSync() throws IOException, InterruptedException {
        String url = buildApiUrl("campaigns_list");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json") 
                .timeout(Duration.ofSeconds(15)) // Timeout de petición
                .build();
        
        // Ejecución SÍNCRONA: El hilo se bloquea aquí hasta que se recibe la respuesta.
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Manejo de códigos de estado básicos
        if (response.statusCode() != 200) {
            throw new IOException("Error al llamar a la API de Vicidial. Código de estado: " + response.statusCode());
        }

        // Devuelve el cuerpo de la respuesta
        String respuesta = response.body();
        String[] rows = respuesta.split("\\R");
        ArrayList<String> campaigns = new ArrayList<>(); 
        
        for( String row : rows){
            String[] segments = row.split("\\|");
            String active = segments[2].equalsIgnoreCase("y") ? "✅" : "❌";                
            
          campaigns.add(active + " ID: " + segments[0] + " | Description: " + segments[1]);
        }

        return String.join("\n", campaigns);
    }
}
