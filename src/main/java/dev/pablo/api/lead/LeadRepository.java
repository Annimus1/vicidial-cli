package dev.pablo.api.lead;

import java.io.IOException;

import dev.pablo.api.VicidialClientSingleton;
import dev.pablo.models.LeadModel;

/**
 * Reads and duplicates leads on a Vicidial instance.
 *
 * <p>
 * Keeps the client calls in one place so the {@code lead} subcommands stay thin.
 * </p>
 */
public class LeadRepository {

    private final VicidialClientSingleton client;

    public LeadRepository() {
        this(VicidialClientSingleton.getInstance());
    }

    public LeadRepository(VicidialClientSingleton client) {
        this.client = client;
    }

    /**
     * Fetches and parses one lead.
     *
     * @param leadId the lead identifier
     * @return the parsed lead
     * @throws IOException          if the API cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public LeadModel fetchInfo(String leadId) throws IOException, InterruptedException {
        return new LeadModel(client.getLeadInfo(leadId));
    }

    /**
     * Duplicates a lead into a list.
     *
     * @param leadId   the lead to copy
     * @param listId   the destination list
     * @param comments optional comments for the new lead
     * @param email    optional email override for the new lead
     * @throws IOException          if the API cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void duplicate(String leadId, String listId, String comments, String email)
            throws IOException, InterruptedException {
        client.DuplicateLeadInList(leadId, listId, comments, email);
    }
}
