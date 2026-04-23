package dev.pablo.models;

public class UserGroupModel {
    private String id;
    private String name;
    private String active;

    public UserGroupModel(String id, String name, String active) {
        this.id = id;
        this.name = name;
        this.active = active;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getActive() {
        return active;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setActive(String active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "UserGroup{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", active='" + active + '\'' +
                '}';
    }
}