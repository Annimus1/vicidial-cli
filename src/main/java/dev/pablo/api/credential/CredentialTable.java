package dev.pablo.api.credential;

import java.util.List;

import dev.pablo.models.UserModel;
import picocli.CommandLine.Help.Ansi;

/**
 * Renders users as an aligned text table.
 */
final class CredentialTable {

    private static final String SEPARATOR = "-".repeat(104);

    private CredentialTable() {
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
        System.out.printf("%-16s %-28s %-16s %-6s %-7s %-24s%n",
                "USER", "FULL NAME", "GROUP", "LEVEL", "ACTIVE", "EMAIL");
        System.out.println(SEPARATOR);
    }

    /**
     * Prints one row per user.
     *
     * @param users the users to print
     */
    static void printRows(List<UserModel> users) {
        if (users.isEmpty()) {
            System.out.println(Ansi.AUTO.text("@|yellow No users to show. |@"));
            return;
        }

        for (UserModel user : users) {
            System.out.printf("%-16s %-28s %-16s %-6s %-7s %-24s%n",
                    truncate(orDash(user.getUser()), 16),
                    truncate(orDash(user.getFullName()), 28),
                    truncate(orDash(user.getUserGroup()), 16),
                    truncate(orDash(user.getUserLevel()), 6),
                    activeMarker(user),
                    truncate(orDash(user.getEmail()), 24));
        }
    }

    /**
     * Prints the users followed by a count.
     *
     * @param users the users to print
     * @param title the section title
     */
    static void print(List<UserModel> users, String title) {
        printHeader(title + " (" + users.size() + ")");
        printColumnHeaders();
        printRows(users);
        System.out.println(SEPARATOR);
    }

    private static Ansi.Text activeMarker(UserModel user) {
        return CredentialRepository.isActive(user)
                ? Ansi.AUTO.text("✅")
                : Ansi.AUTO.text("❌");
    }

    private static String orDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value.trim();
    }

    private static String truncate(String value, int width) {
        if (value.length() <= width) {
            return value;
        }

        return value.substring(0, width - 1) + "…";
    }
}
