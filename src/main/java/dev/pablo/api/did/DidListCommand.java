package dev.pablo.api.did;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import dev.pablo.models.DidModel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Lists the DIDs configured on a Vicidial instance.
 *
 * <p>
 * Subcommand of {@code did}. Output is a table; machine-readable output is
 * tracked separately.
 * </p>
 */
@Command(name = "list", description = {
    "List the DIDs stored on the instance.",
    "",
    "Examples:",
    "  vicidial-cli did list",
    "  vicidial-cli did list --active",
    "  vicidial-cli did list --group SALES_TEAM",
    "  vicidial-cli did list --carrier VOIP"
}, mixinStandardHelpOptions = true)
public class DidListCommand implements Callable<Integer> {

    @Option(names = { "-a",
        "--active" }, description = "Only show DIDs that are active.", defaultValue = "false")
    private boolean onlyActive;

    @Option(names = { "-i",
        "--inactive" }, description = "Only show DIDs that are not active.", defaultValue = "false")
    private boolean onlyInactive;

    @Option(names = { "-g",
        "--group" }, description = "Only show DIDs assigned to this user group.", defaultValue = "")
    private String group;

    @Option(names = { "-c",
        "--carrier" }, description = "Only show DIDs with this carrier.", defaultValue = "")
    private String carrier;

    @Parameters(description = "Optional DID numbers to show. When provided, the other filters are ignored.")
    private List<String> callerIds = new ArrayList<>();

    private final DidRepository repository;

    public DidListCommand() {
        this(new DidRepository());
    }

    DidListCommand(DidRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() throws Exception {
        if (onlyActive && onlyInactive) {
            System.err.println(Ansi.AUTO.text("❌ @|red --active and --inactive cannot be combined. |@"));
            return 1;
        }

        List<DidModel> dids = repository.fetchAll();
        List<DidModel> selected = select(dids);

        DidTable.print(selected, "DIDs found");
        return 0;
    }

    /**
     * Applies the filters chosen on the command line.
     *
     * <p>
     * Passing DID numbers positionally overrides the filters, since the user has
     * already named exactly what they want to see.
     * </p>
     *
     * @param dids every DID on the instance
     * @return the DIDs to display
     */
    private List<DidModel> select(List<DidModel> dids) {
        if (!callerIds.isEmpty()) {
            List<DidModel> selected = new ArrayList<>();
            List<String> missing = new ArrayList<>();

            for (String callerId : callerIds) {
                DidModel match = DidRepository.findByCallerId(dids, callerId);

                if (match == null) {
                    missing.add(callerId);
                } else {
                    selected.add(match);
                }
            }

            for (String callerId : missing) {
                System.err.println(Ansi.AUTO.text("⚠️ @|yellow DID not found on the instance: " + callerId + " |@"));
            }

            return selected;
        }

        return dids.stream()
                .filter(did -> !onlyActive || DidRepository.isActive(did))
                .filter(did -> !onlyInactive || !DidRepository.isActive(did))
                .filter(did -> group.isBlank()
                        || (did.getGroup() != null && did.getGroup().trim().equalsIgnoreCase(group.trim())))
                .filter(did -> carrier.isBlank()
                        || (did.getCarrier() != null && did.getCarrier().trim().equalsIgnoreCase(carrier.trim())))
                .toList();
    }
}
