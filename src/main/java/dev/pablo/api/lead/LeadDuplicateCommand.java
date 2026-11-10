package dev.pablo.api.lead;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Duplicates a lead into a list.
 *
 * <p>
 * Subcommand of {@code lead}. Replaces the flat {@code duplicateInList} command.
 * </p>
 */
@Command(name = "duplicate", description = {
    "Creates a new Lead based on an existing one and places it in a specific list.",
    "",
    "Examples:",
    "  vicidial-cli lead duplicate --lead-id 12345 --list-id 10",
    "  vicidial-cli lead duplicate --lead-id 12345 --list-id 10 -c \"Transfer to new list\" -e new@example.com"
}, mixinStandardHelpOptions = true)
public class LeadDuplicateCommand implements Callable<Integer> {

    @Option(names = { "--lead-id",
        "--lead_id" }, description = "The lead ID (contact identifier).", defaultValue = "")
    private String leadId;

    @Option(names = { "--list-id",
        "--list_id" }, description = "Destination list ID.", defaultValue = "")
    private String listId;

    @Option(names = { "-c", "--comments" }, description = "Agent notes.", defaultValue = "")
    private String comments;

    @Option(names = { "-e", "--email" }, description = "Email to overwrite (optional).", defaultValue = "")
    private String email;

    private final LeadRepository repository;

    public LeadDuplicateCommand() {
        this(new LeadRepository());
    }

    LeadDuplicateCommand(LeadRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() {
        if (leadId.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"Lead_Id\" is missing. |@ "));
            return 1;
        }

        if (listId.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"List_Id\" is missing. |@ "));
            return 1;
        }

        try {
            repository.duplicate(leadId.trim(), listId.trim(), comments, email);
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
