package dev.pablo.api.credential;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Sets a user to inactive.
 *
 * <p>
 * Subcommand of {@code credential}. Calls {@code update_user} with
 * {@code active=N} through the Non-Agent API.
 * </p>
 */
@Command(name = "pause", description = {
    "Change the status of a credential to PAUSED.",
    "",
    "Examples:",
    "  vicidial-cli credential pause --id agent001",
    "  vicidial-cli credential pause -id agent001"
}, mixinStandardHelpOptions = true)
public class CredentialPauseCommand implements Callable<Integer> {

    @Option(names = { "-id",
        "--agent_user", "--id" }, description = "Credential identifier.", defaultValue = "")
    private String id;

    private final CredentialRepository repository;

    public CredentialPauseCommand() {
        this(new CredentialRepository());
    }

    CredentialPauseCommand(CredentialRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() {
        if (id.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"ID\" is missing. |@ "));
            return 1;
        }

        String userId = id.trim();

        try {
            String response = repository.setActive(userId, false);

            if (!CredentialRepository.isSuccess(response)) {
                System.err.println(Ansi.AUTO.text("❌ @|red Credential " + userId + " was not paused: "
                        + CredentialRepository.responseMessage(response) + " |@"));
                return 1;
            }

            System.out.println(Ansi.AUTO.text("@|green ✅ Credential " + userId + " is now paused. |@"));
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
