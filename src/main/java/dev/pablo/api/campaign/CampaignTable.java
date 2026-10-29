package dev.pablo.api.campaign;

import java.util.List;

import dev.pablo.models.CampaignModel;
import picocli.CommandLine.Help.Ansi;

/**
 * Renders campaigns as an aligned text table.
 */
final class CampaignTable {

    private static final String SEPARATOR = "-".repeat(76);

    private CampaignTable() {
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
        System.out.printf("%-24s %-40s %-7s%n", "ID", "DESCRIPTION", "ACTIVE");
        System.out.println(SEPARATOR);
    }

    /**
     * Prints one row per campaign.
     *
     * @param campaigns the campaigns to print
     */
    static void printRows(List<CampaignModel> campaigns) {
        if (campaigns.isEmpty()) {
            System.out.println(Ansi.AUTO.text("@|yellow No campaigns to show. |@"));
            return;
        }

        for (CampaignModel campaign : campaigns) {
            System.out.printf("%-24s %-40s %-7s%n",
                    truncate(orDash(campaign.getId()), 24),
                    truncate(orDash(campaign.getDescription()), 40),
                    activeMarker(campaign));
        }
    }

    /**
     * Prints the campaigns followed by a count.
     *
     * @param campaigns the campaigns to print
     * @param title     the section title
     */
    static void print(List<CampaignModel> campaigns, String title) {
        printHeader(title + " (" + campaigns.size() + ")");
        printColumnHeaders();
        printRows(campaigns);
        System.out.println(SEPARATOR);
    }

    /**
     * Prints a labelled field, used by the detail view.
     *
     * @param label the field name
     * @param value the field value
     */
    static void printField(String label, String value) {
        System.out.printf("  %-18s %s%n", label + ":", value);
    }

    /**
     * Renders the active column.
     *
     * @param campaign the campaign to inspect
     * @return a coloured check or cross marker
     */
    static Ansi.Text activeMarker(CampaignModel campaign) {
        return campaign.isActive()
                ? Ansi.AUTO.text("✅")
                : Ansi.AUTO.text("❌");
    }

    /**
     * Returns a printable value, falling back to a dash when it is absent.
     *
     * @param value the value to render
     * @return the value, or "-" when null or blank
     */
    static String orDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value.trim();
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
