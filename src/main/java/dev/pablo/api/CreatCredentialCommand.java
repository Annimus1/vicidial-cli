package dev.pablo.api;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Help.Ansi;

/**
 * Command-line action to create a credential (user and phone) in Vicidial.
 *
 * <p>This command is registered as "createCreds". It creates a user and a phone
 * using the shared {@link VicidialClientSingleton} instance and prints progress and result
 * messages to standard output.</p>
 *
 * <p>Usage:
 * <pre>
 *   createCreds &lt;ID&gt; &lt;password&gt; &lt;userGroupId&gt; [-n|--name &lt;displayName&gt;]
 *   Example: createCreds agent001 S3cr3t UG_DEFAULT -n "Agent One"
 * </pre>
 * </p>
 *
 * <p>Exit codes:
 * <ul>
 *   <li>0 — Credentials successfully created.</li>
 *   <li>1 — API/network error or interruption</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
@Command(
    name = "createCreds",
    description = {
        "Creates a credential with its user and phone.",
        "Usage: vicidial-cli createCreds <ID> <password> <userGroupId> [-n|--name <displayName>]",
        "Example: vicidial-cli createCreds agent001 secret_password TestGroup -n \"Agent One\""
    },
    mixinStandardHelpOptions = true
)
public class CreatCredentialCommand implements Callable<Integer> {
    /** Vicidial API client singleton used to perform create operations. */
    private VicidialClientSingleton client;

    /** Credential user ID (used for both the user's campaign and phone). */
    @Parameters(index = "0", description = "Credential user ID (used for both the user's campaign and phone)")
    private String ID;

    /** Password for the credential. */
    @Parameters(index = "1", description = "Password for the credential.")
    private String password;

    /** User group ID the user belongs to. */
    @Parameters(index = "2", description = "User group ID the user belongs to.")
    private String userGroupId;

    /** Display name to identify the agent in reports. Defaults to empty string. */
    @Option(names = {"-n", "--name"}, description = "Display name to identify the agent in reports.", defaultValue="")
    private String name;

    /**
     * Creates a new command instance and obtains the shared Vicidial client.
     */
    public CreatCredentialCommand() {
        this.client = VicidialClientSingleton.getInstance();
    }

    /**
     * Executes the creation flow: creates a user and then a phone.
     *
     * <p>If the optional display name is not provided, a default is derived from the ID.</p>
     *
     * @return 0 on success, 1 on error (API/network error or interruption)
     */
    @Override
    public Integer call() {
        try {
            // Create User
            System.out.println(Ansi.AUTO.text("⏳ @|yellow Creating Credentials ...|@ "));
            if(this.name.isEmpty()){
                this.name= this.ID + "1";
            }
            client.createUser(ID, password, name, userGroupId);
            System.out.println(Ansi.AUTO.text("☑️ @|blue User has been created: " + ID + " |@"));

            // Create Phone
            client.createPhone(ID, password);
            System.out.println(Ansi.AUTO.text("☑️ @|blue Phone has been created: " + ID + " |@"));

            System.out.println(Ansi.AUTO.text("✅ @|green Credentials successfully created. |@"));

            return 0;
        } catch (IOException e) {
            System.out.println(Ansi.AUTO.text("❌ @|red API or network error:|@ " + e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            System.out.println(Ansi.AUTO.text("❌ @|red The request was interrupted.|@"));
            return 1;
        }
    }

}
