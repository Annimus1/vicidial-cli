package dev.pablo.api.list;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Creates a list and attaches it to a campaign.
 *
 * <p>
 * Subcommand of {@code list}. Reuses the {@code add_list} flow used by
 * {@code campaign create}. When {@code --id} is omitted the next available list
 * id is used, which requires SERVER_URL for the admin list page.
 * </p>
 */
@Command(name = "create", description = {
    "Create a list and attach it to a campaign.",
    "",
    "When --id is omitted the next available list id is used (requires SERVER_URL).",
    "",
    "Examples:",
    "  vicidial-cli list create --name SALES --desc \"Sales leads\" --campaign-id SALES",
    "  vicidial-cli list create --id 6004 --name SALES --desc \"Sales leads\" --campaign-id SALES"
}, mixinStandardHelpOptions = true)
public class ListCreateCommand implements Callable<Integer> {

    @Option(names = { "-id",
        "--list_id", "--id" }, description = "List identifier, 2-14 digits. Computed when omitted.",
        defaultValue = "")
    private String listId;

    @Option(names = { "-n",
        "--list_name", "--name" }, description = "Name of the new list.", defaultValue = "")
    private String name;

    @Option(names = { "-D",
        "--list_description", "--description", "--desc" }, description = "Description of the new list.",
        defaultValue = "")
    private String description;

    @Option(names = { "-c",
        "--campaign_id", "--campaign-id", "--campaign" }, description = "Campaign to attach the list to.",
        defaultValue = "")
    private String campaignId;

    private final ListRepository repository;

    public ListCreateCommand() {
        this(new ListRepository());
    }

    ListCreateCommand(ListRepository repository) {
        this.repository = repository;
    }

    @Override
    public Integer call() {
        if (name.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"List Name\" is missing. |@ "));
            return 1;
        }

        if (campaignId.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red Parameter \"Campaign Id\" is missing. |@ "));
            return 1;
        }

        String id = listId.trim();
        String campaign = campaignId.trim();

        if (!id.isEmpty() && !ListRepository.isValidListId(id)) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + ListRepository.listIdError(id) + " |@"));
            return 1;
        }

        if (!ListRepository.isValidCampaignId(campaign)) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + ListRepository.campaignIdError(campaign) + " |@"));
            return 1;
        }

        if (id.isEmpty()) {
            try {
                repository.requireServerUrl();
            } catch (IllegalStateException e) {
                System.err.println(Ansi.AUTO.text("❌ @|red " + e.getMessage() + " |@"));
                return 1;
            }
        }

        String created = repository.create(id.isEmpty() ? null : id, name.trim(), description.trim(), campaign);

        if (created == null || created.isBlank()) {
            System.err.println(Ansi.AUTO.text("❌ @|red List was not created. |@"));
            return 1;
        }

        return 0;
    }
}
