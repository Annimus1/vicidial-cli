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
    description="Crea un nuevo Lead basado en otro existente y lo coloca en una lista determinada.",
    mixinStandardHelpOptions = true
)
public class DuplicateLeadCommand implements Callable<Integer> {

    private VicidialClientSingleton client;
    @Parameters(index = "0")
    private String leadId;
    @Parameters(index = "1")
    private String listId;
    @Option(names = { "-c", "--comments"}, defaultValue="")
    private String comments;
    @Option(names = { "-e", "--email" }, defaultValue="")
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
            System.err.println(Ansi.AUTO.text("❌ @|red Error de la API o de red:|@ " + e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red La petición fue interrumpida.|@"));
            Thread.currentThread().interrupt(); 
            return 1;
        }
    }

}
