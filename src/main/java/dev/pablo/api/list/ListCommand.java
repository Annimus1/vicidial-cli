package dev.pablo.api.list;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Entry point for the list operations.
 *
 * <p>
 * Groups the list subcommands so {@code list} reads as a namespace:
 * {@code list list}, {@code list active}, {@code list pause}, {@code list move}
 * and {@code list reset}.
 * </p>
 */
@Command(name = "list", description = {
    "Work with the lists stored on a Vicidial instance.",
    "",
    "Subcommands:",
    "  list     - show the lists on the instance",
    "  create   - create a list and attach it to a campaign",
    "  active   - set a list to active",
    "  pause    - set a list to inactive",
    "  move     - move a list to a different campaign",
    "  reset    - reset the called-since-last-reset flag for every lead in a list",
    "",
    "Examples:",
    "  vicidial-cli list list",
    "  vicidial-cli list list --activeOnly",
    "  vicidial-cli list list --campaign SALES",
    "  vicidial-cli list create --name SALES --desc \"Sales leads\" --campaign-id SALES",
    "  vicidial-cli list active --id 6004",
    "  vicidial-cli list pause --id 6004",
    "  vicidial-cli list move --campaignId SALES --id 6004",
    "  vicidial-cli list reset --id 6004"
}, mixinStandardHelpOptions = true,
    subcommands = { ListListCommand.class, ListCreateCommand.class, ListActiveCommand.class,
        ListPauseCommand.class, ListMoveCommand.class, ListResetCommand.class })
public class ListCommand implements Callable<Integer> {

    @CommandLine.Spec
    CommandSpec spec;

    /**
     * Prints the help when {@code list} is called with no subcommand.
     *
     * @return 0
     */
    @Override
    public Integer call() {
        CommandLine.usage(spec.commandLine(), System.out);
        return 0;
    }
}
