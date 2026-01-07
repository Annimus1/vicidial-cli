package dev.pablo.models;

public class DidModel {
    private int id;
    private String callerId;
    private String description;
    private char active;
    private String carrier;
    private String group;
    private String route;
    private String rec;
    private String modify;


    public DidModel(){}


    public int getId() {
        return id;
    }


    public void setId(int id) {
        this.id = id;
    }


    public String getCallerId() {
        return callerId;
    }


    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public char getActive() {
        return active;
    }


    public void setActive(char active) {
        this.active = active;
    }


    public String getCarrier() {
        return carrier;
    }


    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }


    public String getGroup() {
        return group;
    }


    public void setGroup(String group) {
        this.group = group;
    }


    public String getRoute() {
        return route;
    }


    public void setRoute(String route) {
        this.route = route;
    }


    public String getRec() {
        return rec;
    }


    public void setRec(String rec) {
        this.rec = rec;
    }


    public String getModify() {
        return modify;
    }


    public void setModify(String modify) {
        this.modify = modify;
    }


    @Override
    public String toString() {
        return "DidModel [id=" + id + ", callerId=" + callerId + ", description=" + description + ", active=" + active
                + ", group=" + group + "]";
    }

}
