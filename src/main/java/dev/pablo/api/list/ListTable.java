package dev.pablo.api.list;

import java.util.List;

import dev.pablo.models.ListModel;
import picocli.CommandLine.Help.Ansi;

/**
 * Renders lists as an aligned text table.
 */
final class ListTable {

    private static final String SEPARATOR = "-".repeat(104);

    private ListTable() {
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
        System.out.printf("%-8s %-28s %-36s %-7s %-20s%n",
                "ID", "NAME", "DESCRIPTION", "ACTIVE", "CAMPAIGN");
        System.out.println(SEPARATOR);
    }

    /**
     * Prints one row per list.
     *
     * @param lists the lists to print
     */
    static void printRows(List<ListModel> lists) {
        if (lists.isEmpty()) {
            System.out.println(Ansi.AUTO.text("@|yellow No lists to show. |@"));
            return;
        }

        for (ListModel list : lists) {
            System.out.printf("%-8d %-28s %-36s %-7s %-20s%n",
                    list.getListId(),
                    truncate(ListRepository.orDash(list.getListName()), 28),
                    truncate(ListRepository.orDash(list.getDescription()), 36),
                    ListRepository.activeMarker(list),
                    truncate(ListRepository.orDash(list.getCampaign()), 20));
        }
    }

    /**
     * Prints the lists followed by a count.
     *
     * @param lists the lists to print
     * @param title the section title
     */
    static void print(List<ListModel> lists, String title) {
        printHeader(title + " (" + lists.size() + ")");
        printColumnHeaders();
        printRows(lists);
        System.out.println(SEPARATOR);
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
