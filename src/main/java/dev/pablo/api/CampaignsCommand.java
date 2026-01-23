package dev.pablo.api;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Parameters;

@Command(name = "Campaigns", description = "Gets and displays the list of active/inactive campaigns from Vicidial.", mixinStandardHelpOptions = true)
public class CampaignsCommand implements Callable<Integer> {

    private final VicidialClientSingleton client;

    @Parameters(index = "0", description = "Command to perform the action.\n"
            + "INFO - \n"
            + "CREATE - \n"
            + "STATUS - \n")
    private String command;

    @Option(names = { "-id", "--campaign_id" }, description = ". (optional)", defaultValue = "")
    private String campaign_ID;

    @Option(names = { "-a", "--active" }, description = " 'Y' or 'N'. (optional)", defaultValue = "")
    private String active;

    // TODO
    // 1) ✅ Obtener info de las campa#as INFO
    // 2) Crear nueva campa#a CREATE <Campaign_ID> --name [<name> | camp_ID] --creds
    // [<> | 1]
    // 3) ✅ Actualizar el estado de la campa#a STATUS <Campaign_ID> <yes/no>

    public CampaignsCommand() {
        this.client = VicidialClientSingleton.getInstance();
    }

    @Override
    public Integer call() {

        try {
            if (command.toLowerCase().equals("info")) {
                getCampaign_Info();
            }

            else if (command.toLowerCase().equals("create")) {
                System.out.println("Create");
            }

            else if (command.toLowerCase().equals("status")) {
                if (!campaign_ID.isBlank() && !active.isBlank()) {
                    System.out.println(Ansi.AUTO.text("@|blue Preparing for update Campaign...|@"));
                    client.updateCampaignStatus(campaign_ID, active);
                } else {
                    System.err.println(
                            Ansi.AUTO.text("❌ @|red Parameter \"Campaign_Id\" or \"active\" are missing. |@ "));
                    return 1;
                }
            }

            else {
                System.err.println(Ansi.AUTO.text("❌ @|red Argument " + command + " unknown. |@ "));
                return 1;
            }

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

    public void getCampaign_Info() throws IOException, InterruptedException {
        System.out.println(Ansi.AUTO.text("@|yellow Searching campaigns...|@"));

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
    }

}