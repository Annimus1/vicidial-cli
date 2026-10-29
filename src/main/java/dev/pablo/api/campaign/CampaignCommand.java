package dev.pablo.api.campaign;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Entry point for the campaign operations.
 *
 * <p>
 * Groups the campaign subcommands so {@code campaign} reads as a namespace:
 * {@code campaign list}, {@code campaign info}, {@code campaign create},
 * {@code campaign pause} and {@code campaign active}.
 * </p>
 */
@Command(name = "campaign", description = {
    "Work with the campaigns stored on a Vicidial instance.",
    "",
    "Subcommands:",
    "  list     - show the campaigns on the instance",
    "  info     - show one campaign, or list every campaign when no id is given",
    "  create   - create a campaign and the groups, list and users it needs",
    "  pause    - set a campaign to inactive",
    "  active   - set a campaign to active",
    "",
    "Examples:",
    "  vicidial-cli campaign list",
    "  vicidial-cli campaign info --id SALES",
    "  vicidial-cli campaign create --id SALES --description \"Sales campaign\"",
    "  vicidial-cli campaign pause --id SALES",
    "  vicidial-cli campaign active --id SALES"
}, mixinStandardHelpOptions = true,
    subcommands = { CampaignListCommand.class, CampaignInfoCommand.class, CampaignCreateCommand.class,
        CampaignPauseCommand.class, CampaignActiveCommand.class })
public class CampaignCommand implements Callable<Integer> {

    @CommandLine.Spec
    CommandSpec spec;

    /**
     * Prints the help when {@code campaign} is called with no subcommand.
     *
     * @return 0
     */
    @Override
    public Integer call() {
        CommandLine.usage(spec.commandLine(), System.out);
        return 0;
    }
}
