package dev.pablo.api;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Help.Ansi;

@Command(name = "createCreds", description = "Creates a credential with its user and phone.", mixinStandardHelpOptions = true)
public class CreatCredentialCommand implements Callable<Integer> {
    private VicidialClientSingleton client;

    @Parameters(index = "0", description = "Credential user ID (used for both the campaign and the Phone)")
    private String ID;
    @Parameters(index = "1", description = "Password for the credential.")
    private String password;
    @Parameters(index = "2", description = "User group ID the user belongs to.")
    private String userGroupId;
    @Option(names = {"-n", "--name"}, description = "Display name to identify the agent in reports.", defaultValue="")
    private String name;

    public CreatCredentialCommand() {
        this.client = VicidialClientSingleton.getInstance();
    }

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
