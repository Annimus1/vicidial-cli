package dev.pablo.api;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Help.Ansi;

import dev.pablo.models.LeadModel;

@Command(
    name="leadDetails", 
    description="Muestra toda la informacion de un contacto en específico.",
    mixinStandardHelpOptions = true
)
public class LeadDetailCommand implements Callable<Integer> {

    private final VicidialClientSingleton client;
    @Parameters(index="0",description="El ID del contacto (ej. '1125')")
    private String leadId;

    public LeadDetailCommand() {
        this.client = VicidialClientSingleton.getInstance();
    }

    @Override
    public Integer call() throws Exception {
        System.out.println(Ansi.AUTO.text("@|yellow Searching lead details for ID: " + leadId + "...|@"));

        try {
            String leadDetails = client.getLeadInfo(leadId);
            LeadModel lead = new LeadModel(leadDetails);

            System.out.println(Ansi.AUTO.text("\n@|green ✅ Listas Asociadas:|@"));
            System.out.println("---------------------------------------------------------");           
            System.out.println(lead.toString());
            System.out.println("---------------------------------------------------------");
            
            return 0;
        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red Error de la API o de red:|@ " + e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red La petición fue interrumpida.|@"));
            Thread.currentThread().interrupt(); 
            return 1;
        }
    }

}
