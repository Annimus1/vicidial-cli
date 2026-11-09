package dev.pablo.api.credential;

import java.io.IOException;
import java.util.concurrent.Callable;

import dev.pablo.api.VicidialClientSingleton;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Creates a credential (user and phone) in Vicidial.
 *
 * <p>
 * Subcommand of {@code credential}. Replaces the flat {@code createCreds}
 * command: it creates the user first and then the phone that belongs to it.
 * </p>
 */
@Command(name = "create", description = {
    "Creates a credential with its user and phone.",
    "",
    "Examples:",
    "  vicidial-cli credential create --id agent001 --password S3cr3t --group UG_DEFAULT",
    "  vicidial-cli credential create --id agent001 --password S3cr3t --group UG_DEFAULT -n \"Agent One\""
}, mixinStandardHelpOptions = true)
public class CredentialCreateCommand implements Callable<Integer> {

    @Option(names = { "-id",
        "--agent_user", "--id" }, description = "Credential user ID (used for both the user and its phone).",
        defaultValue = "")
    private String id;

    @Option(names = { "-p",
        "--password" }, description = "Password for the credential.", defaultValue = "")
    private String password;

    @Option(names = { "-g",
        "--group", "--user_group" }, description = "User group ID the user belongs to.", defaultValue = "")
    private String userGroupId;

    @Option(names = { "-n",
        "--name" }, description = "Display name to identify the agent in reports.", defaultValue = "")
    private String name;

    private final VicidialClientSingleton client;

    public CredentialCreateCommand() {
        this(VicidialClientSingleton.getInstance());
    }

    CredentialCreateCommand(VicidialClientSingleton client) {
        this.client = client;
    }

    @Override
    public Integer call() {
        if (id.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"ID\" is missing. |@ "));
            return 1;
        }

        if (password.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"Password\" is missing. |@ "));
            return 1;
        }

        if (userGroupId.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"User Group\" is missing. |@ "));
            return 1;
        }

        try {
            client.requirePhoneConfig();

            String userId = id.trim();
            String displayName = name.isBlank() ? userId + "1" : name;

            System.out.println(Ansi.AUTO.text("⏳ @|yellow Creating Credentials ...|@ "));
            client.createUser(userId, password, displayName, userGroupId);
            System.out.println(Ansi.AUTO.text("☑️ @|blue User has been created: " + userId + " |@"));

            client.createPhone(userId, password);
            System.out.println(Ansi.AUTO.text("☑️ @|blue Phone has been created: " + userId + " |@"));

            System.out.println(Ansi.AUTO.text("✅ @|green Credentials successfully created. |@"));
            return 0;
        } catch (IOException e) {
            System.out.println(Ansi.AUTO.text("❌ @|red API or network error:|@ " + e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            System.out.println(Ansi.AUTO.text("❌ @|red The request was interrupted.|@"));
            Thread.currentThread().interrupt();
            return 1;
        } catch (IllegalStateException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + e.getMessage() + " |@"));
            return 1;
        }
    }
}
