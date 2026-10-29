package dev.pablo.api;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.pablo.models.CampaignBuildInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "campaign", description = "Gets and displays the list of active/inactive campaigns from Vicidial.", mixinStandardHelpOptions = true)
public class CampaignsCommand implements Callable<Integer> {

    private static final String STEP_SEPARATOR =
            "----------------------------------------------------------";

    private final VicidialClientSingleton client;

    @Parameters(index = "0", description = "Command to perform the action.\n"
            + "INFO - It provides information on each campaign available in the Vicidial system.\n"
            + "CREATE - Create a campaign in the Vicidial system from scratch, create the user group, the list, the inbound group, and the users with their respective phone.\n"
            + "PAUSE - Change the status of a specific campaign to \"PAUSED\" in the Vicidial system\n"
            + "ACTIVE - Change the status of a specific campaign to \"ACTIVE\" in the Vicidial system.\n\n"
            + "USAGE\n"
            + "campaign INFO -id [campaignId] \n"
            + "campaign PAUSE -id [campaignId]\n"
            + "campaign ACTIVE -id [campaignId]\n"
            + "campaign CREATE -id [campaignId] -D [description] -n [numberOfUsers] -p [password]")
    private String command;

    @Option(names = { "-id", "--campaign_id" }, description = ". (optional)", defaultValue = "")
    private String campaign_ID;

    @Option(names = { "-D", "--description" }, description = "", defaultValue = "")
    private String campaignDescription;

    @Option(names = { "-n", "--users" }, description = "Number of agent users (with their phones) to create for the campaign.", defaultValue = "0")
    private int userCount;

    @Option(names = { "-p", "--password" }, description = "Shared password for the created agent users.", defaultValue = "")
    private String userPassword;

    public CampaignsCommand() {
        this.client = VicidialClientSingleton.getInstance();
    }

    @Override
    public Integer call() {

        try {
            // INFO
            if (command.toLowerCase().equals("info")) {
                getCampaign_Info();
            }

            // CREATE
            else if (command.toLowerCase().equals("create")) {
                
                // Campaign ID
                if(this.campaign_ID.isBlank()){
                    System.err.println(Ansi.AUTO.text("❌ @|red Campaign ID is missing. |@"));
                    return 1;
                }
                else if(this.campaign_ID.length() <= 4){
                    System.err.println(Ansi.AUTO.text("❌ @|red Campaign ID should be greater than 4 characters. |@"));
                    return 1;
                } 

                // Campaign description
                else if (this.campaignDescription.isBlank()) {
                    System.err.println(Ansi.AUTO.text("❌ @|red Campaign Description is missing. |@"));
                    return 1;
                } 
                else if (this.campaignDescription.length() <= 4) {
                    System.err.println(Ansi.AUTO.text("❌ @|red Description should be greater than 4 characters. |@"));
                    return 1;
                } 

                // Users
                else if (this.userCount < 0) {
                    System.err.println(Ansi.AUTO.text("❌ @|red Number of users should be zero or greater. |@"));
                    return 1;
                }
                else if (this.userCount > 0 && this.userPassword.isBlank()) {
                    System.err.println(Ansi.AUTO.text("❌ @|red A password is required to create " + this.userCount + " user(s). |@"));
                    return 1;
                }

                // Configuration required by the creation chain, checked before any
                // object is created so a missing variable cannot leave a partial
                // campaign behind.
                else if (!checkCreateConfig()) {
                    return 1;
                }

                // Create Camp
                else {
                    return createCampaign() ? 0 : 1;
                }
            }

            // PAUSE
            else if (command.toLowerCase().equals("pause")) {
                if (!campaign_ID.isBlank()) {
                    System.out.println(Ansi.AUTO.text("@|blue Preparing for update Campaign...|@"));
                    client.updateCampaignStatus(campaign_ID, "N");
                } else {
                    System.err.println(
                            Ansi.AUTO.text("❌ @|red Parameter \"Campaign_Id\" is missing. |@ "));
                    return 1;
                }
            }

            // ACTIVE
            else if (command.toLowerCase().equals("active")) {
                if (!campaign_ID.isBlank()) {
                    System.out.println(Ansi.AUTO.text("@|blue Preparing for update Campaign...|@"));
                    client.updateCampaignStatus(campaign_ID, "Y");
                } else {
                    System.err.println(
                            Ansi.AUTO.text("❌ @|red Parameter \"Campaign_Id\" is missing. |@ "));
                    return 1;
                }
            }

            // DEFAULT (argument unknown)
            else {
                System.err.println(Ansi.AUTO.text("❌ @|red Argument " + command + " unknown. |@ "));
                System.err.println(Ansi.AUTO.text("🧐 @|yellow Try campaign -h to get information. |@ "));
                return 1;
            }

            return 0; // Success

        } catch (IOException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red API or network error:|@ " + e.getMessage()));
            return 1; // Error
        } catch (InterruptedException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red The request was interrupted.|@"));
            Thread.currentThread().interrupt();
            return 1;
        }
    }

    public void getCampaign_Info() throws IOException, InterruptedException {
        System.out.println(Ansi.AUTO.text("@|yellow Searching campaigns...|@"));

        String formattedResult = client.getCampaigns();

        System.out.println(Ansi.AUTO.text("\n@|blue Campaigns list's obtained:|@"));
        System.out.println("---------------------------------------------------------");
        String[] rows = formattedResult.split("\\R");

        for (String row : rows) {
            String[] segments = row.split("\\|");
            String active = segments[2].equalsIgnoreCase("y") ? "✅" : "❌";

            System.out.println(active + " ID: " + segments[0] + " | Description: " + segments[1]);
        }
        System.out.println("---------------------------------------------------------");
    }

    public boolean createCampaign() {
        String step = "initialization";
        long campaignStartedAt = System.currentTimeMillis();
        long stepStartedAt = campaignStartedAt;

        String userGroup = null;
        String cidGroup = null;
        String inboundGroup = null;
        String campaignId = null;
        String listId = null;

        try {
            System.out.println(Ansi.AUTO.text("\n@|blue Building campaign|@ " + this.campaign_ID));
            System.out.println("  description: " + this.campaignDescription);
            System.out.println("  users: " + this.userCount
                    + " | agent password: " + (this.userPassword.isBlank() ? "n/a" : "********"));
            System.out.println(STEP_SEPARATOR);

            // 1. User group
            step = "1/8 create user group";
            String userGroupResult = client.createUserGroup(this.campaign_ID, this.campaignDescription);
            userGroup = userGroupResult;
            if (!client.isValidUserGroup(userGroup)) {
                System.out.println(Ansi.AUTO.text(
                        "⚠️ @|yellow User group '" + userGroup + "' was not listed after creation, continuing anyway. |@"));
            }
            logStep(step, "user group", userGroup, stepStartedAt);
            stepStartedAt = System.currentTimeMillis();

            // 2. CID group
            step = "2/8 create CID group";
            String cidGroupResult = client.createCIDGroup(this.campaign_ID, this.campaignDescription);
            cidGroup = cidGroupResult;
            logStep(step, "CID group", cidGroup, stepStartedAt);
            stepStartedAt = System.currentTimeMillis();

            // 3. Inbound group
            step = "3/8 create inbound group";
            String inboundGroupResult = client.createInboundGroup(this.campaign_ID, this.campaignDescription,
                    userGroup);
            inboundGroup = inboundGroupResult;
            logStep(step, "inbound group", inboundGroup, stepStartedAt);
            stepStartedAt = System.currentTimeMillis();

            // 4. Campaign
            step = "4/8 create campaign";
            CampaignBuildInfo campaingInfo = new CampaignBuildInfo(this.campaign_ID, this.campaign_ID,
                    this.campaignDescription, userGroup, inboundGroup, cidGroup);
            String campaignIdResult = client.copyExistingCampaign(campaingInfo);
            campaignId = campaignIdResult;
            if (campaignId.isBlank()) {
                throw new IllegalStateException("The campaign was not created.");
            }
            logStep(step, "campaign", campaignId, stepStartedAt);
            stepStartedAt = System.currentTimeMillis();

            // 5. Apply groups
            step = "5/8 apply groups to campaign";
            boolean applied = client.configureCampaignGroups(campaignId, userGroup, inboundGroup, cidGroup);
            if (!applied) {
                throw new IllegalStateException(
                        "The groups could not be applied to campaign '" + campaignId + "'.");
            }
            logStep(step, "user group -> " + userGroup + " | inbound group -> " + inboundGroup + " | CID group -> "
                    + cidGroup, null, stepStartedAt);
            stepStartedAt = System.currentTimeMillis();

            // 6. Verify the groups were actually stored
            step = "6/8 verify campaign groups";
            verifyCampaignGroups(campaignId, userGroup, inboundGroup, cidGroup);
            logStep(step, "campaign settings read back from Vicidial", null, stepStartedAt);
            stepStartedAt = System.currentTimeMillis();

            // 7. List
            step = "7/8 create list";
            String listIdResult = client.createList(null, this.campaign_ID, "", campaignId);
            listId = listIdResult;
            if (listId == null || listId.isBlank()) {
                System.out.println(Ansi.AUTO.text("⚠️ @|yellow List was not created, the campaign has no list attached. |@"));
            } else {
                logStep(step, "list", listId, stepStartedAt);
            }
            stepStartedAt = System.currentTimeMillis();

            // 8. Users
            if (this.userCount > 0) {
                step = "create " + this.userCount + " user(s) and phone(s)";
                createUsers(userGroup);
                logStep("users", this.userCount + " agent(s) in group " + userGroup, null, stepStartedAt);
            }

            System.out.println(STEP_SEPARATOR);
            System.out.println(Ansi.AUTO.text("@|green ✅ Campaign " + campaignId + " built successfully.|@"));
            System.out.println("  user group:   " + userGroup);
            System.out.println("  CID group:    " + cidGroup);
            System.out.println("  inbound group: " + inboundGroup);
            System.out.println("  campaign:     " + campaignId);
            System.out.println("  list:         " + (listId == null || listId.isBlank() ? "none" : listId));
            System.out.println("  users:        " + this.userCount);
            System.out.println(STEP_SEPARATOR);
            return true;

        } catch (Exception e) {
            String created = (describe("user group", userGroup)
                    + describe("CID group", cidGroup)
                    + describe("inbound group", inboundGroup)
                    + describe("campaign", campaignId)
                    + describe("list", listId)).trim();

            System.err.println(STEP_SEPARATOR);
            System.err.println(Ansi.AUTO.text("❌ @|red Campaign " + this.campaign_ID + " failed at: " + step + " |@"));
            System.err.println("  " + errorMessage(e));
            System.err.println("  created before the failure: " + (created.isEmpty() ? "nothing" : created));
            System.err.println("  elapsed: " + (System.currentTimeMillis() - campaignStartedAt) + " ms");
            System.err.println(STEP_SEPARATOR);
            return false;
        }
    }

    /**
     * Verifies that the configuration needed by the CREATE chain is present.
     *
     * <p>
     * The user group, CID group, inbound group and list steps go through the admin
     * web pages, so SERVER_URL is always required. Phones are only created when
     * users were requested, which additionally requires SERVER_IP and TEMPLATE_ID.
     * </p>
     *
     * @return true when every required variable is configured
     */
    private boolean checkCreateConfig() {
        try {
            client.requireConfig(VicidialClientSingleton.VAR_SERVER_URL);

            if (this.userCount > 0) {
                client.requirePhoneConfig();
            }

            return true;
        } catch (IllegalStateException e) {
            System.err.println(Ansi.AUTO.text("❌ @|red " + e.getMessage() + " |@"));
            return false;
        }
    }

    private static String errorMessage(Exception e) {
        String message = (e.getMessage() == null || e.getMessage().isBlank()) ? "" : e.getMessage();
        return message.isEmpty() ? e.toString() : e.getClass().getSimpleName() + ": " + message;
    }

    /**
     * Reads the campaign back from Vicidial and compares the stored settings with
     * the ones that were requested. The update API answers "SUCCESS" for the
     * parameters it recognised, so a success response alone does not prove the
     * user group, inbound group or CID were stored.
     */
    private void verifyCampaignGroups(String campaignId, String userGroup, String inboundGroup, String cidGroup)
            throws IOException, InterruptedException {

        String info = client.showCampaign(campaignId);
        String actualUserGroup = extractField(info, "user_group");

        if (actualUserGroup == null) {
            System.out.println("   raw response: " + info.trim());
            System.out.println(Ansi.AUTO.text(
                    "   ⚠️ @|yellow Could not read the user group back, check the campaign manually. |@"));
            return;
        }

        reportMismatch("user group", userGroup, actualUserGroup);
        reportMismatch("inbound group", inboundGroup, extractField(info, "closer_campaigns"));
        reportMismatch("CID group", cidGroup, extractField(info, "campaign_cid"));
    }

    private static void reportMismatch(String label, String expected, String actual) {
        if (actual == null || actual.isBlank()) {
            return;
        }
        if (expected.equalsIgnoreCase(actual.trim())) {
            System.out.println(Ansi.AUTO.text("   " + label + " -> " + actual.trim() + " @|green verified|@"));
        } else {
            System.out.println(Ansi.AUTO.text("   ⚠️ @|yellow " + label + ": Vicidial stored '" + actual.trim()
                    + "' but '" + expected + "' was requested. |@"));
        }
    }

    private static String extractField(String body, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"?([^\",}]*)").matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static void logStep(String step, String label, String id, long startedAt) {
        long elapsed = System.currentTimeMillis() - startedAt;
        String suffix = (id == null || id.isBlank()) ? "" : " -> " + id;
        System.out.println(Ansi.AUTO.text("✅ @|green [" + step + "]|@ " + label + suffix) + " (" + elapsed + " ms)");
    }

    private static String describe(String label, String value) {
        return (value == null || value.isBlank()) ? "" : label + "=" + value + " ";
    }

    private void createUsers(String userGroup) throws IOException, InterruptedException {
        System.out.println(Ansi.AUTO.text("@|blue Creating " + this.userCount + " user(s) in group " + userGroup + "...|@"));

        int width = String.valueOf(this.userCount).length();
        long startedAt = System.currentTimeMillis();

        for (int i = 1; i <= this.userCount; i++) {
            String agentId = this.campaign_ID + String.format("%0" + width + "d", i);
            System.out.println(Ansi.AUTO.text("   [" + i + "/" + this.userCount + "] creating " + agentId + " ... "));

            client.createUser(agentId, this.userPassword, agentId + "1", userGroup);
            client.createPhone(agentId, this.userPassword);

            System.out.println(Ansi.AUTO.text("   ☑️ @|green done|@ user and phone " + agentId));
        }

        System.out.println(Ansi.AUTO.text("@|green ✅ " + this.userCount + " user(s) created successfully.|@")
                + " (" + (System.currentTimeMillis() - startedAt) + " ms)");
    }
}
