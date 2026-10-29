package dev.pablo.api.campaign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dev.pablo.api.VicidialClientSingleton;
import dev.pablo.models.CampaignModel;

/**
 * Reads campaigns from a Vicidial instance.
 *
 * <p>
 * Every {@code campaign} subcommand that lists campaigns needs the same fetch
 * and parse step, so it lives here instead of being repeated in each command.
 * </p>
 */
public class CampaignRepository {

    private final VicidialClientSingleton client;

    public CampaignRepository() {
        this(VicidialClientSingleton.getInstance());
    }

    public CampaignRepository(VicidialClientSingleton client) {
        this.client = client;
    }

    /**
     * Fetches and parses every campaign on the instance.
     *
     * @return the parsed campaigns, never null
     * @throws IOException          if the API cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public List<CampaignModel> fetchAll() throws IOException, InterruptedException {
        return parse(client.getCampaigns());
    }

    /**
     * Reads the stored settings of one campaign.
     *
     * @param campaignId the campaign identifier
     * @return the raw API response body
     * @throws IOException          if the API cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public String fetchDetail(String campaignId) throws IOException, InterruptedException {
        return client.showCampaign(campaignId);
    }

    /**
     * Parses the pipe-delimited response returned by {@code campaigns_list}.
     *
     * <p>
     * Each row is {@code id|description|active|...}. Blank and malformed rows are
     * skipped so one unexpected line does not hide the whole list.
     * </p>
     *
     * @param response the raw API response
     * @return the parsed campaigns, never null
     */
    static List<CampaignModel> parse(String response) {
        List<CampaignModel> campaigns = new ArrayList<>();

        if (response == null || response.isBlank()) {
            return campaigns;
        }

        for (String row : response.split("\\R")) {
            String line = row.trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] segments = line.split("\\|", -1);

            if (segments.length < 3 || segments[0].isBlank()) {
                continue;
            }

            campaigns.add(new CampaignModel(
                    segments[0].trim(),
                    segments[1].trim(),
                    segments[2].trim()));
        }

        return campaigns;
    }
}
