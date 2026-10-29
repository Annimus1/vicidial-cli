package dev.pablo.api.did;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import dev.pablo.models.DidModel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Adds DIDs to a Vicidial instance.
 *
 * <p>
 * NOT IMPLEMENTED. The CLI surface below is the agreed contract; the admin form
 * that creates a DID has not been mapped yet, so calling this reports that the
 * command is unavailable rather than pretending to work.
 * </p>
 *
 * <p>
 * Intended behaviour once implemented:
 * </p>
 * <ol>
 * <li>Validate the number with the same format rule used by {@code verify} and
 * {@code remove}.</li>
 * <li>Reject a number already on the instance, reported by {@code did verify}.</li>
 * <li>Submit the admin add-DID form, one call per DID.</li>
 * <li>Read the instance back and confirm each DID was stored, since a success
 * response alone does not prove it.</li>
 * <li>Report a per-DID summary and exit {@code 1} if any DID failed.</li>
 * </ol>
 *
 * <p>
 * The open questions are the admin form field names and ADD codes, which the
 * Vicidial API does not expose.
 * </p>
 */
@Command(name = "add", description = {
    "Add DIDs to the instance. (not implemented yet)",
    "",
    "The CLI contract below is settled, but the Vicidial admin form that creates",
    "a DID has not been mapped yet, so this subcommand reports that it is",
    "unavailable instead of running.",
    "",
    "Intended options:",
    "  <number>...       one or more DID numbers, each starting with '1' and 11 digits",
    "  -g, --group       user group to assign the DIDs to",
    "  -c, --carrier     carrier to record on the DIDs",
    "  -d, --description description to record on the DIDs",
    "",
    "Example:",
    "  vicidial-cli did add 15551234567 --group SALES_TEAM"
}, mixinStandardHelpOptions = true)
public class DidAddCommand implements Callable<Integer> {

    @Parameters(description = "DID numbers to add. Each must start with '1' and be 11 digits.")
    private List<String> dids;

    @Option(names = { "-g",
        "--group" }, description = "User group to assign the DIDs to.", defaultValue = "")
    private String group;

    @Option(names = { "-c",
        "--carrier" }, description = "Carrier to record on the DIDs.", defaultValue = "")
    private String carrier;

    @Option(names = { "-d",
        "--description" }, description = "Description to record on the DIDs.", defaultValue = "")
    private String description;

    @Override
    public Integer call() {
        System.err.println(Ansi.AUTO.text("❌ @|red did add is not implemented yet. |@"));
        System.err.println(Ansi.AUTO.text("🧐 @|yellow Available subcommands: list, verify, remove. |@"));

        if (dids != null && !dids.isEmpty()) {
            System.err.println(Ansi.AUTO.text("   @|yellow would have added: " + String.join(", ", dids) + " |@"));
        }

        if (!group.isBlank()) {
            System.err.println(Ansi.AUTO.text("   @|yellow would have used group: " + group.trim() + " |@"));
        }

        if (!carrier.isBlank()) {
            System.err.println(Ansi.AUTO.text("   @|yellow would have used carrier: " + carrier.trim() + " |@"));
        }

        if (!description.isBlank()) {
            System.err.println(Ansi.AUTO.text("   @|yellow would have used description: " + description.trim() + " |@"));
        }

        return 1;
    }

    /**
     * Validates the requested numbers against the same rule used by the other
     * subcommands.
     *
     * <p>
     * Kept next to the stub so the contract is executable as soon as the admin
     * form is mapped, and so a caller can rely on one format rule across the whole
     * command.
     * </p>
     *
     * @return the numbers that are well formed
     */
    List<String> validateFormat() {
        return dids == null
                ? List.of()
                : dids.stream().filter(DidRepository::isValidFormat).toList();
    }

    /**
     * Reports which of the requested numbers are already on the instance.
     *
     * <p>
     * Defined but not called: it needs the instance lookup, which is only worth
     * doing once a DID can actually be created.
     * </p>
     *
     * @param existing the DIDs currently on the instance
     * @return the requested numbers that already exist
     * @throws IOException          if the instance cannot be read
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    List<DidModel> findExisting(List<DidModel> existing) throws IOException, InterruptedException {
        if (dids == null) {
            return List.of();
        }

        return dids.stream()
                .map(callerId -> DidRepository.findByCallerId(existing, callerId))
                .filter(did -> did != null)
                .toList();
    }
}
