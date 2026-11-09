package dev.pablo.api.list;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.pablo.api.VicidialClientSingleton;
import dev.pablo.models.ListModel;
import picocli.CommandLine.Help.Ansi;

/**
 * Reads the lists stored on a Vicidial instance.
 *
 * <p>
 * Every {@code list} subcommand needs the same fetch step, so it lives here
 * instead of being repeated in each command.
 * </p>
 */
public class ListRepository {

    private final VicidialClientSingleton client;

    public ListRepository() {
        this(VicidialClientSingleton.getInstance());
    }

    public ListRepository(VicidialClientSingleton client) {
        this.client = client;
    }

    /**
     * Fetches every list on the instance.
     *
     * @return the lists, never null
     * @throws IOException          if the admin page cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public List<ListModel> fetchAll() throws IOException, InterruptedException {
        return client.getAllLists();
    }

    /**
     * Sets the active status of a list.
     *
     * @param listId the list identifier, 2-14 digits
     * @param active true for 'Y', false for 'N'
     * @return the raw API response body
     * @throws IOException          if the API cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public String setActive(String listId, boolean active) throws IOException, InterruptedException {
        return client.updateList(listId, Map.of("active", active ? "Y" : "N"));
    }

    /**
     * Moves a list to a different campaign.
     *
     * @param listId     the list identifier, 2-14 digits
     * @param campaignId the destination campaign identifier
     * @return the raw API response body
     * @throws IOException          if the API cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public String moveToCampaign(String listId, String campaignId) throws IOException, InterruptedException {
        return client.updateList(listId, Map.of("campaign_id", campaignId));
    }

    /**
     * Resets the called-since-last-reset flag for every lead in a list.
     *
     * @param listId the list identifier, 2-14 digits
     * @return the raw API response body
     * @throws IOException          if the API cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public String reset(String listId) throws IOException, InterruptedException {
        return client.updateList(listId, Map.of("reset_list", "Y"));
    }

    /**
     * Checks that a list id is all digits and 2 to 14 characters long.
     *
     * @param listId the value to check
     * @return true when the id is valid
     */
    public static boolean isValidListId(String listId) {
        return listId != null && listId.trim().matches("\\d{2,14}");
    }

    /**
     * Builds the error message for an invalid list id.
     *
     * @param listId the invalid value
     * @return a message describing the expected format
     */
    public static String listIdError(String listId) {
        return "Invalid list id \"" + listId + "\": expected 2 to 14 digits.";
    }

    /**
     * Checks that a campaign id is 2 to 8 characters long.
     *
     * @param campaignId the value to check
     * @return true when the id is valid
     */
    public static boolean isValidCampaignId(String campaignId) {
        if (campaignId == null) {
            return false;
        }

        int length = campaignId.trim().length();
        return length >= 2 && length <= 8;
    }

    /**
     * Builds the error message for an invalid campaign id.
     *
     * @param campaignId the invalid value
     * @return a message describing the expected format
     */
    public static String campaignIdError(String campaignId) {
        return "Invalid campaign id \"" + campaignId + "\": expected 2 to 8 characters.";
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

    /**
     * Checks whether a list is active.
     *
     * @param list the list to inspect
     * @return true when the list is marked active
     */
    public static boolean isActive(ListModel list) {
        return list != null && list.getActive() != null
                && "Y".equalsIgnoreCase(list.getActive().trim());
    }

    /**
     * Renders the active column for display.
     *
     * @param list the list to inspect
     * @return a coloured check or cross marker
     */
    public static Ansi.Text activeMarker(ListModel list) {
        return isActive(list)
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
}
