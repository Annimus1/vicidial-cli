package dev.pablo.api;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Help.Ansi;

import dev.pablo.models.LeadModel;

@Command(
    name="leadDetails", 
    description="Displays all information for a specific contact (lead).",
    mixinStandardHelpOptions = true
)
public class LeadDetailCommand implements Callable<Integer> {

    private final VicidialClientSingleton client;
    @Parameters(index="0",description="The lead ID (e.g., '1125')")
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

            System.out.println(Ansi.AUTO.text("\n@|green ✅ Associated Lists:|@"));
            System.out.println("---------------------------------------------------------");           
            System.out.println(lead.toString());
            System.out.println("---------------------------------------------------------");
            
            return 0;
        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red API or network error:|@ " + e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red The request was interrupted.|@"));
            Thread.currentThread().interrupt(); 
            return 1;
        }
    }

}
