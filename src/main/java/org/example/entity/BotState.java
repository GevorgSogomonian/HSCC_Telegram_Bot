package org.example.entity;

public enum BotState {
    //start states
    START,
    REGISTRATION,
    CHOOSING_ROLE,
    ENTERING_SPECIAL_KEY,

    //shared states
    COMMAND_CHOOSING,

    //admin states
    ENTERING_EVENT_NAME,
    ENTERING_EVENT_DESCRIPTION
}
