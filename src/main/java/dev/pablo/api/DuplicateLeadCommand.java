package dev.pablo.api;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.lang.InterruptedException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Help.Ansi;;

@Command( 
    name="DuplicateInList", 
    description="Creates a new Lead based on an existing one and places it in a specific list.",
    mixinStandardHelpOptions = true
)
public class DuplicateLeadCommand implements Callable<Integer> {

    private VicidialClientSingleton client;
    @Parameters(index = "0", description="The lead ID (contact identifier).")
    private String leadId;
    @Parameters(index = "1", description="Destination list ID.")
    private String listId;
    @Option(names = { "-c", "--comments"}, description = "Agent notes.", defaultValue="")
    private String comments;
    @Option(names = { "-e", "--email" }, description = "Email to overwrite (optional).", defaultValue="")
    private String email;

    public DuplicateLeadCommand() {
        this.client = VicidialClientSingleton.getInstance();
    }

    @Override
    public Integer call() throws IOException, InterruptedException  {
        try {
            client.DuplicateLeadInList(leadId, listId, comments, email);
            return 0;
        }catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red API or network error:|@ " + e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red The request was interrupted.|@"));
            Thread.currentThread().interrupt(); 
            return 1;
        }
    }

}
