package dev.pablo.api.did;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import dev.pablo.models.DidModel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Removes DIDs from a Vicidial instance.
 *
 * <p>
 * Replaces the {@code deleteDIDs -m SINGLE|MULTIPLE|GROUP} modes. The three
 * selectors are mutually exclusive options instead of a mode enum, so the command
 * reads as what it does rather than as a mode name.
 * </p>
 */
@Command(name = "remove", description = {
    "Remove DIDs from the instance.",
    "",
    "Select the DIDs to remove with exactly one of:",
    "  --did <number>     a single DID",
    "  --file <path>      one DID per line",
    "  --group <group>    every DID assigned to a user group",
    "",
    "A bulk removal asks for confirmation first. Use --dry-run to preview and",
    "--force to skip the prompt.",
    "",
    "Examples:",
    "  vicidial-cli did remove --did 15551234567",
    "  vicidial-cli did remove --file /path/to/dids.txt",
    "  vicidial-cli did remove --group SALES_TEAM --dry-run",
    "  vicidial-cli did remove --group SALES_TEAM --force"
}, mixinStandardHelpOptions = true)
public class DidRemoveCommand implements Callable<Integer> {

    @Option(names = { "-d",
        "--did" }, description = "A single DID to remove. Must start with '1' and be 11 digits.", defaultValue = "")
    private String did;

    @Option(names = { "-f",
        "--file" }, description = "Path to a file with one DID per line.", defaultValue = "")
    private String file;

    @Option(names = { "-g",
        "--group" }, description = "Remove every DID assigned to this user group.", defaultValue = "")
    private String group;

    @Option(names = { "--dry-run" }, description = "Show what would be removed without changing anything.", defaultValue = "false")
    private boolean dryRun;

    @Option(names = { "--force" }, description = "Remove without asking for confirmation.", defaultValue = "false")
    private boolean force;

    private final DidRepository repository;

    public DidRemoveCommand() {
        this(new DidRepository());
    }

    DidRemoveCommand(DidRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() throws Exception {
        int selectors = 0;

        if (!did.isBlank()) {
            selectors++;
        }
        if (!file.isBlank()) {
            selectors++;
        }
        if (!group.isBlank()) {
            selectors++;
        }

        if (selectors == 0) {
            System.err.println(Ansi.AUTO.text("❌ @|red Provide one of --did, --file or --group. |@"));
            return 1;
        }

        if (selectors > 1) {
            System.err.println(Ansi.AUTO.text("❌ @|red --did, --file and --group cannot be combined. |@"));
            return 1;
        }

        List<DidModel> dids = repository.fetchAll();
        List<DidModel> targets;

        try {
            targets = resolveTargets(dids);
        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + e.getMessage() + " |@"));
            return 1;
        }

        if (targets.isEmpty()) {
            System.err.println(Ansi.AUTO.text("❌ @|red No matching DIDs to remove. |@"));
            return 1;
        }

        warnAboutProtected(targets);

        DidTable.print(targets, dryRun ? "Would remove" : "Removing");

        if (dryRun) {
            System.out.println(Ansi.AUTO.text("@|yellow Dry run, nothing was removed. |@"));
            return 0;
        }

        if (targets.size() > 1 && !force && !confirm(targets)) {
            System.out.println(Ansi.AUTO.text("@|yellow Cancelled, nothing was removed. |@"));
            return 1;
        }

        return removeAll(targets);
    }

    /**
     * Warns when the selection includes the seeded default DID.
     *
     * <p>
     * Reported before the preview so a group removal that sweeps up the default
     * does not hide it in the middle of the table.
     * </p>
     *
     * @param targets the DIDs selected for removal
     */
    private static void warnAboutProtected(List<DidModel> targets) {
        boolean included = targets.stream().anyMatch(did -> did.getId() == DidRepository.PROTECTED_DID_ID);

        if (included) {
            System.out.println(Ansi.AUTO.text("🛡️ @|yellow ID " + DidRepository.PROTECTED_DID_ID
                    + " is the default DID and will be skipped. |@"));
        }
    }

    /**
     * Resolves the selected option into the DIDs it matches.
     *
     * @param dids every DID on the instance
     * @return the DIDs to remove, empty when the selector matched nothing
     * @throws IOException if the file named by {@code --file} cannot be read
     */
    private List<DidModel> resolveTargets(List<DidModel> dids) throws IOException {
        if (!did.isBlank()) {
            DidModel match = findSingle(did, dids);

            return match == null ? List.of() : List.of(match);
        }

        if (!group.isBlank()) {
            List<DidModel> matches = DidRepository.findByGroup(dids, group);
            System.out.println(Ansi.AUTO.text("@|blue " + matches.size() + " DID(s) assigned to group '" + group.trim()
                    + "'. |@"));
            return matches;
        }

        return findFromFile(dids, Paths.get(file.trim()));
    }

    /**
     * Looks up a single DID, reporting why it cannot be used.
     *
     * @param callerId the number requested by the user
     * @param dids     every DID on the instance
     * @return the matching DID, or null when it is not present
     */
    private DidModel findSingle(String callerId, List<DidModel> dids) {
        if (!DidRepository.isValidFormat(callerId)) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + DidRepository.formatError(callerId) + " |@"));
            return null;
        }

        DidModel match = DidRepository.findByCallerId(dids, callerId);

        if (match == null) {
            System.err.println(Ansi.AUTO.text("❌ @|red DID " + callerId.trim() + " is not on the instance. |@"));
        }

        return match;
    }

    /**
     * Reads DIDs from a file and resolves the ones that exist on the instance.
     *
     * <p>
     * Invalid lines are reported and skipped so one bad entry does not abort a
     * bulk removal.
     * </p>
     *
     * @param dids  every DID on the instance
     * @param path  the file to read
     * @return the DIDs to remove
     * @throws IOException if the file cannot be read
     */
    private List<DidModel> findFromFile(List<DidModel> dids, Path path) throws IOException {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IOException("Invalid path: " + path.toAbsolutePath());
        }

        List<DidModel> matches = new ArrayList<>();

        for (String line : Files.readAllLines(path)) {
            String candidate = line.trim();

            if (candidate.isEmpty() || candidate.startsWith("#")) {
                continue;
            }

            if (!DidRepository.isValidFormat(candidate)) {
                System.err.println(Ansi.AUTO.text("⚠️ @|yellow Skipped invalid DID: " + candidate + " |@"));
                continue;
            }

            DidModel match = DidRepository.findByCallerId(dids, candidate);

            if (match == null) {
                System.err.println(Ansi.AUTO
                        .text("⚠️ @|yellow Skipped, not on the instance: " + candidate + " |@"));
            } else {
                matches.add(match);
            }
        }

        return matches;
    }

    /**
     * Asks the user to confirm a bulk removal.
     *
     * <p>
     * A non-interactive stdin is treated as a decline, so a script cannot remove a
     * group of DIDs by accident.
     * </p>
     *
     * @param targets the DIDs that would be removed
     * @return true when the user confirmed
     */
    private boolean confirm(List<DidModel> targets) {
        Set<String> numbers = new LinkedHashSet<>();

        for (DidModel did : targets) {
            numbers.add(DidRepository.callerIdOrDash(did));
        }

        System.out.print(Ansi.AUTO.string("@|yellow Remove " + targets.size() + " DID(s) ["
                + String.join(", ", numbers) + "]? [y/N] |@"));
        System.out.flush();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String answer = reader.readLine();

            if (answer == null) {
                System.out.println();
                return false;
            }

            String normalized = answer.trim().toLowerCase();

            return normalized.equals("y") || normalized.equals("yes");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Removes each DID, reporting failures without stopping the run.
     *
     * @param targets the DIDs to remove
     * @return 0 when every removal succeeded, 1 when any failed
     */
    private int removeAll(List<DidModel> targets) {
        int failed = 0;

        for (DidModel did : targets) {
            if (removeOne(did)) {
                failed++;
            }
        }

        System.out.println();

        if (failed == 0) {
            System.out.println(Ansi.AUTO.text("@|green ✅ Removed " + targets.size() + " DID(s). |@"));
            return 0;
        }

        System.err.println(Ansi.AUTO.text("❌ @|red " + failed + " of " + targets.size() + " DID(s) could not be removed. |@"));
        return 1;
    }

    /**
     * Removes a single DID, protecting the seeded default.
     *
     * @param did the DID to remove
     * @return true when the removal failed
     */
    private boolean removeOne(DidModel did) {
        if (did.getId() == DidRepository.PROTECTED_DID_ID) {
            System.out.println(Ansi.AUTO.text("@|red Skipped ID " + did.getId()
                    + ", the default DID cannot be removed. |@"));
            return false;
        }

        try {
            repository.remove(did.getId());
            System.out.println(Ansi.AUTO.text("@|green ✅ ID: " + did.getId() + " DID: "
                    + DidRepository.callerIdOrDash(did) + " removed. |@"));
            return false;
        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red I/O error removing DID ID: " + did.getId() + " |@"));
            return true;
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red Removal interrupted for DID ID: " + did.getId() + " |@"));
            Thread.currentThread().interrupt();
            return true;
        }
    }
}
