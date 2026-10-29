package dev.pablo.api.campaign;

import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Shows the stored settings of a campaign.
 *
 * <p>
 * Subcommand of {@code campaign}. Calling {@code info} without an identifier
 * keeps the old {@code campaign INFO} behaviour and lists every campaign.
 * </p>
 */
@Command(name = "info", description = {
    "Show the stored settings of a campaign.",
    "",
    "Omit the campaign id to list every campaign, like the old 'campaign INFO'.",
    "",
    "Examples:",
    "  vicidial-cli campaign info",
    "  vicidial-cli campaign info --id SALES",
    "  vicidial-cli campaign info SALES"
}, mixinStandardHelpOptions = true)
public class CampaignInfoCommand implements Callable<Integer> {

    private static final String[] DETAIL_FIELDS = {
        "campaign_id",
        "campaign_name",
        "active",
        "user_group",
        "closer_campaigns",
        "campaign_cid"
    };

    @Option(names = { "-id", "--campaign_id", "--id" }, description = "Campaign identifier to inspect.", defaultValue = "")
    private String campaignIdOption;

    @Parameters(index = "0", arity = "0..1", description = "Campaign identifier to inspect.")
    private String campaignId;

    private final CampaignRepository repository;

    public CampaignInfoCommand() {
        this(new CampaignRepository());
    }

    CampaignInfoCommand(CampaignRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() throws Exception {
        String id = resolveId();

        if (id.isBlank()) {
            return new CampaignListCommand(repository).call();
        }

        String response = repository.fetchDetail(id);
        CampaignTable.printHeader("Campaign " + id.trim());

        boolean printed = false;

        for (String field : DETAIL_FIELDS) {
            String value = extractField(response, field);

            if (value != null && !value.isBlank()) {
                CampaignTable.printField(field, value.trim());
                printed = true;
            }
        }

        if (!printed) {
            System.out.println(Ansi.AUTO.text("@|yellow Could not parse the response, showing it raw:|@"));
            System.out.println(response == null ? "" : response.trim());
        }

        System.out.println();
        return 0;
    }

    /**
     * Resolves the campaign identifier from the option or the positional
     * argument.
     *
     * @return the identifier, or an empty string when none was given
     */
    private String resolveId() {
        if (campaignIdOption != null && !campaignIdOption.isBlank()) {
            return campaignIdOption.trim();
        }

        return campaignId == null ? "" : campaignId.trim();
    }

    /**
     * Extracts a field from the JSON-like {@code show_campaign} response.
     *
     * @param body  the raw response
     * @param field the field name
     * @return the value, or null when the field is absent
     */
    private static String extractField(String body, String field) {
        if (body == null || body.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"?([^\",}]*)").matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }
}
