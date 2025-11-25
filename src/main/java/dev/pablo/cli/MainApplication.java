package dev.pablo.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

import java.util.concurrent.Callable;

import dev.pablo.api.CampaignsCommand;
import dev.pablo.api.LeadDetailCommand;
import dev.pablo.api.DuplicateLeadCommand;
import dev.pablo.api.UpdateCredCommand;

@Command(
    name = "vicidial-cli",
    mixinStandardHelpOptions = true, // Habilita --help, -h, --version
    version = "Vicidial CLI 1.0",
    description = "Herramienta de línea de comandos para la API de Vicidial."
)
public class MainApplication implements Callable<Integer> {
    @CommandLine.Spec
    CommandSpec spec; 

    public static void main(String[] args) {
        // Reemplazamos la lógica de la API por el motor de Picocli
        
        int exitCode = new CommandLine(new MainApplication())
                            .addSubcommand("getAllCampaigns", CampaignsCommand.class)
                            .addSubcommand("leadDetails", LeadDetailCommand.class)
                            .addSubcommand("duplicateInList", DuplicateLeadCommand.class)
                            .addSubcommand("updateCred", UpdateCredCommand.class)

                            .execute(args);
            
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() {
        
        // Comportamiento por defecto
        CommandLine.usage(spec.commandLine(), System.out); 
        
        return 0; 
    }
}