package dev.pablo.api.credential;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Updates a credential's user and phone.
 *
 * <p>
 * Subcommand of {@code credential}. Replaces the flat {@code updateCred}
 * command: it updates the user's display name and/or password and, when a new
 * password is given, the password of the associated phone.
 * </p>
 */
@Command(name = "modify", description = {
    "Updates credentials for a User and a Phone at the same time.",
    "",
    "Examples:",
    "  vicidial-cli credential modify --id agent001 -n \"John Doe\"",
    "  vicidial-cli credential modify --id agent001 -p N3wP@ss"
}, mixinStandardHelpOptions = true)
public class CredentialModifyCommand implements Callable<Integer> {

    @Option(names = { "-id",
        "--agent_user", "--id" }, description = "Credential identifier.", defaultValue = "")
    private String id;

    @Option(names = { "-n", "--name" }, description = "New name for the updated user.", defaultValue = "")
    private String name;

    @Option(names = { "-p",
        "--password" }, description = "New password for the User and the Phone.", defaultValue = "")
    private String password;

    private final CredentialRepository repository;

    public CredentialModifyCommand() {
        this(new CredentialRepository());
    }

    CredentialModifyCommand(CredentialRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() {
        if (id.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"ID\" is missing. |@ "));
            return 1;
        }

        if (password.isEmpty() && name.isEmpty()) {
            System.err.println(Ansi.AUTO.text(
                    "❌ @|red In order to update credentials you must provide either name, password or both.|@"));
            return 1;
        }

        String userId = id.trim();
        System.out.println(Ansi.AUTO.text("⏱️  @|yellow Updating Credentials for: " + userId + "...|@"));

        if (!password.isEmpty()) {
            try {
                repository.requirePhoneConfig();
            } catch (IllegalStateException e) {
                System.err.println(Ansi.AUTO.text("❌ @|red " + e.getMessage() + " |@"));
                return 1;
            }
        }

        try {
            System.out.println(Ansi.AUTO.text("@|blue Updating User ...|@"));
            repository.updateUser(userId, name, password);
            System.err.println(Ansi.AUTO.text("☑️ @|blue Name and password has been updated.|@"));

            if (!password.isEmpty()) {
                System.out.println(Ansi.AUTO.text("@|blue Updating Phone ...|@"));
                repository.updatePhone(userId, password);
                System.err.println(Ansi.AUTO.text("☑️ @|blue Phone's password has been updated.|@"));
            }

            System.out.println(Ansi.AUTO.text("✅ @|green The credentials have been successfully updated.|@"));
            return 0;
        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red API or network error: |@ " + e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red The request was interrupted.|@"));
            Thread.currentThread().interrupt();
            return 1;
        }
    }
}
