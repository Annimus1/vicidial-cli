package dev.pablo.models;

public class CampaignBuildInfo {
    String campaignID; 
    String campaignName; 
    String campaignDescripcion; 
    String userGroup; 
    String campaignInboundGroup; 
    String campaignCidGroup;    
    
    public CampaignBuildInfo(String campaignID, String campaignName, String campaignDescripcion, String userGroup,
            String campaignInboundGroup, String campaignCidGroup) {
        this.campaignID = campaignID;
        this.campaignName = campaignName;
        this.campaignDescripcion = campaignDescripcion;
        this.userGroup = userGroup;
        this.campaignInboundGroup = campaignInboundGroup;
        this.campaignCidGroup = campaignCidGroup;
    }

    public String getCampaignID() {
        return campaignID;
    }
    public void setCampaignID(String campaignID) {
        this.campaignID = campaignID;
    }
    public String getCampaignName() {
        return campaignName;
    }
    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }
    public String getCampaignDescripcion() {
        return campaignDescripcion;
    }
    public void setCampaignDescripcion(String campaignDescripcion) {
        this.campaignDescripcion = campaignDescripcion;
    }
    public String getUserGroup() {
        return userGroup;
    }
    public void setUserGroup(String userGroup) {
        this.userGroup = userGroup;
    }
    public String getCampaignInboundGroup() {
        return campaignInboundGroup;
    }
    public void setCampaignInboundGroup(String campaignInboundGroup) {
        this.campaignInboundGroup = campaignInboundGroup;
    }
    public String getCampaignCidGroup() {
        return campaignCidGroup;
    }
    public void setCampaignCidGroup(String campaignCidGroup) {
        this.campaignCidGroup = campaignCidGroup;
    }
}
