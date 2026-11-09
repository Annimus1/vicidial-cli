package dev.pablo.api.list;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Sets a list to inactive.
 *
 * <p>
 * Subcommand of {@code list}. Calls {@code update_list} through the Non-Agent
 * API.
 * </p>
 */
@Command(name = "pause", description = {
    "Change the status of a list to PAUSED.",
    "",
    "Examples:",
    "  vicidial-cli list pause --id 6004",
    "  vicidial-cli list pause -id 6004"
}, mixinStandardHelpOptions = true)
public class ListPauseCommand implements Callable<Integer> {

    @Option(names = { "-id",
        "--list_id", "--id" }, description = "List identifier, 2-14 digits.", defaultValue = "")
    private String listId;

    private final ListRepository repository;

    public ListPauseCommand() {
        this(new ListRepository());
    }

    ListPauseCommand(ListRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() {
        if (listId.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"List_Id\" is missing. |@ "));
            return 1;
        }

        String id = listId.trim();

        if (!ListRepository.isValidListId(id)) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + ListRepository.listIdError(id) + " |@"));
            return 1;
        }

        try {
            String response = repository.setActive(id, false);

            if (!ListRepository.isSuccess(response)) {
                System.err.println(Ansi.AUTO.text("❌ @|red List " + id + " was not paused: "
                        + ListRepository.responseMessage(response) + " |@"));
                return 1;
            }

            System.out.println(Ansi.AUTO.text("@|green ✅ List " + id + " is now paused. |@"));
            return 0;
        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red API or network error: |@" + e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red The request was interrupted. |@"));
            Thread.currentThread().interrupt();
            return 1;
        }
    }
}
