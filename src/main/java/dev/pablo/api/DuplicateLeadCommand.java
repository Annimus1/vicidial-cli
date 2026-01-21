package dev.pablo.api;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.lang.InterruptedException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Help.Ansi; 

/**
 * Command to duplicate an existing lead and place the new lead into a specified list.
 *
 * <p>This command looks up a lead by its ID, copies its details and creates a new lead
 * inside the provided destination list. Optionally the agent can override comments
 * and/or the email address for the duplicated lead.</p>
 *
 * <p>Usage:
 * <pre>
 *   duplicateInList &lt;leadId&gt; &lt;listId&gt; [-c|--comments &lt;notes&gt;] [-e|--email &lt;email&gt;]
 *   Example: duplicateInList 12345 10 -c "Transfer to new list" -e new@example.com
 * </pre>
 * </p>
 *
 * <p>Exit codes:
 * <ul>
 *   <li>0 — Lead duplicated successfully.</li>
 *   <li>1 — API/network error or interruption.</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
@Command(
    name = "duplicateInList",
    description = {
        "Creates a new Lead based on an existing one and places it in a specific list.",
        "Usage: vicidial-cli duplicateInList <leadId> <listId> [-c|--comments <notes>] [-e|--email <email>]",
        "Example: vicidial-cli duplicateInList 12345 10 -c \"Transfer to new list\" -e new@example.com"
    },
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

    /**
     * Constructs the command and obtains the shared Vicidial client instance.
     */
    public DuplicateLeadCommand() {
        this.client = VicidialClientSingleton.getInstance();
    }

    /**
     * Executes the duplicate lead flow.
     *
     * @return 0 on success, 1 on error (API/network or interruption)
     * @throws IOException If an I/O error occurs during the API call.
     * @throws InterruptedException If the operation is interrupted.
     */
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
