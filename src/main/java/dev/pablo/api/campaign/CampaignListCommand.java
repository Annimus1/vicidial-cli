package dev.pablo.api.campaign;

import java.util.List;
import java.util.concurrent.Callable;

import dev.pablo.models.CampaignModel;
import picocli.CommandLine.Command;

/**
 * Lists the campaigns configured on a Vicidial instance.
 *
 * <p>
 * Subcommand of {@code campaign}. Output is a table; machine-readable output is
 * tracked separately.
 * </p>
 */
@Command(name = "list", description = {
    "List the campaigns stored on the instance.",
    "",
    "Examples:",
    "  vicidial-cli campaign list"
}, mixinStandardHelpOptions = true)
public class CampaignListCommand implements Callable<Integer> {

    private final CampaignRepository repository;

    public CampaignListCommand() {
        this(new CampaignRepository());
    }

    CampaignListCommand(CampaignRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() throws Exception {
        List<CampaignModel> campaigns = repository.fetchAll();

        CampaignTable.print(campaigns, "Campaigns found");
        return 0;
    }
}
