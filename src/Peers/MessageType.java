package Peers;


public enum MessageType {
    // Define types of messages that can be sent in the network.
    REGISTER("Register"),      // Used when a peer wants to register itself with the tracker
    LIST_FILES("List Files"),  // Used when a peer requests a list of all files available in the network
    LOGIN("Login"),            // Used when a peer is attempting to log in
    LOGIN_SUCCESS("Login complete"), // Sent when the login is successfully processed
    LOGOUT_SUCCESS("Logout complete"),
    RESPONSE("Returned"),      // General purpose response message
    LOGOUT("Logout"),          // Used when a peer is logging out
    ERROR("Error"),            // Used to indicate an error in processing a request
    INFORM("Inform");          // Used to send updates or notifications to the tracker





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
