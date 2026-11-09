package dev.pablo.api.credential;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import dev.pablo.models.UserModel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Lists the users configured on a Vicidial instance.
 *
 * <p>
 * Subcommand of {@code credential}. Calls {@code users_list} through the
 * Non-Agent API.
 * </p>
 */
@Command(name = "list", description = {
    "List the users stored on the instance.",
    "",
    "Examples:",
    "  vicidial-cli credential list",
    "  vicidial-cli credential list --id agent001"
}, mixinStandardHelpOptions = true)
public class CredentialListCommand implements Callable<Integer> {

    @Option(names = { "-id",
        "--agent_user", "--id" }, description = "Only show this user.", defaultValue = "")
    private String agentUser;

    private final CredentialRepository repository;

    public CredentialListCommand() {
        this(new CredentialRepository());
    }

    CredentialListCommand(CredentialRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() {
        try {
            List<UserModel> users = repository.fetchAll(agentUser.trim());

            CredentialTable.print(users, "Users found");
            return 0;
        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red API or network error: |@" + e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red The request was interrupted. |@"));
            Thread.currentThread().interrupt();
            return 1;
        }
    }
}
