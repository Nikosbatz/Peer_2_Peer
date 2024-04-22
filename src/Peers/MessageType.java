package Peers;


public enum MessageType {
    // Define types of messages that can be sent in the network.
    REGISTER("Register"),      // Used when a peer wants to register itself with the tracker
    LIST_FILES("List Files"),
    LOGIN("Login "),
    LOGIN_SUCCESS("Login complete"),
    RESPONSE("returned"),
    LOGOUT("logout"),
    ERROR("error");




    private final String actionDescription;

    // Constructor for the enum to set the action descriptions
    MessageType(String description) {
        this.actionDescription = description;
    }

    // Getter method to retrieve the action description
    public String getActionDescription() {
        return actionDescription;
    }

    @Override
    public String toString() {
        return this.actionDescription;
    }
}
