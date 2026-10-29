package dev.pablo.api.did;

import java.util.List;
import java.util.concurrent.Callable;

import dev.pablo.models.DidModel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Reports whether a DID exists on the instance and what it is assigned to.
 *
 * <p>
 * Read-only. Exits {@code 1} when the DID is not on the instance, so it can be
 * used as a condition in a shell script.
 * </p>
 */
@Command(name = "verify", description = {
    "Check that a DID exists on the instance and show what it is assigned to.",
    "",
    "Exits 0 when the DID is present, 1 when it is not.",
    "",
    "Examples:",
    "  vicidial-cli did verify 15551234567",
    "  vicidial-cli did verify 15551234567 --verbose"
}, mixinStandardHelpOptions = true)
public class DidVerifyCommand implements Callable<Integer> {

    @Parameters(description = "The DID number to look for. Must start with '1' and be 11 digits.")
    private String did;

    @Option(names = { "-v",
        "--verbose" }, description = "Show every field parsed for the DID.", defaultValue = "false")
    private boolean verbose;

    private final DidRepository repository;

    public DidVerifyCommand() {
        this(new DidRepository());
    }

    DidVerifyCommand(DidRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() throws Exception {
        if (!DidRepository.isValidFormat(did)) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + DidRepository.formatError(did) + " |@"));
            return 1;
        }

        List<DidModel> dids = repository.fetchAll();
        DidModel match = DidRepository.findByCallerId(dids, did);

        if (match == null) {
            System.err.println(Ansi.AUTO.text("❌ @|red DID " + did.trim() + " is not on the instance. |@"));
            return 1;
        }

        printMatch(match);
        return 0;
    }

    /**
     * Prints what the DID is assigned to, so the caller can tell whether it is
     * routed the way they expect.
     *
     * @param did the DID that was found
     */
    private void printMatch(DidModel did) {
        DidTable.printHeader("DID " + DidRepository.callerIdOrDash(did) + " found");
        DidTable.printField("id", String.valueOf(did.getId()));
        DidTable.printField("active", DidRepository.activeMarker(did) + " " + describeActive(did));
        DidTable.printField("group", DidRepository.orDash(did.getGroup()));
        DidTable.printField("description", DidRepository.orDash(did.getDescription()));
        DidTable.printField("carrier", DidRepository.orDash(did.getCarrier()));
        DidTable.printField("route", DidRepository.orDash(did.getRoute()));

        if (verbose) {
            DidTable.printField("recording", DidRepository.orDash(did.getRec()));
            DidTable.printField("modified", DidRepository.orDash(did.getModify()));
        }

        System.out.println();
    }

    /**
     * Describes the active flag in words, since the column stores a single char.
     *
     * @param did the DID to inspect
     * @return "active" or "inactive"
     */
    private static String describeActive(DidModel did) {
        return DidRepository.isActive(did) ? "active" : "inactive";
    }
}
