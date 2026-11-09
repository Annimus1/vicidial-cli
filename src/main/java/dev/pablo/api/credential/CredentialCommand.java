package dev.pablo.api.credential;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Entry point for the credential operations.
 *
 * <p>
 * Groups the credential subcommands so {@code credential} reads as a namespace:
 * {@code create}, {@code list}, {@code modify}, {@code active} and {@code pause}.
 * </p>
 */
@Command(name = "credential", description = {
    "Work with the credentials (users and phones) on a Vicidial instance.",
    "",
    "Subcommands:",
    "  create   - create a credential with its user and phone",
    "  list     - show the users on the instance",
    "  modify   - update a user's name and/or password",
    "  active   - set a credential to active",
    "  pause    - set a credential to inactive",
    "",
    "Examples:",
    "  vicidial-cli credential create --id agent001 --password S3cr3t --group UG_DEFAULT",
    "  vicidial-cli credential list",
    "  vicidial-cli credential list --id agent001",
    "  vicidial-cli credential modify --id agent001 -n \"John Doe\"",
    "  vicidial-cli credential active --id agent001",
    "  vicidial-cli credential pause --id agent001"
}, mixinStandardHelpOptions = true,
    subcommands = { CredentialCreateCommand.class, CredentialListCommand.class,
        CredentialModifyCommand.class, CredentialActiveCommand.class, CredentialPauseCommand.class })
public class CredentialCommand implements Callable<Integer> {

    @CommandLine.Spec
    CommandSpec spec;

    /**
     * Prints the help when {@code credential} is called with no subcommand.
     *
     * @return 0
     */
    @Override
    public Integer call() {
        CommandLine.usage(spec.commandLine(), System.out);
        return 0;
    }
}
