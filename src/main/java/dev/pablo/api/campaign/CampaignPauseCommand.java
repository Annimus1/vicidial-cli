package dev.pablo.api.campaign;

import java.io.IOException;
import java.util.concurrent.Callable;

import dev.pablo.api.VicidialClientSingleton;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Sets a campaign to inactive.
 *
 * <p>
 * Subcommand of {@code campaign}. Replaces {@code campaign PAUSE -id ID}.
 * </p>
 */
@Command(name = "pause", description = {
    "Change the status of a campaign to PAUSED.",
    "",
    "Examples:",
    "  vicidial-cli campaign pause --id SALES",
    "  vicidial-cli campaign pause -id SALES"
}, mixinStandardHelpOptions = true)
public class CampaignPauseCommand implements Callable<Integer> {

    @Option(names = { "-id",
        "--campaign_id", "--id" }, description = "Campaign identifier.", defaultValue = "")
    private String campaignId;

    private final VicidialClientSingleton client;

    public CampaignPauseCommand() {
        this(VicidialClientSingleton.getInstance());
    }

    CampaignPauseCommand(VicidialClientSingleton client) {
        this.client = client;
    }

    @Override
    public Integer call() {
        if (campaignId.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"Campaign_Id\" is missing. |@ "));
            return 1;
        }

        try {
            System.out.println(Ansi.AUTO.text("@|blue Preparing for update Campaign...|@"));
            client.updateCampaignStatus(campaignId.trim(), "N");
            return 0;
        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red API or network error:|@ " + e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red The request was interrupted.|@"));
            Thread.currentThread().interrupt();
            return 1;
        }
    }
}
