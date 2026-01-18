package dev.pablo.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import dev.pablo.models.LeadModel;
import io.github.cdimascio.dotenv.Dotenv;
import picocli.CommandLine.Help.Ansi;

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

    private VicidialClientSingleton(HttpClient client) {
        // Configure the client with a timeout to avoid infinite blocking.
        this.client = client;

        // Load variables from .env if present, otherwise use system environment variables.
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();

        String envBase = dotenv.get("BASE_URL");
        String envUser = dotenv.get("API_USER");
        String envPass = dotenv.get("API_PASSWORD");
        String serverIp = dotenv.get("SERVER_IP");
        String templateId = dotenv.get("TEMPLATE_ID");

        String sysBase = System.getenv("BASE_URL");
        String sysUser = System.getenv("API_USER");
        String sysPass = System.getenv("API_PASSWORD");

        this.baseUrl = (envBase != null && !envBase.isBlank()) ? envBase : sysBase;
        this.apiUser = (envUser != null && !envUser.isBlank()) ? envUser : sysUser;
        this.apiPass = (envPass != null && !envPass.isBlank()) ? envPass : sysPass;
        this.serverIp = (serverIp != null && !serverIp.isBlank()) ? serverIp : serverIp;
        this.templateId = (templateId != null && !templateId.isBlank()) ? templateId : templateId;

        if (this.baseUrl == null || this.apiUser == null || this.apiPass == null) {
            throw new IllegalStateException(
                "Missing credentials: define BASE_URL, API_USER and API_PASSWORD in .env or environment variables.");
        }
    }

    public static VicidialClientSingleton getInstance() {
        if (VicidialClientSingleton.instance == null) {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            VicidialClientSingleton.instance = new VicidialClientSingleton(client);
        }

        return VicidialClientSingleton.instance;
    }

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
     * Creates a new contact based on an existing one and places it in a specific list. You can overwrite comments
     * and/or the email address.
     *
     * @param leadId   Unique identifier of the lead to duplicate.
     * @param listId   Unique identifier of the list where the new lead will be placed.
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
                "&first_name=" + URLEncoder.encode(contactInfo.getFirst_name(), StandardCharsets.UTF_8) +
                "&last_name=" + URLEncoder.encode(contactInfo.getLast_name(), StandardCharsets.UTF_8) +
                "&address1=" + URLEncoder.encode(contactInfo.getAddress1(), StandardCharsets.UTF_8) +
                "&address2=" + URLEncoder.encode(contactInfo.getAddress2(), StandardCharsets.UTF_8) +
                "&address3=" + URLEncoder.encode(contactInfo.getAddress3(), StandardCharsets.UTF_8) +
                "&city=" + URLEncoder.encode(contactInfo.getCity(), StandardCharsets.UTF_8) +
                "&state=" + contactInfo.getState() +
                "&alt_phone=" + contactInfo.getAlt_phone() +
                "&email=" + contactInfo.getEmail() +
                "&comments=" + URLEncoder.encode(contactInfo.getComments(), StandardCharsets.UTF_8);

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
     * @throws IOException          When an error occurs updating the Phone, or when password is not provided.
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
     * @param userGroup Identifier of the Usergroup, used to assign the user to a campaign.
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
                "&template_id=" + templateId; // optional -> env

        String response = executeApiCall(phoneURL);

        if (response.contains("ERROR:")) {
            throw new IOException("Error while creating phone " + ID + ":\n" + response);
        }

        return;

    }

    public String getFromWeb(String URL) throws IOException, InterruptedException{
        

        String originalInput = apiUser +":"+apiPass;
        Base64.Encoder encoder = Base64.getEncoder();
        String encodedString = encoder.encodeToString(originalInput.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET()
                .header("Authorization", "Basic " + encodedString)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .timeout(Duration.ofSeconds(15)) // Request timeout
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Error calling the API. Status code: " + response.statusCode());
        }
        
        return response.body();
    }

}
