package dev.pablo.api.did;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

import dev.pablo.api.VicidialClientSingleton;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Adds DIDs to an inbound group and to a CID group.
 *
 * <p>
 * Subcommand of {@code did}. Each DID follows the same flow: it is created in
 * {@code vicidial_inbound_dids} and routed to the inbound group first, then an
 * entry is added to {@code vicidial_campaign_cid_areacodes} with the leading
 * {@code 1} removed from the outbound caller id.
 * </p>
 */
@Command(name = "add", description = {
    "Add DIDs to an inbound group and a CID group.",
    "",
    "Each DID is added to the inbound group first, then to the CID group with",
    "the leading 1 removed from the outbound caller id.",
    "",
    "Select DIDs with exactly one of:",
    "  --did <number>     a single DID in US format (11 digits, starts with 1)",
    "  --file <path>      one DID per line",
    "",
    "The CID entry uses areacode=NONE by default. Use --areacode, --state or",
    "--all to override it.",
    "",
    "Examples:",
    "  vicidial-cli did add --did 11231234321 --inbound SALES --cid SALESCID",
    "  vicidial-cli did add --file /path/to/dids.txt --inbound SALES --cid SALESCID",
    "  vicidial-cli did add --did 11231234321 --inbound SALES --cid SALESCID --state FL",
    "  vicidial-cli did add --did 11231234321 --inbound SALES --cid SALESCID --all"
}, mixinStandardHelpOptions = true)
public class DidAddCommand implements Callable<Integer> {

    @Option(names = { "-d",
        "--did" }, description = "A single DID to add. Must start with '1' and be 11 digits.", defaultValue = "")
    private String did;

    @Option(names = { "-f",
        "--file" }, description = "Path to a file with one DID per line.", defaultValue = "")
    private String file;

    @Option(names = { "-i",
        "--inbound" }, description = "Inbound group that should receive calls for the DIDs.", defaultValue = "")
    private String inboundGroup;

    @Option(names = { "-c",
        "--cid" }, description = "CID group (or campaign id) that should receive the outbound caller ids.",
        defaultValue = "")
    private String cidGroup;

    @Option(names = { "--description" }, description = "Optional description, 6 to 50 characters.",
        defaultValue = "")
    private String description;

    @Option(names = { "--areacode" }, description = "CID group areacode, a state code, NONE or ---ALL---.",
        defaultValue = "")
    private String areacode;

    @Option(names = { "--state" }, description = "State code to use as the CID group areacode, e.g. FL.",
        defaultValue = "")
    private String state;

    @Option(names = { "--all" }, description = "Use ---ALL--- as the CID group areacode.", defaultValue = "false")
    private boolean allAreacodes;

    @Option(names = { "--dry-run" }, description = "Show what would be added without changing anything.",
        defaultValue = "false")
    private boolean dryRun;

    private final VicidialClientSingleton client;

    public DidAddCommand() {
        this(VicidialClientSingleton.getInstance());
    }

    DidAddCommand(VicidialClientSingleton client) {
        this.client = client;
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

        if (selectors == 0) {
            System.err.println(Ansi.AUTO.text("❌ @|red Provide one of --did or --file. |@"));
            return 1;
        }

        if (selectors > 1) {
            System.err.println(Ansi.AUTO.text("❌ @|red --did and --file cannot be combined. |@"));
            return 1;
        }

        if (inboundGroup.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red --inbound is required. |@"));
            return 1;
        }

        if (cidGroup.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red --cid is required. |@"));
            return 1;
        }

        if (!description.isBlank() && (description.trim().length() < 6 || description.trim().length() > 50)) {
            System.err.println(Ansi.AUTO.text("❌ @|red --description must be between 6 and 50 characters. |@"));
            return 1;
        }

        String resolvedAreacode;

        try {
            resolvedAreacode = DidRepository.resolveAreacode(areacode, state, allAreacodes);
        } catch (IllegalArgumentException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + e.getMessage() + " |@"));
            return 1;
        }

        List<String> dids;

        try {
            dids = resolveDids();
        } catch (IllegalArgumentException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + e.getMessage() + " |@"));
            return 1;
        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + e.getMessage() + " |@"));
            return 1;
        }

        if (dids.isEmpty()) {
            System.err.println(Ansi.AUTO.text("❌ @|red No valid DIDs to add. |@"));
            return 1;
        }

        System.out.println(Ansi.AUTO.text("@|blue Adding " + dids.size() + " DID(s) to inbound group '"
                + inboundGroup.trim() + "' and CID group '" + cidGroup.trim() + "' (areacode="
                + resolvedAreacode + "). |@"));

        if (dryRun) {
            printPlan(dids, resolvedAreacode);
            System.out.println(Ansi.AUTO.text("@|yellow Dry run, nothing was added. |@"));
            return 0;
        }

        int failed = 0;

        for (String number : dids) {
            if (!addOne(number, resolvedAreacode)) {
                failed++;
            }
        }

        System.out.println();

        if (failed == 0) {
            System.out.println(Ansi.AUTO.text("@|green ✅ Added " + dids.size() + " DID(s). |@"));
            return 0;
        }

        System.err.println(Ansi.AUTO.text("❌ @|red " + failed + " of " + dids.size()
                + " DID(s) could not be fully added. |@"));
        return 1;
    }

    /**
     * Resolves the DID selector into the numbers to add.
     *
     * @return the DIDs to add, empty when the selector matched nothing
     * @throws IOException if the file named by {@code --file} cannot be read
     */
    private List<String> resolveDids() throws IOException {
        if (!did.isBlank()) {
            String candidate = did.trim();

            if (!DidRepository.isValidFormat(candidate)) {
                throw new IllegalArgumentException(DidRepository.formatError(candidate));
            }

            return List.of(candidate);
        }

        return readFile(Paths.get(file.trim()));
    }

    /**
     * Reads and validates the DIDs listed in a file.
     *
     * <p>
     * Invalid lines are reported and skipped so one bad entry does not abort a
     * bulk add.
     * </p>
     *
     * @param path the file to read
     * @return the unique, well-formed DIDs
     * @throws IOException if the file cannot be read
     */
    private List<String> readFile(Path path) throws IOException {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IOException("Invalid path: " + path.toAbsolutePath());
        }

        Set<String> unique = new LinkedHashSet<>();

        for (String line : Files.readAllLines(path)) {
            String candidate = line.trim();

            if (candidate.isEmpty() || candidate.startsWith("#")) {
                continue;
            }

            if (!DidRepository.isValidFormat(candidate)) {
                System.err.println(Ansi.AUTO.text("⚠️ @|yellow Skipped invalid DID: " + candidate + " |@"));
                continue;
            }

            unique.add(candidate);
        }

        return new ArrayList<>(unique);
    }

    /**
     * Prints what would be sent without calling the API.
     *
     * @param dids      the DIDs that would be added
     * @param areacode  the resolved areacode
     */
    private void printPlan(List<String> dids, String areacode) {
        for (String number : dids) {
            System.out.println("  " + number + " -> inbound " + inboundGroup.trim()
                    + " | CID " + cidGroup.trim() + " outbound_cid=" + DidRepository.cidNumber(number)
                    + " areacode=" + areacode);
        }
    }

    /**
     * Adds one DID to the inbound group and then to the CID group.
     *
     * @param number    the inbound DID number
     * @param areacode  the resolved CID group areacode
     * @return true when both steps succeeded
     */
    private boolean addOne(String number, String areacode) {
        try {
            String inboundResponse = client.addDidToInboundGroup(number, inboundGroup.trim(), description.trim());

            if (!isSuccess(inboundResponse)) {
                System.err.println(Ansi.AUTO.text("❌ @|red DID " + number
                        + " was not added to inbound group '" + inboundGroup.trim() + "': "
                        + responseMessage(inboundResponse) + " |@"));
                return false;
            }

            System.out.println(Ansi.AUTO.text("@|green ✅|@ inbound " + number + " -> " + inboundGroup.trim()));

            String outboundCid = DidRepository.cidNumber(number);
            String cidResponse = client.addCidGroupEntry(cidGroup.trim(), areacode, outboundCid,
                    description.trim(), "Y");

            if (!isSuccess(cidResponse)) {
                System.err.println(Ansi.AUTO.text("⚠️ @|yellow DID " + number
                        + " was added to the inbound group, but the CID entry failed: "
                        + responseMessage(cidResponse) + " |@"));
                return false;
            }

            System.out.println(Ansi.AUTO.text("@|green ✅|@ CID " + outboundCid + " -> " + cidGroup.trim()
                    + " (areacode=" + areacode + ")"));
            return true;

        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red I/O error adding DID " + number + ": "
                    + e.getMessage() + " |@"));
            return false;
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red Add interrupted for DID " + number + " |@"));
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean isSuccess(String response) {
        return response != null && response.trim().toUpperCase(Locale.ROOT).startsWith("SUCCESS");
    }

    private static String responseMessage(String response) {
        if (response == null || response.isBlank()) {
            return "empty response";
        }

        for (String line : response.split("\\R")) {
            if (!line.isBlank()) {
                return line.trim();
            }
        }

        return response.trim();
    }
}
