package dev.pablo.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

import java.util.concurrent.Callable;

import dev.pablo.api.CampaignsCommand;
import dev.pablo.api.LeadDetailCommand;
import dev.pablo.api.DuplicateLeadCommand;
import dev.pablo.api.UpdateCredCommand;
import dev.pablo.api.CreatCredentialCommand;
import dev.pablo.api.DeleteDIDCommand;

@Command(name = "vicidial-cli", mixinStandardHelpOptions = true, // Enables --help, -h, --version
    version = "Vicidial CLI 1.0", description = "Command-line tool for the Vicidial API.")
public class MainApplication implements Callable<Integer> {
    @CommandLine.Spec
    CommandSpec spec;

    public static void main(String[] args) {
        // Use Picocli as the command engine instead of custom API handling

        int exitCode = new CommandLine(new MainApplication())
                .addSubcommand("createCreds", CreatCredentialCommand.class)
                .addSubcommand("duplicateInList", DuplicateLeadCommand.class)
                .addSubcommand("getAllCampaigns", CampaignsCommand.class)
                .addSubcommand("leadDetails", LeadDetailCommand.class)
                .addSubcommand("updateCred", UpdateCredCommand.class)
                .addSubcommand("deleteDIDs", DeleteDIDCommand.class)

                .execute(args);

        System.exit(exitCode);
    }

    @Override
    public Integer call() {

        // Default behavior
        CommandLine.usage(spec.commandLine(), System.out);

        return 0;
    }
}