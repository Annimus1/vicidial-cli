package dev.pablo.api.lead;

import java.io.IOException;
import java.util.concurrent.Callable;

import dev.pablo.models.LeadModel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Shows all information for one lead.
 *
 * <p>
 * Subcommand of {@code lead}. Replaces the flat {@code leadDetails} command.
 * </p>
 */
@Command(name = "info", description = {
    "Displays all information for a specific contact (lead).",
    "",
    "Examples:",
    "  vicidial-cli lead info --id 1125",
    "  vicidial-cli lead info -id 1125"
}, mixinStandardHelpOptions = true)
public class LeadInfoCommand implements Callable<Integer> {

    @Option(names = { "-id",
        "--lead_id", "--id" }, description = "The lead ID (e.g. '1125').", defaultValue = "")
    private String leadId;

    private final LeadRepository repository;

    public LeadInfoCommand() {
        this(new LeadRepository());
    }

    LeadInfoCommand(LeadRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() {
        if (leadId.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"Lead_Id\" is missing. |@ "));
            return 1;
        }

        String id = leadId.trim();
        System.out.println(Ansi.AUTO.text("@|yellow Searching lead details for ID: " + id + "...|@"));

        try {
            LeadModel lead = repository.fetchInfo(id);

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
