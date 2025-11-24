package dev.pablo.api;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(
    name = "getAllCampaigns", 
    description = "Obtiene y muestra la lista de campañas activas/inactivas de Vicidial.",
    mixinStandardHelpOptions = true
)
public class CampaignsCommand implements Callable<Integer> {
    private final VicidialClientSingleton client;

    public CampaignsCommand() {
        this.client = VicidialClientSingleton.getInstance();
    }

    @Override
    public Integer call() {
        System.out.println(Ansi.AUTO.text("@|yellow Searching campaigns...|@"));

        try {
            // 2. Llama al método de la capa API para obtener los datos formateados
            String formattedResult = client.getCampaigns();

            System.out.println(Ansi.AUTO.text("\n@|blue Campaigns list's obtained:|@"));
            System.out.println("---------------------------------------------------------");
            String[] rows = formattedResult.split("\\R");

            for (String row : rows) {
                String[] segments = row.split("\\|");
                String active = segments[2].equalsIgnoreCase("y") ? "✅" : "❌";

                System.out.println(active + " ID: " + segments[0] + " | Description: " + segments[1]);
            }
            System.out.println("---------------------------------------------------------");

            return 0; // Éxito

        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red Error de la API o de red:|@ " + e.getMessage()));
            return 1; // Error
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red La petición fue interrumpida.|@"));
            Thread.currentThread().interrupt();
            return 1;
        }
    }
}