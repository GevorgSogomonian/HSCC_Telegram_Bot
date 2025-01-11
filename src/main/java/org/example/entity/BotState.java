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
    ENTERING_EVENT_DESCRIPTION,
    ENTERING_EVENT_PICTURE,
    EDITING_EVENT_NAME,
    EDITING_EVENT_DESCRIPTION,
    EDITING_EVENT_PICTURE
}
