package org.example.entity;

public enum UserState {
    //start states
    START,

    //base user registration
    ENTERING_FIRSTNAME,
    ENTERING_LASTNAME,

    //admin registration
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
    EDITING_EVENT_PICTURE,
    ENTERING_EVENT_START_TIME,
    CHOOSING_EVENT_DURATION,
}
