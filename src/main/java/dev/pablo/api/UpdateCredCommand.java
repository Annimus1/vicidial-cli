package dev.pablo.api;

import java.io.IOException;
import java.lang.InterruptedException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Help.Ansi;

/**
 * Command-line action to update credentials (user and phone) in Vicidial.
 *
 * <p>This command updates a user's display name and/or password and, if a new password
 * is provided, updates the associated phone password as well using the shared
 * {@link VicidialClientSingleton} instance.</p>
 *
 * <p>Usage:
 * <pre>
 *   updateCred &lt;ID&gt; [-n|--name &lt;displayName&gt;] [-p|--password &lt;newPassword&gt;]
 *   Example: updateCred agent001 -n "Jhon Doe" -p S3cr3t
 * </pre>
 * </p>
 *
 * <p>Exit codes:
 * <ul>
 *   <li>0 — Credentials successfully updated.</li>
 *   <li>1 — Validation error (no update fields provided), API/network error, or interruption.</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
@Command(
    name="updateCred",
    description={
        "Updates credentials for a User and a Phone at the same time.",
        "Usage: vicidial-cli updateCred <ID> [-n|--name <displayName>] [-p|--password <newPassword>]",
        "Example: vicidial-cli updateCred agent001 -n \"Jhon Doe\" -p S3cr3t"
    },
    mixinStandardHelpOptions=true
)
public class UpdateCredCommand implements Callable<Integer> {

    /** Vicidial API client singleton used to communicate with the Vicidial server. */
    private VicidialClientSingleton client;

    /** Credential identifier (user and phone name). */
    @Parameters(index = "0", description = "Credential identifier.")
    private String ID;

    /** New display name for the user (optional). */
    @Option(names = {"-n", "--name"}, description = "New name for the updated user.", defaultValue="")
    private String name;

    /** New password for both User and Phone (optional). */
    @Option(names = {"-p", "--password"}, description = "New password for the User and the Phone.", defaultValue="")
    private String password;

    /**
     * Constructs the command and obtains the shared Vicidial client instance.
     */
    public UpdateCredCommand() {
        this.client = VicidialClientSingleton.getInstance();
    }

    /**
     * Returns the credential identifier this command will operate on.
     *
     * @return the credential ID
     */
    public String getID(){
        return this.ID;
    }

    /**
     * Sets the credential identifier this command will operate on.
     *
     * @param ID the credential ID to set
     */
    public void setID(String ID){
        this.ID = ID;
    }

    /**
     * Executes the update flow.
     *
     * <p>This method validates that at least one of name or password is provided,
     * updates the user via the Vicidial client, and if a new password is supplied,
     * updates the associated phone password as well.</p>
     *
     * @return 0 on success, 1 on validation error, API/network error, or interruption
     * @throws Exception propagated by the Callable contract (caught internally for IO/Interrupted cases)
     */
    @Override
    public Integer call() throws Exception {
        System.out.println(Ansi.AUTO.text("⏱️  @|yellow Updating Credentials for: " + ID + "...|@"));
        
        if(password.isEmpty() && name.isEmpty()){
            System.err.println(Ansi.AUTO.text("❌ @|red In order to update credentials you must provide either name, password or both.|@"));
            return 1;
        }
        
        try{
            System.out.println(Ansi.AUTO.text("@|blue Updating User ...|@"));
            client.updateUser(ID, name, password);

            System.err.println(Ansi.AUTO.text("☑️ @|blue Name and password has been updated.|@"));
            
            if(!password.isEmpty()){
                System.out.println(Ansi.AUTO.text("@|blue Updating Phone ...|@"));
                client.updatePhone(ID, password);
                System.err.println(Ansi.AUTO.text("☑️ @|blue Phone's password has been updated.|@"));
            }
            System.out.println(Ansi.AUTO.text("✅ @|green The credentials have been successfully updated.|@"));
        }
        catch(IOException e){
            System.err.println(Ansi.AUTO.text("❌ @|red API or network error: |@ " + e.getMessage()));
            return 1;
        }
        catch(InterruptedException e ){
            System.err.println(Ansi.AUTO.text("❌ @|red The request was interrupted.|@"));
            Thread.currentThread().interrupt(); 
            return 1;
        } 
        return 0;
    }

}
