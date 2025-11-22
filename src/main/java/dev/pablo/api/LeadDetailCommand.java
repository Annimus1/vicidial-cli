package dev.pablo.api;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Help.Ansi;

@Command(name="leadDetails", description="Muestra toda la informacion de un contacto en específico.")
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
            String[] details = leadDetails.split("\\|");
            String[] fields = {
                "status",
                "user",
                "vendor_lead_code",
                "source_id",
                "list_id",
                "gmt_offset_now",
                "phone_code",
                "phone_number",
                "title",
                "first_name",
                "middle_initial",
                "last_name",
                "address1",
                "address2",
                "address3",
                "city",
                "state",
                "province",
                "postal_code",
                "country_code",
                "gender",
                "date_of_birth",
                "alt_phone",
                "email",
                "security_phrase",
                "comments",
                "called_count",
                "last_local_call_time",
                "rank",
                "owner",
                "entry_list_id",
                "lead_id"
            };

            System.out.println(Ansi.AUTO.text("\n@|green ✅ Listas Asociadas:|@"));
            System.out.println("---------------------------------------------------------");           
            for(int i=0; i<details.length; i++){
                    System.out.println(fields[i] +": "+details[i]);
                }
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
