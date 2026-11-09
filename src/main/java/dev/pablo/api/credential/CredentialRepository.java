package dev.pablo.api.credential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import dev.pablo.api.VicidialClientSingleton;
import dev.pablo.models.UserModel;

/**
 * Reads and updates users on a Vicidial instance.
 *
 * <p>
 * Uses the Non-Agent API {@code users_list} and {@code update_user} functions so
 * the {@code credential} subcommands stay thin.
 * </p>
 */
public class CredentialRepository {

    /**
     * Users above this level are hidden from listings, even when the API user is
     * allowed to see them.
     */
    public static final int MAX_VISIBLE_LEVEL = 8;

    private final VicidialClientSingleton client;

    public CredentialRepository() {
        this(VicidialClientSingleton.getInstance());
    }

    public CredentialRepository(VicidialClientSingleton client) {
        this.client = client;
    }

    /**
     * Fetches users, optionally for a single user id.
     *
     * <p>
     * Uses {@code users_list} when the instance supports it. Older builds return
     * {@code ERROR: NO FUNCTION SPECIFIED}; those fall back to {@code user_details}
     * for a single user and to the admin Users page for a full list.
     * </p>
     *
     * @param agentUser one user id, or blank for every user
     * @return the parsed users, never null
     * @throws IOException          if the API rejects the request or cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public List<UserModel> fetchAll(String agentUser) throws IOException, InterruptedException {
        String response = client.getUsersList(agentUser);
        List<UserModel> users;

        if (isUnsupportedFunction(response)) {
            users = fetchFallback(agentUser);
        } else if (isError(response)) {
            throw new IOException(responseMessage(response));
        } else {
            users = parseUsersList(response);
        }

        return users.stream().filter(CredentialRepository::isVisible).toList();
    }

    /**
     * Hides users above {@link #MAX_VISIBLE_LEVEL}.
     *
     * @param user the user to check
     * @return true when the user may be shown
     */
    static boolean isVisible(UserModel user) {
        if (user == null || user.getUserLevel() == null) {
            return true;
        }

        try {
            return Integer.parseInt(user.getUserLevel().trim()) <= MAX_VISIBLE_LEVEL;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private List<UserModel> fetchFallback(String agentUser) throws IOException, InterruptedException {
        if (agentUser != null && !agentUser.isBlank()) {
            String response = client.getUserDetails(agentUser.trim());

            if (isError(response)) {
                throw new IOException(responseMessage(response));
            }

            return parseUsersList(response);
        }

        String html = client.getFromWeb(client.adminPageUrl("0A") + "&status=display_all");
        return parseUsersHtml(html);
    }

    private static boolean isError(String response) {
        return response != null && response.trim().toUpperCase(Locale.ROOT).startsWith("ERROR");
    }

    private static boolean isUnsupportedFunction(String response) {
        return response != null
                && response.toUpperCase(Locale.ROOT).contains("NO FUNCTION SPECIFIED");
    }

    /**
     * Validates the configuration needed to update a phone.
     *
     * @throws IllegalStateException if SERVER_IP is not configured
     */
    public void requirePhoneConfig() {
        client.requireConfig(VicidialClientSingleton.VAR_SERVER_IP);
    }

    /**
     * Updates the display name and/or password of a user.
     *
     * @param id       the user identifier
     * @param name     new display name, or blank to keep it
     * @param password new password, or blank to keep it
     * @throws IOException          if the API cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void updateUser(String id, String name, String password) throws IOException, InterruptedException {
        client.updateUser(id, name, password);
    }

    /**
     * Updates the password of the phone that belongs to a user.
     *
     * @param id       the user and phone identifier
     * @param password the new password
     * @throws IOException          if the API cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void updatePhone(String id, String password) throws IOException, InterruptedException {
        client.updatePhone(id, password);
    }

    /**
     * Sets the active status of a user.
     *
     * @param id     the user identifier
     * @param active true for 'Y', false for 'N'
     * @return the raw API response body
     * @throws IOException          if the API cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public String setActive(String id, boolean active) throws IOException, InterruptedException {
        return client.updateUserStatus(id, active ? "Y" : "N");
    }

    /**
     * Parses a pipe-delimited user response.
     *
     * <p>
     * Accepts both {@code users_list} rows
     * ({@code user|full_name|user_group|user_level|active|email}) and the shorter
     * {@code user_details} rows
     * ({@code user|full_name|user_group|user_level|active}), where the email is
     * left blank. Blank rows, a header row and malformed rows are skipped.
     * </p>
     *
     * @param response the raw API response
     * @return the parsed users, never null
     */
    static List<UserModel> parseUsersList(String response) {
        List<UserModel> users = new ArrayList<>();

        if (response == null || response.isBlank()) {
            return users;
        }

        for (String row : response.split("\\R")) {
            String line = row.trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] segments = line.split("\\|", -1);

            if (segments.length < 5 || segments[0].isBlank()
                    || "user".equalsIgnoreCase(segments[0].trim())) {
                continue;
            }

            users.add(new UserModel(
                    segments[0].trim(),
                    segments[1].trim(),
                    segments[2].trim(),
                    segments[3].trim(),
                    segments[4].trim(),
                    segments.length >= 6 ? segments[5].trim() : ""));
        }

        return users;
    }

    /**
     * Parses the admin Users page ({@code admin.php?ADD=0A}).
     *
     * <p>
     * Each row starts with a link to {@code ADD=3&user=<id>} and has the columns
     * user, full name, level, group and active. The page does not expose email.
     * </p>
     *
     * @param html the admin page response
     * @return the parsed users, never null
     */
    static List<UserModel> parseUsersHtml(String html) {
        List<UserModel> users = new ArrayList<>();

        if (html == null || html.isBlank()) {
            return users;
        }

        Document doc = Jsoup.parse(html);

        for (Element row : doc.select("tr.records_list_x, tr.records_list_y")) {
            Elements cols = directCells(row);

            if (cols.size() < 5 || !isUserRow(cols)) {
                continue;
            }

            users.add(new UserModel(
                    cols.get(0).text().trim(),
                    cols.get(1).text().trim(),
                    cols.get(3).text().trim(),
                    cols.get(2).text().trim(),
                    cols.get(4).text().trim(),
                    ""));
        }

        return users;
    }

    private static Elements directCells(Element row) {
        Elements cells = new Elements();

        for (Element child : row.children()) {
            if ("td".equals(child.tagName())) {
                cells.add(child);
            }
        }

        return cells;
    }

    /**
     * Checks that a row is a real user row: its first cell links to
     * {@code ADD=3&user=<id>} and the cell text is a valid Vicidial user id.
     *
     * @param cols the row cells
     * @return true when the row looks like a user
     */
    private static boolean isUserRow(Elements cols) {
        Element link = cols.get(0).selectFirst("a[href]");

        if (link == null || !link.attr("href").contains("ADD=3&user=")) {
            return false;
        }

        return cols.get(0).text().trim().matches("[A-Za-z0-9_]{2,20}");
    }

    /**
     * Checks whether a user is active.
     *
     * @param user the user to inspect
     * @return true when the user is marked active
     */
    public static boolean isActive(UserModel user) {
        return user != null && user.getActive() != null
                && "Y".equalsIgnoreCase(user.getActive().trim());
    }

    /**
     * Checks whether an API response reports success.
     *
     * @param response the raw API response
     * @return true when the first token is {@code SUCCESS}
     */
    public static boolean isSuccess(String response) {
        return response != null && response.trim().toUpperCase(Locale.ROOT).startsWith("SUCCESS");
    }

    /**
     * Returns the first non-blank line of an API response.
     *
     * @param response the raw API response
     * @return a printable message
     */
    public static String responseMessage(String response) {
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
