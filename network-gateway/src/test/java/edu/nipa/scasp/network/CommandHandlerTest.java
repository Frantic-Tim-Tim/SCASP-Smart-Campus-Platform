package edu.nipa.scasp.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandHandlerTest {

    private final CommandHandler handler = new CommandHandler();

    @Test
    void shouldHandleGetCourses() {
        assertEquals(
                "SUCCESS: Course catalogue requested",
                handler.handleCommand("GET_COURSES")
        );
    }

    @Test
    void shouldHandleRegister() {
        assertEquals(
                "SUCCESS: Registration request received",
                handler.handleCommand("REGISTER")
        );
    }

    @Test
    void shouldHandleSubmitRequest() {
        assertEquals(
                "SUCCESS: Academic service request received",
                handler.handleCommand("SUBMIT_REQUEST")
        );
    }

    @Test
    void shouldHandleGetAnnouncements() {
        assertEquals(
                "SUCCESS: Announcements requested",
                handler.handleCommand("GET_ANNOUNCEMENTS")
        );
    }

    @Test
    void shouldRejectUnknownCommand() {
        assertEquals(
                "ERROR: Unknown command",
                handler.handleCommand("INVALID_COMMAND")
        );
    }

    @Test
    void shouldRejectEmptyCommand() {
        assertEquals(
                "ERROR: Command cannot be empty",
                handler.handleCommand(" ")
        );
    }

    @Test
    void shouldHandleDifferentCommandCases() {
        assertEquals(
                "SUCCESS: Course catalogue requested",
                handler.handleCommand(" get_courses ")
        );
    }
}