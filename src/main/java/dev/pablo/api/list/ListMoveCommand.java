package dev.pablo.api.list;

import java.io.IOException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Moves a list to a different campaign.
 *
 * <p>
 * Subcommand of {@code list}. Calls {@code update_list} through the Non-Agent
 * API with the {@code campaign_id} field.
 * </p>
 */
@Command(name = "move", description = {
    "Move a list to a different campaign.",
    "",
    "Examples:",
    "  vicidial-cli list move --campaignId SALES --id 6004",
    "  vicidial-cli list move -c SALES -id 6004"
}, mixinStandardHelpOptions = true)
public class ListMoveCommand implements Callable<Integer> {

    @Option(names = { "-c",
        "--campaignId" }, description = "Destination campaign identifier, 2-8 characters.", defaultValue = "")
    private String campaignId;

    @Option(names = { "-id",
        "--list_id", "--id" }, description = "List identifier, 2-14 digits.", defaultValue = "")
    private String listId;

    private final ListRepository repository;

    public ListMoveCommand() {
        this(new ListRepository());
    }

    ListMoveCommand(ListRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() {
        if (listId.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"List_Id\" is missing. |@ "));
            return 1;
        }

        if (campaignId.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"Campaign_Id\" is missing. |@ "));
            return 1;
        }

        String id = listId.trim();
        String campaign = campaignId.trim();

        if (!ListRepository.isValidListId(id)) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + ListRepository.listIdError(id) + " |@"));
            return 1;
        }

        if (!ListRepository.isValidCampaignId(campaign)) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + ListRepository.campaignIdError(campaign) + " |@"));
            return 1;
        }

        try {
            String response = repository.moveToCampaign(id, campaign);

            if (!ListRepository.isSuccess(response)) {
                System.err.println(Ansi.AUTO.text("❌ @|red List " + id + " was not moved: "
                        + ListRepository.responseMessage(response) + " |@"));
                return 1;
            }

            System.out.println(Ansi.AUTO.text("@|green ✅ List " + id + " moved to campaign "
                    + campaign + ". |@"));
            return 0;
        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red API or network error: |@" + e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red The request was interrupted. |@"));
            Thread.currentThread().interrupt();
            return 1;
        }
    }
}
