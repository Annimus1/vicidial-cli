package dev.pablo.api.list;

import java.util.List;
import java.util.concurrent.Callable;

import dev.pablo.models.ListModel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Lists the lists configured on a Vicidial instance.
 *
 * <p>
 * Subcommand of {@code list}. Output is a table; machine-readable output is
 * tracked separately.
 * </p>
 */
@Command(name = "list", description = {
    "List the lists stored on the instance.",
    "",
    "Examples:",
    "  vicidial-cli list list",
    "  vicidial-cli list list --activeOnly",
    "  vicidial-cli list list --campaign SALES"
}, mixinStandardHelpOptions = true)
public class ListListCommand implements Callable<Integer> {

    @Option(names = { "-a",
        "--activeOnly" }, description = "Only show lists that are active.", defaultValue = "false")
    private boolean onlyActive;

    @Option(names = { "-i",
        "--inactiveOnly" }, description = "Only show lists that are not active.", defaultValue = "false")
    private boolean onlyInactive;

    @Option(names = { "-c",
        "--campaign" }, description = "Only show lists assigned to this campaign.", defaultValue = "")
    private String campaign;

    private final ListRepository repository;

    public ListListCommand() {
        this(new ListRepository());
    }

    ListListCommand(ListRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() throws Exception {
        if (onlyActive && onlyInactive) {
            System.err.println(Ansi.AUTO.text("❌ @|red --activeOnly and --inactiveOnly cannot be combined. |@"));
            return 1;
        }

        List<ListModel> lists = repository.fetchAll();
        List<ListModel> selected = lists.stream()
                .filter(list -> !onlyActive || ListRepository.isActive(list))
                .filter(list -> !onlyInactive || !ListRepository.isActive(list))
                .filter(list -> campaign.isBlank()
                        || (list.getCampaign() != null
                                && list.getCampaign().trim().equalsIgnoreCase(campaign.trim())))
                .toList();

        ListTable.print(selected, "Lists found");
        return 0;
    }
}
