package dev.pablo.models;

public class ListModel {
    private int listId;
    private String listName;
    private String description;
    private String active;
    private String campaign;
    
    public ListModel(int listId, String listName, String description, String active, String campaign) {
        this.listId = listId;
        this.listName = listName;
        this.description = description;
        this.active = active;
        this.campaign = campaign;
    }

    public int getListId() {
        return listId;
    }
    public void setListId(int listId) {
        this.listId = listId;
    }
    public String getListName() {
        return listName;
    }
    public void setListName(String listName) {
        this.listName = listName;
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
    public String getCampaign() {
        return campaign;
    }
    public void setCampaign(String campaign) {
        this.campaign = campaign;
    }

    @Override
    public String toString() {
        return "ListModel [listId=" + listId + ", listName=" + listName + ", description=" + description + ", active="
                + active + ", campaign=" + campaign + "]";
    }
    
}


