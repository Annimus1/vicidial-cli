package dev.pablo.api.did;

import java.util.List;

import dev.pablo.models.DidModel;
import picocli.CommandLine.Help.Ansi;

/**
 * Renders DIDs as an aligned text table.
 *
 * <p>
 * Table output only for now. Machine-readable output is tracked separately so the
 * column layout is defined once.
 * </p>
 */
final class DidTable {

    private static final String SEPARATOR = "-".repeat(96);

    private DidTable() {
    }

    /**
     * Prints a section header.
     *
     * @param title the text to print
     */
    static void printHeader(String title) {
        System.out.println(Ansi.AUTO.text("@|blue " + title + "|@"));
        System.out.println(SEPARATOR);
    }

    /**
     * Prints the column headers.
     */
    static void printColumnHeaders() {
        System.out.printf("%-6s %-14s %-7s %-16s %-28s %-14s%n", "ID", "DID", "ACTIVE", "GROUP", "DESCRIPTION", "CARRIER");
        System.out.println(SEPARATOR);
    }

    /**
     * Prints one row per DID.
     *
     * @param dids the DIDs to print
     */
    static void printRows(List<DidModel> dids) {
        if (dids.isEmpty()) {
            System.out.println(Ansi.AUTO.text("@|yellow No DIDs to show. |@"));
            return;
        }

        for (DidModel did : dids) {
            System.out.printf("%-6d %-14s %-7s %-16s %-28s %-14s%n",
                    did.getId(),
                    DidRepository.callerIdOrDash(did),
                    DidRepository.activeMarker(did),
                    truncate(DidRepository.orDash(did.getGroup()), 16),
                    truncate(DidRepository.orDash(did.getDescription()), 28),
                    truncate(DidRepository.orDash(did.getCarrier()), 14));
        }
    }

    /**
     * Prints the DIDs followed by a count.
     *
     * @param dids  the DIDs to print
     * @param title the section title
     */
    static void print(List<DidModel> dids, String title) {
        printHeader(title + " (" + dids.size() + ")");
        printColumnHeaders();
        printRows(dids);
        System.out.println(SEPARATOR);
    }

    /**
     * Prints a labelled field, used by the detail view.
     *
     * @param label the field name
     * @param value the field value
     */
    static void printField(String label, String value) {
        System.out.printf("  %-12s %s%n", label + ":", value);
    }

    /**
     * Shortens a value to fit its column, marking the cut with an ellipsis.
     *
     * @param value the value to shorten
     * @param width the column width
     * @return the value, truncated when needed
     */
    static String truncate(String value, int width) {
        if (value.length() <= width) {
            return value;
        }

        return value.substring(0, width - 1) + "…";
    }
}
