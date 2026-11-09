package dev.pablo.api.list;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Sets a list to active.
 *
 * <p>
 * Subcommand of {@code list}. Calls {@code update_list} through the Non-Agent
 * API.
 * </p>
 */
@Command(name = "active", description = {
    "Change the status of a list to ACTIVE.",
    "",
    "Examples:",
    "  vicidial-cli list active --id 6004",
    "  vicidial-cli list active -id 6004"
}, mixinStandardHelpOptions = true)
public class ListActiveCommand implements Callable<Integer> {

    @Option(names = { "-id",
        "--list_id", "--id" }, description = "List identifier, 2-14 digits.", defaultValue = "")
    private String listId;

    private final ListRepository repository;

    public ListActiveCommand() {
        this(new ListRepository());
    }

    ListActiveCommand(ListRepository repository) {
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
            String response = repository.setActive(id, true);

            if (!ListRepository.isSuccess(response)) {
                System.err.println(Ansi.AUTO.text("❌ @|red List " + id + " was not activated: "
                        + ListRepository.responseMessage(response) + " |@"));
                return 1;
            }

            System.out.println(Ansi.AUTO.text("@|green ✅ List " + id + " is now active. |@"));
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
