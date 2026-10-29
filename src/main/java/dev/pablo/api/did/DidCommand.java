package dev.pablo.api.did;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Entry point for the DID operations.
 *
 * <p>
 * Groups the DID subcommands so {@code did} reads as a namespace:
 * {@code did list}, {@code did verify}, {@code did remove} and {@code did add}.
 * </p>
 */
@Command(name = "did", description = {
    "Work with the DIDs stored on a Vicidial instance.",
    "",
    "Subcommands:",
    "  list     - show the DIDs on the instance",
    "  verify   - check that a DID exists and show what it is assigned to",
    "  remove   - remove one DID, a list of DIDs, or a whole group",
    "  add      - add DIDs (not implemented yet)",
    "",
    "Examples:",
    "  vicidial-cli did list",
    "  vicidial-cli did verify 15551234567",
    "  vicidial-cli did remove --did 15551234567",
    "  vicidial-cli did remove --group SALES_TEAM --dry-run"
}, mixinStandardHelpOptions = true,
    subcommands = { DidListCommand.class, DidVerifyCommand.class, DidRemoveCommand.class, DidAddCommand.class })
public class DidCommand implements Callable<Integer> {

    @CommandLine.Spec
    CommandSpec spec;

    /**
     * Prints the help when {@code did} is called with no subcommand.
     *
     * @return 0
     */
    @Override
    public Integer call() {
        CommandLine.usage(spec.commandLine(), System.out);
        return 0;
    }
}
