package Peers;

public enum MessageType {
    // Existing types...
    REGISTER("Register"),      // Used when a peer wants to register itself with the tracker
    LIST_FILES("List Files"),  // Used when a peer requests a list of all files available in the network
    LOGIN("Login"),            // Used when a peer is attempting to log in
    LOGIN_SUCCESS("Login complete"), // Sent when the login is successfully processed
    LOGOUT_SUCCESS("Logout complete"),
    RESPONSE("Returned"),      // General purpose response message
    LOGOUT("Logout"),          // Used when a peer is logging out
    ERROR("Error"),            // Used to indicate an error in processing a request
    INFORM("Inform"),          // Used to send updates or notifications to the tracker
    DETAILS("Details"),        // Request details about peers holding a specific file
    DOWNLOAD_SUCCESS("Download Success"), // Acknowledges successful file download
    DOWNLOAD_FAIL("Download Fail"), // Acknowledges a failed file download
    DOWNLOAD_REQUEST("Download request"),
    CHECK_ACTIVE("Check Active"), // Used to verify if a peer is active
    REQUEST_FILE("Request File"), // Request a file from another peer
    FILE_RESPONSE("File Response"), // Response message containing the file data
    NOTIFY("Notify"),          // Notify the tracker about changes in file availability
    ACTIVE_RESPONSE("Active Response"), // Response to CHECK_ACTIVE request

    EXIT("Exit");              // Indicate that a peer or tracker is exiting the session

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
