package dev.pablo.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import dev.pablo.models.LeadModel;
import io.github.cdimascio.dotenv.Dotenv;
import picocli.CommandLine.Help.Ansi;

public class VicidialClientSingleton {

    public static VicidialClientSingleton instance = null;
    private HttpClient client;
    private final String baseUrl;
    // Configuración de credenciales (se cargan desde .env o variables de entorno)
    private final String apiUser;
    private final String apiPass;
    private final String source = "java";
    private final String serverIp;

    private VicidialClientSingleton(HttpClient client) {
        // Configuramos el cliente con un timeout para evitar bloqueos infinitos.
        this.client = client;

        // Cargamos variables desde .env si existe, y si no, usamos las variables de
        // entorno del sistema.
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();

        String envBase = dotenv.get("BASE_URL");
        String envUser = dotenv.get("API_USER");
        String envPass = dotenv.get("API_PASSWORD");
        String serverIp = dotenv.get("SERVER_IP");

        String sysBase = System.getenv("BASE_URL");
        String sysUser = System.getenv("API_USER");
        String sysPass = System.getenv("API_PASSWORD");

        this.baseUrl = (envBase != null && !envBase.isBlank()) ? envBase : sysBase;
        this.apiUser = (envUser != null && !envUser.isBlank()) ? envUser : sysUser;
        this.apiPass = (envPass != null && !envPass.isBlank()) ? envPass : sysPass;
        this.serverIp = (serverIp != null && !serverIp.isBlank()) ? serverIp : serverIp;

        if (this.baseUrl == null || this.apiUser == null || this.apiPass == null) {
            throw new IllegalStateException(
                    "Faltan credenciales: define BASE_URL, API_USER y API_PASSWORD en .env o en variables de entorno.");
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
        // Construye la URL con la función get_campaign_list
        return new StringBuilder()
                .append(baseUrl)
                .append("?source=").append(source)
                .append("&user=").append(apiUser)
                .append("&pass=").append(apiPass)
                .append("&function=").append(functionName)
                .toString();
    }

    /**
     * Realiza una petición GET SÍNCRONA para obtener todas las campañas.
     * 
     * @return El cuerpo de la respuesta de la API (JSON/XML) como String.
     * @throws IOException          Si ocurre un error de entrada/salida (red).
     * @throws InterruptedException Si el hilo es interrumpido durante la espera.
     */
    public String getCampaigns() throws IOException, InterruptedException {
        String url = buildApiUrl("campaigns_list");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15)) // Timeout de petición
                .build();

        // Ejecución SÍNCRONA: El hilo se bloquea aquí hasta que se recibe la respuesta.
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Manejo de códigos de estado básicos
        if (response.statusCode() != 200) {
            throw new IOException("Error al llamar a la API de Vicidial. Código de estado: " + response.statusCode());
        }

        return response.body();
    }

    /**
     * Método auxiliar para ejecutar la llamada HTTP (para evitar duplicación de
     * código)
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
            throw new IOException("Error al llamar a la API. Código de estado: " + response.statusCode());
        }
        return response.body();
    }

    /**
     * Obtiene informacion detallada de una Campaña, extrayendo las listas activas.
     * Utiliza 'update_campaign' como workaround.
     * 
     * @param campaignId identificador unico de la campaña.
     * @return Las IDs de las listas activas separadas por saltos de línea, o un
     *         mensaje de error.
     * @throws IOException          Si ocurre un error de entrada/salida (red).
     * @throws InterruptedException Si el hilo es interrumpido durante la espera.
     */
    public String getLeadInfo(String leadId) throws IOException, InterruptedException {

        String url = buildApiUrl("lead_all_info") + "&lead_id=" + leadId;

        String response = executeApiCall(url);
        return response;
    }

    /**
     * Crea un Contacto nuevo basado en uno ya existente y lo Coloca en una
     * lista en especifico, pudiendo sobre-escribir las notas (comments)
     * y el correo electronico (email).
     * 
     * @param leadId   Identificador unico de un lead a ser duplicado.
     * @param listId   Identificador unico de la lista en la cual se colocara el
     *                 lead.
     * @param comments Notas que seran agregadas (Default "").
     * @param email    Correo Electronico que sera sobre-escrito (Default "").
     * @return Id del Lead Creado.
     * @throws IOException          Si ocurre un error de entrada/salida (red).
     * @throws InterruptedException Si el hilo es interrumpido durante la espera.
     */
    public void DuplicateLeadInList(String leadId, String listId, String comments, String email)
            throws IOException, InterruptedException {
        // Crear la url
        String url = buildApiUrl("lead_all_info") + "&lead_id=" + leadId;

        // Buscar Contacto
        System.out.println(Ansi.AUTO.text("@|yellow Searching lead details for ID: " + leadId + "...|@"));
        String response = executeApiCall(url);

        // Verificar que exista
        if (response.isEmpty()) {
            System.out.println(Ansi.AUTO.text("@|red Lead not found for ID: " + leadId + "...|@"));
            throw new InterruptedException("Lead not found for ID: " + leadId);
        }

        // Crear objeto Lead
        System.out.println(Ansi.AUTO.text("@|green Lead found for ID: " + leadId + "...|@"));
        LeadModel contactInfo = new LeadModel(response);

        // Modificar lead si es necesario
        System.out.println(Ansi.AUTO.text("@|blue Changing key info ...|@"));
        if (!comments.isEmpty()) {
            contactInfo.setComments(comments);
        }
        if (!email.isEmpty()) {
            contactInfo.setEmail(email);
        }

        // crear url para nuevo lead
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

        // hacer peticion de creacion
        System.out.println(Ansi.AUTO.text("@|blue Creating New lead in List Id: " + listId + "...|@"));
        String LeadResponse = executeApiCall(urlLead);

        if (LeadResponse.isEmpty()) {
            throw new InterruptedException("Fail while creating new Lead (Already Exists).");
        }

        // deivide la respuesta por '|'
        String NewLeadId = LeadResponse.split("\\|")[2];
        System.out.println(
                Ansi.AUTO.text("@|green New lead Created inside list " + listId + "\nLead ID: " + NewLeadId + "|@"));

        return;
    }

    /**
     * Actualiza un Usuario Vicidial sobre escribiendo nombre y/o password.
     * 
     * @param ID       Identificador del Usuario
     * @param name     Nuevo nombre (Default "").
     * @param password Nueva Password (Default "").
     * @throws IOException          Cuando ocurre un error al actualizar el Usuario
     * @throws InterruptedException Cuando el hilo es interrumpido durante la
     *                              espera.
     */
    public void updateUser(String ID, String name, String password) throws IOException, InterruptedException {
        // Crear la url
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
     * Actualiza la Password de un Phone Vicidial.
     * 
     * @param ID       Identificador del Phone.
     * @param password Nueva Password (Default "").
     * @throws IOException          Cuando ocurre un error al actualizar el Phone o
     *                              si la password no es pasada.
     * @throws InterruptedException Cuando el hilo es interrumpido durante la
     *                              espera.
     */
    public void updatePhone(String ID, String password) throws IOException, InterruptedException {
        // Crear la url
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
}
