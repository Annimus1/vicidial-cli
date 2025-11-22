package dev.pablo.cli;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import dev.pablo.api.VicidialClient;

public class MainApplication {

    public static void main(String[] args) {
        // 1. Crear el HttpClient (El motor HTTP)
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
            
        // 2. Crear una instancia de la clase cliente API (VicidialClient)
        VicidialClient client = new VicidialClient(httpClient);
        
        try {
            // 3. Ejecutar la llamada síncrona
            // El programa se detiene en este punto hasta que hay respuesta.
            String rawResult = client.getCampaignsSync(); 
            
            // 4. Mostrar el resultado
            System.out.println(rawResult); 
            
        } catch (IOException e) {
            // Manejo de errores de red o códigos de estado HTTP no exitosos
            System.err.println("❌ Error en la comunicación con la API:");
            System.err.println(e.getMessage());
            
        } catch (InterruptedException e) {
            // Manejo de interrupción del hilo
            System.err.println("❌ La petición fue interrumpida.");
            // Restaurar el estado de interrupción
            Thread.currentThread().interrupt(); 
        }
    }
}
