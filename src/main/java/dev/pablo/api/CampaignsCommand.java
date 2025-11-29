package dev.pablo.api;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(
    name = "getAllCampaigns", 
    description = "Gets and displays the list of active/inactive campaigns from Vicidial.",
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
            // 2. Call the API layer method to obtain formatted data
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

            return 0; // Success

        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red API or network error:|@ " + e.getMessage()));
            return 1; // Error
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red The request was interrupted.|@"));
            Thread.currentThread().interrupt();
            return 1;
        }
    }
}