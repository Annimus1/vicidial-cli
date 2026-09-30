package dev.pablo.api.did;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import dev.pablo.api.VicidialClientSingleton;
import dev.pablo.models.DidModel;
import dev.pablo.models.HtmlParser;
import picocli.CommandLine.Help.Ansi;

/**
 * Reads the DIDs configured on a Vicidial instance.
 *
 * <p>
 * Every {@code did} subcommand needs the same set of DIDs, so the fetch and parse
 * step lives here instead of being repeated in each subcommand.
 * </p>
 */
public class DidRepository {

    /**
     * Vicidial ADD code for the admin page that lists the DIDs.
     */
    private static final String DIDS_ADD_CODE = "1300";

    /**
     * DID id that Vicidial seeds on a fresh install. Removing it would leave the
     * instance without a DID, so it is always protected.
     */
    public static final int PROTECTED_DID_ID = 1;

    private final VicidialClientSingleton client;

    public DidRepository() {
        this(VicidialClientSingleton.getInstance());
    }

    public DidRepository(VicidialClientSingleton client) {
        this.client = client;
    }

    /**
     * Removes a DID by its Vicidial id.
     *
     * @param id the Vicidial id of the DID
     * @throws IOException          if the admin page rejects the removal
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void remove(int id) throws IOException, InterruptedException {
        client.removeDID(id);
    }

    /**
     * Fetches and parses every DID stored on the instance.
     *
     * @return the parsed DIDs, never null
     * @throws IOException          if the admin page cannot be read
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public List<DidModel> fetchAll() throws IOException, InterruptedException {
        String html = client.getFromWeb(client.adminPageUrl(DIDS_ADD_CODE));
        return HtmlParser.ParseDIDs(html);
    }

    /**
     * Validates a DID number in the format the instance expects.
     *
     * <p>
     * Kept as-is: a leading {@code 1} followed by ten digits. This is
     * US-centric and tracked separately.
     * </p>
     *
     * @param did the number to validate
     * @return true when the number is well formed
     */
    public static boolean isValidFormat(String did) {
        if (did == null || did.isBlank()) {
            return false;
        }

        String trimmed = did.trim();

        return trimmed.startsWith("1") && trimmed.length() == 11 && isAsciiDigits(trimmed);
    }

    /**
     * Describes why a DID number was rejected, for use in error messages.
     *
     * @param did the number that failed validation
     * @return a human readable explanation
     */
    public static String formatError(String did) {
        if (did == null || did.isBlank()) {
            return "A DID number is required.";
        }

        String trimmed = did.trim();

        if (!trimmed.startsWith("1")) {
            return "Invalid DID '" + trimmed + "': it must start with '1'.";
        }

        if (trimmed.length() != 11) {
            return "Invalid DID '" + trimmed + "': it must be 11 digits long, found " + trimmed.length() + ".";
        }

        if (!isAsciiDigits(trimmed)) {
            return "Invalid DID '" + trimmed + "': it must contain digits only.";
        }

        return "Invalid DID '" + trimmed + "'.";
    }

    /**
     * Checks for {@code 0-9} only.
     *
     * <p>
     * {@link Character#isDigit(int)} also accepts other Unicode digit forms,
     * which Vicidial would not accept as a DID.
     * </p>
     *
     * @param value the value to check
     * @return true when every character is an ASCII digit
     */
    private static boolean isAsciiDigits(String value) {
        return value.chars().allMatch(c -> c >= '0' && c <= '9');
    }

    /**
     * Finds a DID by its caller id.
     *
     * <p>
     * The comparison is case-insensitive and trimmed, and tolerates DIDs whose
     * caller id was not parsed.
     * </p>
     *
     * @param dids      the DIDs to search
     * @param callerId  the number to look for
     * @return the match, or null when the DID is not on the instance
     */
    public static DidModel findByCallerId(List<DidModel> dids, String callerId) {
        if (dids == null || callerId == null || callerId.isBlank()) {
            return null;
        }

        String wanted = callerId.trim();

        return dids.stream()
                .filter(d -> d.getCallerId() != null && d.getCallerId().trim().equalsIgnoreCase(wanted))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds every DID assigned to a group.
     *
     * @param dids  the DIDs to search
     * @param group the group to look for
     * @return the matches, possibly empty
     */
    public static List<DidModel> findByGroup(List<DidModel> dids, String group) {
        if (dids == null || group == null || group.isBlank()) {
            return List.of();
        }

        String wanted = group.trim();

        return dids.stream()
                .filter(d -> d.getGroup() != null && d.getGroup().trim().equalsIgnoreCase(wanted))
                .toList();
    }

    /**
     * Normalises the value used for the active column, which the parser stores as
     * a char and leaves unset when the column is missing.
     *
     * @param did the DID to inspect
     * @return true when the DID is active
     */
    public static boolean isActive(DidModel did) {
        return did != null && Character.toUpperCase(did.getActive()) == 'Y';
    }

    /**
     * Renders the active column for display.
     *
     * @param did the DID to inspect
     * @return a coloured check or cross marker
     */
    public static Ansi.Text activeMarker(DidModel did) {
        return isActive(did)
                ? Ansi.AUTO.text("✅")
                : Ansi.AUTO.text("❌");
    }

    /**
     * Returns a printable value, falling back to a dash when it is absent.
     *
     * @param value the value to render
     * @return the value, or "-" when null or blank
     */
    public static String orDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value.trim();
    }

    /**
     * Uppercases a caller id for the header, tolerating an unset value.
     *
     * @param did the DID to inspect
     * @return the caller id, uppercased, or "-" when absent
     */
    public static String callerIdOrDash(DidModel did) {
        String callerId = (did == null) ? null : did.getCallerId();
        return (callerId == null || callerId.isBlank()) ? "-" : callerId.trim().toUpperCase(Locale.ROOT);
    }
}
