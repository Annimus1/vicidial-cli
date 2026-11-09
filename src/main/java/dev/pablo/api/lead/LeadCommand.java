package dev.pablo.api.lead;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Entry point for the lead operations.
 *
 * <p>
 * Groups the lead subcommands so {@code lead} reads as a namespace:
 * {@code lead info} and {@code lead duplicate}.
 * </p>
 */
@Command(name = "lead", description = {
    "Work with the leads stored on a Vicidial instance.",
    "",
    "Subcommands:",
    "  info        - show all information for one lead",
    "  duplicate   - duplicate a lead into a list",
    "",
    "Examples:",
    "  vicidial-cli lead info --id 1125",
    "  vicidial-cli lead duplicate --lead-id 12345 --list-id 10",
    "  vicidial-cli lead duplicate --lead-id 12345 --list-id 10 -c \"Transfer to new list\" -e new@example.com"
}, mixinStandardHelpOptions = true,
    subcommands = { LeadInfoCommand.class, LeadDuplicateCommand.class })
public class LeadCommand implements Callable<Integer> {

    @CommandLine.Spec
    CommandSpec spec;

    /**
     * Prints the help when {@code lead} is called with no subcommand.
     *
     * @return 0
     */
    @Override
    public Integer call() {
        CommandLine.usage(spec.commandLine(), System.out);
        return 0;
    }
}
