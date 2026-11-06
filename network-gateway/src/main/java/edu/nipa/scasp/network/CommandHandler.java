package edu.nipa.scasp.network;

public class CommandHandler {

    public String handleCommand(String command) {

        if (command == null || command.isBlank()) {
            return "ERROR: Command cannot be empty";
        }

        String normalizedCommand = command.trim().toUpperCase();

        switch (normalizedCommand) {

            case "GET_COURSES":
                return "SUCCESS: Course catalogue requested";

            case "REGISTER":
                return "SUCCESS: Registration request received";

            case "SUBMIT_REQUEST":
                return "SUCCESS: Academic service request received";

            case "GET_ANNOUNCEMENTS":
                return "SUCCESS: Announcements requested";

            default:
                return "ERROR: Unknown command";
        }
    }
}