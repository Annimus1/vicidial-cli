package dev.pablo.models;

public class CampaignModel {

    private String id;
    private String description;
    private String active;

    public CampaignModel(String id, String description, String active) {
        this.id = id;
        this.description = description;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public boolean isActive() {
        return active != null && active.trim().equalsIgnoreCase("y");
    }

    @Override
    public String toString() {
        return "CampaignModel [id=" + id + ", description=" + description + ", active=" + active + "]";
    }
}
