package dev.pablo.models;

/**
 * Represents one user returned by the Vicidial {@code users_list} function.
 */
public class UserModel {

    private final String user;
    private final String fullName;
    private final String userGroup;
    private final String userLevel;
    private final String active;
    private final String email;

    public UserModel(String user, String fullName, String userGroup, String userLevel, String active, String email) {
        this.user = user;
        this.fullName = fullName;
        this.userGroup = userGroup;
        this.userLevel = userLevel;
        this.active = active;
        this.email = email;
    }

    public String getUser() {
        return user;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUserGroup() {
        return userGroup;
    }

    public String getUserLevel() {
        return userLevel;
    }

    public String getActive() {
        return active;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "UserModel [user=" + user + ", fullName=" + fullName + ", userGroup=" + userGroup
                + ", userLevel=" + userLevel + ", active=" + active + ", email=" + email + "]";
    }
}
