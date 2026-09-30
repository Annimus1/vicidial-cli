package dev.pablo.api;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import dev.pablo.models.CampaignBuildInfo;
import dev.pablo.models.LeadModel;
import dev.pablo.models.ListModel;
import dev.pablo.models.UserGroupModel;
import io.github.cdimascio.dotenv.Dotenv;
import picocli.CommandLine.Help.Ansi;

/**
 * Singleton HTTP client wrapper for interacting with a Vicidial API.
 *
 * <p>
 * This class centralizes HTTP interactions required by the CLI:
 * loading configuration (from .env or system environment), building API URLs
 * with the required credentials, executing requests and providing higher-level
 * helper methods for specific Vicidial operations (campaigns, leads, users,
 * phones, DIDs, etc.).
 * </p>
 *
 * <p>
 * Configuration values consumed:
 * <ul>
 * <li>BASE_URL — Base API endpoint</li>
 * <li>API_USER — API username</li>
 * <li>API_PASSWORD — API password</li>
 * <li>SERVER_IP — SIP server IP for phone operations</li>
 * <li>TEMPLATE_ID — optional phone template id</li>
 * <li>SERVER_URL — secondary web UI URL used for DID operations</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
public class VicidialClientSingleton {

    public static VicidialClientSingleton instance = null;
    private HttpClient client;
    private final String baseUrl;
    // Credentials configuration (loaded from .env or system environment variables)
    private final String apiUser;
    private final String apiPass;
    private final String source = "java";
    private final String serverIp;
    private final String templateId;
    private final String serverUrl;

    /**
     * Creates a new wrapper instance using the provided HttpClient.
     *
     * <p>
     * The constructor loads environment variables using dotenv (if present)
     * and falls back to system environment variables. It validates that the
     * mandatory configuration (BASE_URL, API_USER and API_PASSWORD) is present.
     * </p>
     *
     * @param client configured HttpClient to use for requests
     * @throws IllegalStateException if required configuration values are missing
     */
    private VicidialClientSingleton(HttpClient client) {
        // Configure the client with a timeout to avoid infinite blocking.
        this.client = client;

        // Load variables from .env if present, otherwise use system environment
        // variables.
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();

        String envBase = dotenv.get("BASE_URL");
        String envUser = dotenv.get("API_USER");
        String envPass = dotenv.get("API_PASSWORD");
        String envServerIp = dotenv.get("SERVER_IP");
        String envTemplateId = dotenv.get("TEMPLATE_ID");
        String envServerUrl = dotenv.get("SERVER_URL");

        String sysBase = System.getenv("BASE_URL");
        String sysUser = System.getenv("API_USER");
        String sysPass = System.getenv("API_PASSWORD");
        String sysServerIp = System.getenv("SERVER_IP");
        String sysTemplateId = System.getenv("TEMPLATE_ID");
        String sysServerUrl = System.getenv("SERVER_URL");

        this.baseUrl = (envBase != null && !envBase.isBlank()) ? envBase : sysBase;
        this.apiUser = (envUser != null && !envUser.isBlank()) ? envUser : sysUser;
        this.apiPass = (envPass != null && !envPass.isBlank()) ? envPass : sysPass;
        this.serverIp = (envServerIp != null && !envServerIp.isBlank()) ? envServerIp : sysServerIp;
        this.templateId = (envTemplateId != null && !envTemplateId.isBlank()) ? envTemplateId : sysTemplateId;
        this.serverUrl = (envServerUrl != null && !envServerUrl.isBlank()) ? envServerUrl : sysServerUrl;

        if (this.baseUrl == null || this.apiUser == null || this.apiPass == null) {
            throw new IllegalStateException(
                    "Missing credentials: define BASE_URL, API_USER and API_PASSWORD in .env or environment variables.");
        }
    }

    /**
     * Returns the singleton instance, creating it if necessary.
     *
     * <p>
     * The instance is lazily initialized with a default HttpClient configured
     * with a 10 second connection timeout and a global cookie manager.
     * </p>
     *
     * @return the singleton VicidialClientSingleton instance
     */
    public static VicidialClientSingleton getInstance() {
        if (VicidialClientSingleton.instance == null) {
            // Add a global CookieManager to preserve PHPSESSID between methods
            CookieManager globalCookieManager = new CookieManager();

            HttpClient client = HttpClient.newBuilder()
                    .cookieHandler(globalCookieManager)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            VicidialClientSingleton.instance = new VicidialClientSingleton(client);
        }

        return VicidialClientSingleton.instance;
    }

    /**
     * Builds a full API URL for a given Vicidial function name.
     *
     * <p>
     * The returned URL already includes source, user and pass query parameters.
     * </p>
     *
     * @param functionName function name expected by the Vicidial API (e.g.
     *                     "add_user")
     * @return a full URL string ready to be extended with function-specific
     *         parameters
     */
    private String buildApiUrl(String functionName) {
        // Build the URL using the provided function name
        return new StringBuilder()
                .append(baseUrl)
                .append("?source=").append(source)
                .append("&user=").append(apiUser)
                .append("&pass=").append(apiPass)
                .append("&function=").append(functionName)
                .toString();
    }

    /**
     * Performs a synchronous GET request to retrieve all campaigns.
     *
     * @return The body of the API response (JSON/XML) as a String.
     * @throws IOException          If an I/O (network) error occurs.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public String getCampaigns() throws IOException, InterruptedException {
        String url = buildApiUrl("campaigns_list");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15)) // Request timeout
                .build();

        // Synchronous execution: the thread blocks here until a response is received.
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Basic status code handling
        if (response.statusCode() != 200) {
            throw new IOException("Error calling the Vicidial API. Status code: " + response.statusCode());
        }

        return response.body();
    }

    /**
     * Helper method to execute an HTTP call (to avoid duplicating code).
     */
    private String executeApiCall(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Error calling the API. Status code: " + response.statusCode());
        }
        return response.body();
    }

    /**
     * Safely encodes a string for use in URL query parameters.
     * Treats null as empty string to avoid URLEncoder throwing NPE.
     */
    private String safeEncode(String s) {
        return URLEncoder.encode((s == null) ? "" : s, StandardCharsets.UTF_8);
    }

    /**
     * Obtains detailed information for a lead.
     *
     * @param leadId unique identifier of the lead.
     * @return The API response body for the lead.
     * @throws IOException          If an I/O (network) error occurs.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public String getLeadInfo(String leadId) throws IOException, InterruptedException {

        String url = buildApiUrl("lead_all_info") + "&lead_id=" + leadId;

        String response = executeApiCall(url);
        return response;
    }

    /**
     * Creates a new contact based on an existing one and places it in a specific
     * list. You can overwrite comments
     * and/or the email address.
     *
     * @param leadId   Unique identifier of the lead to duplicate.
     * @param listId   Unique identifier of the list where the new lead will be
     *                 placed.
     * @param comments Notes to be added (Default "").
     * @param email    Email to overwrite (Default "").
     * @throws IOException          If an I/O (network) error occurs.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public void DuplicateLeadInList(String leadId, String listId, String comments, String email)
            throws IOException, InterruptedException {
        // Build the URL
        String url = buildApiUrl("lead_all_info") + "&lead_id=" + leadId;

        // Lookup the contact
        System.out.println(Ansi.AUTO.text("@|yellow Searching lead details for ID: " + leadId + "...|@"));
        String response = executeApiCall(url);

        // Verify that it exists
        if (response.isEmpty()) {
            System.out.println(Ansi.AUTO.text("@|red Lead not found for ID: " + leadId + "...|@"));
            throw new InterruptedException("Lead not found for ID: " + leadId);
        }

        // Create Lead object
        System.out.println(Ansi.AUTO.text("@|green Lead found for ID: " + leadId + "...|@"));
        LeadModel contactInfo = new LeadModel(response);

        // Modify the lead if necessary
        System.out.println(Ansi.AUTO.text("@|blue Changing key info ...|@"));
        if (!comments.isEmpty()) {
            contactInfo.setComments(comments);
        }
        if (!email.isEmpty()) {
            contactInfo.setEmail(email);
        }

        // Create URL for the new lead
        String urlLead = buildApiUrl("add_lead") +
                "&phone_number=" + contactInfo.getPhone_number() +
                "&phone_code=1" +
                "&list_id=" + listId +
                "&first_name=" + safeEncode(contactInfo.getFirst_name()) +
                "&last_name=" + safeEncode(contactInfo.getLast_name()) +
                "&address1=" + safeEncode(contactInfo.getAddress1()) +
                "&address2=" + safeEncode(contactInfo.getAddress2()) +
                "&address3=" + safeEncode(contactInfo.getAddress3()) +
                "&city=" + safeEncode(contactInfo.getCity()) +
                "&state=" + safeEncode(contactInfo.getState()) +
                "&postal_code=" + safeEncode(contactInfo.getPostal_code()) +
                "&alt_phone=" + safeEncode(contactInfo.getAlt_phone()) +
                "&email=" + safeEncode(contactInfo.getEmail()) +
                "&comments=" + safeEncode(contactInfo.getComments());

        // Make create request
        System.out.println(Ansi.AUTO.text("@|blue Creating New lead in List Id: " + listId + "...|@"));
        String LeadResponse = executeApiCall(urlLead);

        if (LeadResponse.isEmpty()) {
            throw new InterruptedException("Fail while creating new Lead (Already Exists).");
        }

        // Split the response by '|'
        String NewLeadId = LeadResponse.split("\\|")[2];
        System.out.println(
                Ansi.AUTO.text("@|green New lead Created inside list " + listId + "\nLead ID: " + NewLeadId + "|@"));

        return;
    }

    /**
     * Updates a Vicidial User, overwriting name and/or password.
     *
     * @param ID       User identifier
     * @param name     New name (Default "").
     * @param password New password (Default "").
     * @throws IOException          When an error occurs updating the User.
     * @throws InterruptedException When the thread is interrupted while waiting.
     */
    public void updateUser(String ID, String name, String password) throws IOException, InterruptedException {
        // Build the URL
        String userUrl = buildApiUrl("update_user") + "&agent_user=" + ID;

        if (!name.isEmpty()) {
            userUrl = userUrl + String.format("&agent_full_name=%s", URLEncoder.encode(name, StandardCharsets.UTF_8));
        }
        if (!password.isEmpty()) {
            userUrl = userUrl + String.format("&agent_pass=%s", password);
        }

        String response = executeApiCall(userUrl);

        if (response.contains("ERROR:")) {
            throw new IOException("Error while updating " + ID);
        }

        return;
    }

    /**
     * Updates the password of a Vicidial Phone.
     *
     * @param ID       Phone identifier.
     * @param password New password (Default "").
     * @throws IOException          When an error occurs updating the Phone, or when
     *                              password is not provided.
     * @throws InterruptedException When the thread is interrupted while waiting.
     */
    public void updatePhone(String ID, String password) throws IOException, InterruptedException {
        // Build the URL
        String phoneUrl = buildApiUrl("update_phone") +
                "&extension=" + ID +
                "&server_ip=" + this.serverIp +
                "&phone_pass=" + password;

        if (password.isEmpty()) {
            throw new IOException("Error: No password given.");
        }

        String response = executeApiCall(phoneUrl);

        if (response.contains("ERROR:")) {
            throw new IOException("Error while updating " + ID);
        }

        return;
    }

    /**
     * Creates a User for a given campaign using a Usergroup ID.
     *
     * @param ID        Unique identifier for the User; will be used as Login.
     * @param password  Password for the user, used for Login.
     * @param name      User's display name, used in reports.
     * @param userGroup Identifier of the Usergroup, used to assign the user to a
     *                  campaign.
     * @throws IOException          When an error occurs creating the User.
     * @throws InterruptedException When the thread is interrupted while waiting.
     */
    public void createUser(String ID, String password, String name, String userGroup)
            throws IOException, InterruptedException {
        String userURL = buildApiUrl("add_user") + "&agent_user=" + ID + "&agent_pass=" + password
                + "&hotkeys_active=1&closer_default_blended=1&agent_user_level=1&agent_full_name=" + name
                + "&agent_user_group=" + userGroup;

        String response = executeApiCall(userURL);

        if (response.contains("ERROR:")) {
            throw new IOException("Error while creating user " + ID + ":\n" + response);
        }

        return;
    }

    /**
     * Creates a Phone for a User.
     *
     * @param ID       Unique identifier for the Phone, used as Login.
     * @param password Password for the Phone, used for Login.
     * @throws IOException          When an error occurs creating the Phone.
     * @throws InterruptedException When the thread is interrupted while waiting.
     */
    public void createPhone(String ID, String password) throws IOException, InterruptedException {
        if (this.serverIp == null || this.serverIp.isBlank()) {
            throw new IOException("Missing configuration: define SERVER_IP in .env or as an environment variable.");
        }
        if (this.templateId == null || this.templateId.isBlank()) {
            throw new IOException("Missing configuration: define TEMPLATE_ID in .env or as an environment variable.");
        }

        String cid = "0000000000";
        String phoneURL = buildApiUrl("add_phone") +
                "&extension=" + ID +
                "&dialplan_number=" + ID +
                "&voicemail_id=" + ID +
                "&phone_login=" + ID +
                "&phone_pass=" + password +
                "&server_ip=" + this.serverIp +
                "&protocol=SIP" +
                "&registration_password=" + password +
                "&phone_full_name=" + ID +
                "&local_gmt=-5.00" +
                "&is_webphone=Y" +
                "&webphone_auto_answer=Y" +
                "&outbound_cid=" + cid +
                "&template_id=" + templateId;

        String response = executeApiCall(phoneURL);

        if (response.contains("ERROR:")) {
            throw new IOException("Error while creating phone " + ID + ":\n" + response);
        }

        return;

    }

    /**
     * Performs an authenticated GET request against a provided URL using basic
     * auth.
     *
     * @param URL full URL to call
     * @return response body as text
     * @throws IOException          If an I/O (network) error occurs.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public String getFromWeb(String URL) throws IOException, InterruptedException {

        String originalInput = apiUser + ":" + apiPass;
        Base64.Encoder encoder = Base64.getEncoder();
        String encodedString = encoder.encodeToString(originalInput.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET()
                .header("Authorization", "Basic " + encodedString)
                .header("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .timeout(Duration.ofSeconds(15)) // Request timeout
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Error calling the API. Status code: " + response.statusCode());
        }

        return response.body();
    }

    /**
     * Removes a DID (Direct Inward Dial) entry using the configured serverUrl.
     *
     * @param id numeric DID identifier to be removed
     * @throws IOException          If an I/O (network) error occurs.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public void removeDID(int id) throws IOException, InterruptedException {
        String originalInput = apiUser + ":" + apiPass;
        Base64.Encoder encoder = Base64.getEncoder();
        String encodedString = encoder.encodeToString(originalInput.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "?ADD=6311&did_id=" + id + "&CoNfIrM=YES"))
                .GET()
                .header("Authorization", "Basic " + encodedString)
                .header("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .timeout(Duration.ofSeconds(15)) // Request timeout
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Error calling the API. Status code: " + response.statusCode());
        }
    }

    /**
     * Update the campaign's active status using the Non-Agent API.
     *
     * @param campaignId the campaign identifier
     * @param active     'Y' to activate or 'N' to deactivate
     * @throws IOException          if a network or I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public void updateCampaignStatus(String campaignId, String active) throws IOException, InterruptedException {
        // &campaign_id=TESTOUT&active=N
        String url = buildApiUrl("update_campaign") + "&campaign_id=" + campaignId + "&active=" + active;

        String response = executeApiCall(url);

        if (response.contains("ERROR")) {
            System.err.println(Ansi.AUTO.text("❌ @|red Error while updating the campaign: .|@" + response));

        }
        if (response.contains("NOTICE")) {
            System.err.println(Ansi.AUTO.text("⚠️ @|yellow No updates defined on this campaign.|@"));

        }
        if (response.contains("SUCCESS")) {
            System.err.println(Ansi.AUTO.text("✅ @|green Campaign has been updated.|@"));
        }
    }

    /**
     * Create a new user group via the admin web UI by simulating form submission.
     *
     * @param groupName   the desired group identifier (will be lowercased and
     *                    trimmed)
     * @param description the human-readable name or description for the group
     * @return the cleaned group id created on success
     * @throws IOException          if a network I/O error occurs
     * @throws InterruptedException if the operation is interrupted or the group
     *                              exists
     */
    public String createUserGroup(String groupName, String description) throws IOException, InterruptedException {
        // 1. Configure the client with cookie management to maintain the PHPSESSID
        // session
        CookieManager cookieManager = new CookieManager();
        HttpClient sessionClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 2. Prepare credentials and sanitize the group ID
        String cleanGroupId = groupName.toLowerCase().trim();
        String auth = Base64.getEncoder().encodeToString((apiUser + ":" + apiPass).getBytes(StandardCharsets.UTF_8));
        // STEP 1: Simulate form access (GET) to initialize the session on the server
        HttpRequest step1 = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "?ADD=111111"))
                .header("Authorization", "Basic " + auth)
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        sessionClient.send(step1, HttpResponse.BodyHandlers.ofString());

        // STEP 2: Prepare the POST data
        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("ADD", "211111"); // action: process insertion
        formData.put("DB", "0"); // database (required by admin.php)
        formData.put("user_group", cleanGroupId); // group ID
        formData.put("group_name", description); // description
        formData.put("SUBMIT", "SUBMIT"); // simulate submit button

        String formBody = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        // STEP 3: Send the POST request
        HttpRequest step2 = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl)) // sent to admin.php
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", serverUrl + "?ADD=111111") // required by Vicidial
                .header("User-Agent", "Mozilla/5.0")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = sessionClient.send(step2, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        // 3. Validate response
        if (responseBody.contains("USER GROUP ADDED") || responseBody.contains("has been added")) {
            System.out.println("✅ Success: The group '" + cleanGroupId + "' has been successfully created.");
            return cleanGroupId;
        } else if (responseBody.contains("USER GROUP NOT ADDED")) {
            System.err.println("⚠️ Error: The group '" + cleanGroupId + "' already exists.");
            System.out.println("name: " + groupName);
            System.out.println("description: " + description);
            throw new InterruptedException("❌ Creation failed. The Group already exists.");
        } else {
            System.err.println("❌ Creation failed. The server rejected the request.");
            throw new InterruptedException("❌ Creation failed. The server rejected the request.");
        }
    }

    /**
     * Retrieve the list of user groups by parsing the admin HTML page.
     *
     * @return a List of UserGroupModel objects or null if no records are found
     * @throws IOException          if a network I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public List<UserGroupModel> listUserGroups() throws IOException, InterruptedException {

        List<UserGroupModel> groups = new ArrayList<>();
        String auth = Base64.getEncoder().encodeToString((apiUser + ":" + apiPass).getBytes(StandardCharsets.UTF_8));

        // URL used to list user groups
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "?ADD=100000"))
                .header("Authorization", "Basic " + auth)
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Document doc = Jsoup.parse(response.body());

        // Select the rows that contain the group data
        Elements rows = doc.select("tr.records_list_x, tr.records_list_y");

        for (Element row : rows) {
            Elements cols = row.select("td");

            // Table structure:
            // 0: USER GROUP (ID) | 1: GROUP NAME | 2: USERS | 3: ACTIVE
            if (cols.size() >= 4) {
                groups.add(new UserGroupModel(
                        cols.get(0).text().trim(),
                        cols.get(1).text().trim(),
                        cols.get(3).text().trim() // ACTIVE is at position 3 according to the HTML
                ));
            }
        }

        // Display results to console
        if (groups.isEmpty()) {
            System.out.println("No records found in the table.");
            return null;
        } else {
            return groups;
        }
    }

    /**
     * Check whether the provided user group exists in the system
     * (case-insensitive).
     *
     * @param groupName the group name to validate
     * @return true if the group exists, false otherwise
     * @throws IOException          if a network I/O error occurs while listing
     *                              groups
     * @throws InterruptedException if the operation is interrupted
     */
    public Boolean isValidUserGroup(String groupName) throws IOException, InterruptedException {

        if (groupName.isBlank()) {
            return false;
        }

        List<UserGroupModel> groups = this.listUserGroups();

        for (UserGroupModel userGroupModel : groups) {
            if (userGroupModel.getId().toLowerCase().equals(groupName.toLowerCase().trim())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Create a CID group by submitting the admin form via HTTP session simulation.
     *
     * @param CidName        the desired CID group name
     * @param cidDescription description or notes for the CID group
     * @return the cleaned CID group id on success
     * @throws IOException          if a network I/O error occurs
     * @throws InterruptedException if the operation is interrupted or the CID
     *                              exists
     */
    public String createCIDGroup(String CidName, String cidDescription) throws IOException, InterruptedException {

        // 1. Configure the client with cookie management to maintain the PHPSESSID
        // session
        CookieManager cookieManager = new CookieManager();
        HttpClient sessionClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 2. Prepare credentials and sanitize the group ID
        String cleanGroupId = CidName.toLowerCase().trim().replaceAll(" ", "%20") + "CID";
        String auth = Base64.getEncoder().encodeToString((apiUser + ":" + apiPass).getBytes(StandardCharsets.UTF_8));

        // STEP 1: Simulate form access (GET) to initialize the session on the server
        HttpRequest step1 = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "?ADD=196111111111"))
                .header("Authorization", "Basic " + auth)
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        sessionClient.send(step1, HttpResponse.BodyHandlers.ofString());

        // STEP 2: Prepare the POST data
        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("ADD", "296111111111"); // action: process insertion
        formData.put("DB", "0"); // database (required by admin.php)
        formData.put("cid_group_id", cleanGroupId); // group ID
        formData.put("cid_group_notes", cidDescription); // description
        formData.put("cid_group_type", "NONE");
        formData.put("user_group", "---ALL---");
        formData.put("SUBMIT", "SUBMIT"); // simulate submit button

        String formBody = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        // STEP 3: Send the POST request
        HttpRequest step2 = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl)) // sent to admin.php
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", serverUrl + "?ADD=196111111111") // required by Vicidial
                .header("User-Agent", "Mozilla/5.0")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = sessionClient.send(step2, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        // 3. Validate response
        if (responseBody.contains("CID GROUP ADDED") || responseBody.contains("has been added")) {
            System.out.println("✅ Success: The CID group '" + cleanGroupId + "' has been successfully created.");
            return cleanGroupId;
        } else if (responseBody.contains("CID GROUP NOT ADDED")) {
            System.err.println("⚠️ Error: The CID group '" + cleanGroupId + "' already exists.");
            throw new InterruptedException("❌ Creation failed. The CID already exists.");
        } else {
            System.err.println("❌ Creation failed. The server rejected the request.");
            throw new InterruptedException("❌ Creation failed. The server rejected the request.");
        }

    }

    /**
     * Create an inbound group by simulating the admin form submission.
     *
     * @param inboundName        the inbound group name to create
     * @param inboundDescription the display description for the inbound group
     * @param groupID            the user group id to associate ("---ALL---" for
     *                           all)
     * @return the cleaned inbound group id on success
     * @throws IOException          if a network I/O error occurs
     * @throws InterruptedException if the operation is interrupted or the group
     *                              exists
     */
    public String createInboundGroup(String inboundName, String inboundDescription, String groupID)
            throws IOException, InterruptedException {
        if (groupID.isEmpty()) {
            groupID = "---ALL---";
        }

        // 1. Configure the client with cookie management to maintain the PHPSESSID
        // session
        CookieManager cookieManager = new CookieManager();
        HttpClient sessionClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 2. Prepare credentials and sanitize the inbound group ID
        String cleanGroupId = inboundName.toLowerCase().trim().replaceAll(" ", "%20") + "Inb";
        String auth = Base64.getEncoder().encodeToString((apiUser + ":" + apiPass).getBytes(StandardCharsets.UTF_8));

        // STEP 1: Simulate form access (GET) to initialize the session on the server
        HttpRequest step1 = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "?ADD=1111"))
                .header("Authorization", "Basic " + auth)
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        sessionClient.send(step1, HttpResponse.BodyHandlers.ofString());

        // STEP 2: Prepare the POST data
        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("ADD", "2111"); // action: process insertion
        formData.put("DB", "0"); // Base de datos (requerido por admin.php)
        formData.put("group_id", cleanGroupId);
        formData.put("group_name", inboundDescription);
        formData.put("group_color", "#FF00FF");
        formData.put("active", "Y");
        formData.put("user_group", groupID);
        formData.put("web_form_address", "");
        formData.put("voicemail_ext", "");
        formData.put("next_agent_call", "oldest_call_finish");
        formData.put("fronter_display", "Y");
        formData.put("script_id", "NONE");
        formData.put("get_call_launch", "NONE");
        formData.put("group_handling", "PHONE");
        formData.put("SUBMIT", "SUBMIT");

        String formBody = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        // STEP 3: Send the POST request
        HttpRequest step2 = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl)) // sent to admin.php
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", serverUrl + "?ADD=196111111111") // required by Vicidial
                .header("User-Agent", "Mozilla/5.0")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = sessionClient.send(step2, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        // 3. Validate response
        if (responseBody.contains("GROUP ADDED") || responseBody.contains("has been added")) {
            System.out.println("✅ Success: The inbound group '" + cleanGroupId + "' has been successfully created.");
            return cleanGroupId;
        } else if (responseBody.contains("GROUP NOT ADDED")) {
            System.err.println("⚠️ Error: The inbound group '" + cleanGroupId + "' already exists.");
            throw new InterruptedException("❌ Creation failed. The inbound group already exists.");
        } else {
            System.err.println("❌ Creation failed. The server rejected the request.");
            throw new InterruptedException("❌ Creation failed. The server rejected the request.");
        }
    }

    /**
     * Retrieves all available lists from the Vicidial system.
     *
     * <p>
     * This method performs an HTTP GET request to the Vicidial admin interface
     * to fetch the list of all lists. It parses the HTML response using Jsoup
     * and extracts relevant information such as list ID, name, description,
     * active status, and associated campaign into ListModel objects.
     * </p>
     *
     * @return a list of ListModel objects representing all available lists
     * @throws IOException          if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the thread is interrupted while waiting for
     *                              the response
     */
    public List<ListModel> getAllLists() throws IOException, InterruptedException {
        String LIST_URL = "https://cloud.yourserviceva.net/vicidial/admin.php?ADD=100";

        String response = this.getFromWeb(LIST_URL);
        List<ListModel> lists = new ArrayList<>();
        Document doc = Jsoup.parse(response);

        // Select the rows that have the record list classes
        Elements rows = doc.select("tr.records_list_x, tr.records_list_y");

        for (Element row : rows) {
            Elements cols = row.select("td");

            if (cols.size() >= 9) {
                // Extract the text, trimming extra whitespace
                String id = cols.get(0).text().trim();
                String name = cols.get(1).text().trim();
                String description = cols.get(2).text().trim();
                String active = cols.get(6).text().trim();
                String campaign = cols.get(8).text().trim();

                lists.add(new ListModel(Integer.parseInt(id), name, description, active, campaign));
            }
        }

        return lists;
    }

    /**
     * Determines the next available list ID based on the current lists.
     *
     * <p>
     * This method finds the list with the highest ID from the provided list
     * and returns the next sequential ID. If the list is empty, it returns -1.
     * </p>
     *
     * @param currentLists the list of existing ListModel objects
     * @return the next available list ID, or -1 if the input list is empty
     */
    public int getNextListId(List<ListModel> currentLists) {

        if (currentLists.size() < 1) {
            return -1;
            // TODO : Create a custom List Error "unable to figure the id of the list".
        }

        ListModel currentId = Collections.max(currentLists, Comparator.comparingInt(ListModel::getListId));

        return currentId.getListId() + 1;
    }

    /**
     * Creates a new list in the Vicidial system.
     *
     * <p>
     * This method creates a new list with the specified name, description, and
     * campaign ID.
     * If the listId is null, it automatically determines the next available list ID
     * by
     * retrieving all existing lists and finding the highest ID, then incrementing
     * it.
     * </p>
     *
     * @param listId          the unique identifier for the list; if null, the next
     *                        available ID will be used
     * @param listName        the name of the list to be created
     * @param listDescription the description of the list
     * @param campaignId      the ID of the campaign to associate the list with
     * @return the ID of the created list, or null if creation failed
     * @throws InterruptedException if the list already exists or if an error occurs
     *                              during creation
     */
    public String createList(String listId, String listName, String listDescription, String campaignId) {

        try {
            if (listId == null) {
                List<ListModel> currentLists = this.getAllLists();
                listId = String.valueOf(this.getNextListId(currentLists));
            }

            String API_URL = this.buildApiUrl("add_list") + "&list_id=" + listId
                    + "&list_name=" + URLEncoder.encode(listName, StandardCharsets.UTF_8)
                    + "&campaign_id=" + URLEncoder.encode(campaignId, StandardCharsets.UTF_8)
                    + "&list_description=" + URLEncoder.encode(listDescription, StandardCharsets.UTF_8);

            // Make create request
            System.out.println(Ansi.AUTO.text("@|blue Creating New list: " + listId + "...|@"));
            String response = this.executeApiCall(API_URL);

            if (response.contains("ALREADY")) {
                System.out.println(Ansi.AUTO.text("@|red 🔴 ERROR: add_list LIST ALREADY EXISTS |@"));
                throw new InterruptedException("Fail while creating new List (Already Exists).");
            }

            if (response.contains("SUCCESS")) {
                System.out
                        .println(Ansi.AUTO.text("@|green ✅ SUCCESS: add_list LIST HAS BEEN ADDED - " + listId + "|@"));
            }

            return listId;
        } catch (Exception e) {
            // TODO: handle exception
            return null;
        }

    }

    /**
     * Copy an existing campaign by simulating the admin copy form and submitting
     * it.
     *
     * @param campaignInfo information required to build the new campaign (id and
     *                     name)
     * @return the cleaned campaign id if created successfully, otherwise empty
     *         string
     * @throws IOException          if a network I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public String copyExistingCampaign(CampaignBuildInfo campaignInfo) throws IOException, InterruptedException {
        final String FORM_CODE_SIMULATE_FORM = "12"; // form page ADD code (simulate opening the form)
        final String FORM_CODE_SIMULATE_ACTION = "20"; // action ADD code (form action for copying campaign)
        final String FORM_CODE_SIMULATE_PERMISSION = "193111111111"; // permission ADD code (adds permissions to view
                                                                     // info)
        final String SOURCE_CAMPAIGN_ID = "Test";

        Map<String, String> formData = new LinkedHashMap<>();

        String cleanedCampaignId = campaignInfo.getCampaignID().substring(0, 1).toUpperCase()
                + campaignInfo.getCampaignID().substring(1);
        ;

        // 1.1 Activate simulate form ADD=12
        CookieManager cookieManager = new CookieManager();
        HttpClient sessionClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String auth = Base64.getEncoder().encodeToString((apiUser + ":" + apiPass).getBytes(StandardCharsets.UTF_8));
        HttpRequest step1 = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "?ADD=" + FORM_CODE_SIMULATE_FORM))
                .header("Authorization", "Basic " + auth)
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        sessionClient.send(step1, HttpResponse.BodyHandlers.ofString());

        // 1.2 Simulate copy using ADD=20
        formData.put("ADD", FORM_CODE_SIMULATE_ACTION); // action: process insertion
        formData.put("DB", "0"); // database (required by admin.php)
        formData.put("campaign_id", cleanedCampaignId);
        formData.put("campaign_name", campaignInfo.getCampaignName() + "Camp");
        formData.put("source_campaign_id", SOURCE_CAMPAIGN_ID);
        formData.put("SUBMIT", "SUBMIT"); // Simulación de clic en botón

        String formBody = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        // Send the POST request
        HttpRequest step2 = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl)) // sent to admin.php
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", serverUrl + "?ADD=" + FORM_CODE_SIMULATE_PERMISSION) // required by Vicidial
                .header("User-Agent", "Mozilla/5.0")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = sessionClient.send(step2, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        // 4. Response handling
        if (responseBody.contains("CAMPAIGN NOT ADDED")) {
            System.err.println(
                    Ansi.AUTO.text("@|red 🔴 ERROR: CAMPAIGN " + cleanedCampaignId + " HAS NOT BEEN ADDED. |@"));
        } else {
            System.out.println(
                    Ansi.AUTO.text("@|green ✅ SUCCESS: CAMPAIGN " + cleanedCampaignId + " HAS BEEN ADDED. |@"));

            return cleanedCampaignId;
        }

        return "";
    }

    /**
     * Apply user group, inbound group and CID parameters to an existing campaign
     * using the Vicidial Non-Agent API.
     *
     * @param campaignId       The campaign ID to modify (e.g. "PABLO0").
     * @param userGroupId      The user group ID to assign (e.g. "pablo").
     * @param inboundGroupId   The inbound group ID to assign (e.g. "pabloinb").
     * @param cidGroupOrNumber The CID number or CID group ID to set for the
     *                         campaign.
     * @return true if the update succeeded, false otherwise.
     * @throws IOException          If a network error occurs.
     * @throws InterruptedException If the execution is interrupted.
     */
    public boolean configureCampaignGroups(String campaignId, String userGroupId, String inboundGroupId,
            String cidGroupOrNumber)
            throws IOException, InterruptedException {

        String closerCampaignsFormatted = inboundGroupId.trim();

        // 3. Construir la URL con los parámetros de actualización de la Non-Agent API
        String urlBuilder = this.buildApiUrl("update_campaign") +
                "&active=Y" +
                "&campaign_id=" + campaignId +
                "&campaign_cid=" + cidGroupOrNumber.trim() +
                "&user_group=" + userGroupId +
                "&closer_campaigns=" + closerCampaignsFormatted;

        System.out.println(Ansi.AUTO.text("@|blue ⚙️ Applying groups to campaign " + campaignId + "...|@"));

        // 4. Ejecutar la llamada a la API
        String response = executeApiCall(urlBuilder.toString());

        // 5. Evaluar la respuesta del backend
        System.out.println("   API response: " + response.trim());

        if (response.contains("SUCCESS")) {
            System.out.println(Ansi.AUTO
                    .text("@|green ✅ Success: Groups successfully applied to campaign " + campaignId + ".|@"));
            return true;
        } else {
            System.err.println(Ansi.AUTO.text("❌ @|red API error updating groups: |@" + response));
            return false;
        }
    }

    /**
     * Reads back the stored settings of a campaign through the Non-Agent API.
     *
     * @param campaignId the campaign identifier to inspect
     * @return the raw API response body
     * @throws IOException          If a network error occurs.
     * @throws InterruptedException If the execution is interrupted.
     */
    public String showCampaign(String campaignId) throws IOException, InterruptedException {
        return executeApiCall(this.buildApiUrl("show_campaign") + "&campaign_id=" + campaignId);
    }
}
