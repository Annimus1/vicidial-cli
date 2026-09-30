package dev.pablo.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;

import java.util.concurrent.Callable;

import dev.pablo.api.CampaignsCommand;
import dev.pablo.api.LeadDetailCommand;
import dev.pablo.api.DuplicateLeadCommand;
import dev.pablo.api.UpdateCredCommand;
import dev.pablo.api.CreatCredentialCommand;
import dev.pablo.api.DIDsCommand;
import dev.pablo.api.VicidialClientSingleton;

@Command(name = "vicidial-cli", mixinStandardHelpOptions = true,
    version = "Vicidial CLI 1.0", description = "Command-line tool for the Vicidial API.")
public class MainApplication implements Callable<Integer> {
    @CommandLine.Spec
    CommandSpec spec;

    public static void main(String[] args) {
        // Use Picocli as the command engine instead of custom API handling

        if (!exitOnMissingCredentials(args)) {
            return;
        }

        CommandLine commandLine = new CommandLine(new MainApplication())
                .addSubcommand("campaign", CampaignsCommand.class)
                .addSubcommand("leadDetails", LeadDetailCommand.class)
                .addSubcommand("duplicateInList", DuplicateLeadCommand.class)
                .addSubcommand("createCreds", CreatCredentialCommand.class)
                .addSubcommand("updateCred", UpdateCredCommand.class)
                .addSubcommand("deleteDIDs", DIDsCommand.class);

        System.exit(commandLine.execute(args));
    }

    /**
     * Verifies the credentials every command needs before picocli builds the
     * command tree.
     *
     * <p>
     * Commands read the configuration in their constructor, and picocli reports
     * a constructor failure as an unhandled exception with a stack trace. The
     * check is done here so a missing variable produces a readable message
     * instead.
     * </p>
     *
     * @param args the raw command line arguments
     * @return true when the run can continue, false when it must be aborted with
     *         exit code 1
     */
    private static boolean exitOnMissingCredentials(String[] args) {
        try {
            VicidialClientSingleton.getInstance().requireConfig(
                    VicidialClientSingleton.VAR_BASE_URL,
                    VicidialClientSingleton.VAR_API_USER,
                    VicidialClientSingleton.VAR_API_PASSWORD);
            return true;
        } catch (IllegalStateException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + e.getMessage() + " |@"));
            System.exit(1);
            return false;
        }
    }

    @Override
    public Integer call() {

        // Default behavior
        CommandLine.usage(spec.commandLine(), System.out);

        return 0;
    }
}