package dev.pablo.api.list;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Resets the called-since-last-reset flag for every lead in a list.
 *
 * <p>
 * Subcommand of {@code list}. Calls {@code update_list} through the Non-Agent
 * API with {@code reset_list=Y}.
 * </p>
 */
@Command(name = "reset", description = {
    "Reset the called-since-last-reset flag for every lead in a list.",
    "",
    "Examples:",
    "  vicidial-cli list reset --id 6004",
    "  vicidial-cli list reset -id 6004"
}, mixinStandardHelpOptions = true)
public class ListResetCommand implements Callable<Integer> {

    @Option(names = { "-id",
        "--list_id", "--id" }, description = "List identifier, 2-14 digits.", defaultValue = "")
    private String listId;

    private final ListRepository repository;

    public ListResetCommand() {
        this(new ListRepository());
    }

    ListResetCommand(ListRepository repository) {
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
            String response = repository.reset(id);

            if (!ListRepository.isSuccess(response)) {
                System.err.println(Ansi.AUTO.text("❌ @|red List " + id + " was not reset: "
                        + ListRepository.responseMessage(response) + " |@"));
                return 1;
            }

            System.out.println(Ansi.AUTO.text("@|green ✅ Reset the called-since-last-reset flag for list "
                    + id + ". |@"));
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
