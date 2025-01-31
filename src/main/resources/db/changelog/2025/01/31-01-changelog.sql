-- liquibase formatted sql

-- changeset gs:1738318022312-1
CREATE TABLE `admin`
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    chat_id       BIGINT                NOT NULL,
    is_bot        BIT(1)                NULL,
    is_premium    BIT(1)                NULL,
    language_code VARCHAR(255)          NULL,
    user_mode     BIT(1)                NULL,
    username      VARCHAR(255)          NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738318022312-2
CREATE TABLE event
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime              NULL,
    creator_chat_id  BIGINT                NULL,
    `description`    VARCHAR(1024)         NULL,
    duration         DECIMAL(21)           NULL,
    event_location   VARCHAR(255)          NULL,
    event_name       VARCHAR(255)          NULL,
    image_url        VARCHAR(255)          NULL,
    start_time       datetime              NULL,
    telegram_file_id VARCHAR(255)          NULL,
    updated_at       datetime              NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738318022312-3
CREATE TABLE event_notification
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    event_id          BIGINT                NULL,
    notification_text VARCHAR(255)          NULL,
    notification_time datetime              NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738318022312-4
CREATE TABLE event_subscription
(
    id       BIGINT AUTO_INCREMENT NOT NULL,
    chat_id  BIGINT                NULL,
    event_id BIGINT                NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738318022312-5
CREATE TABLE unique_number_seq
(
    number BIGINT AUTO_INCREMENT NOT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (number)
);

-- changeset gs:1738318022312-6
CREATE TABLE usr
(
    id                       BIGINT AUTO_INCREMENT NOT NULL,
    chat_id                  BIGINT                NOT NULL,
    first_name               VARCHAR(255)          NULL,
    is_admin_clone           BIT(1)                NULL,
    is_bot                   BIT(1)                NULL,
    ishsestudent             BIT(1)                NULL,
    is_premium               BIT(1)                NULL,
    language_code            VARCHAR(255)          NULL,
    last_name                VARCHAR(255)          NULL,
    number_of_missed_events  INT                   NULL,
    number_of_visited_events INT                   NULL,
    `role`                   TINYINT               NULL,
    subscribed_event_ids     VARCHAR(255)          NULL,
    user_id                  BIGINT                NULL,
    username                 VARCHAR(255)          NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738318022312-7
ALTER TABLE usr
    ADD CONSTRAINT UK_blhwp61ksqcuubvckl0ck2942 UNIQUE (chat_id);

-- changeset gs:1738318022312-8
ALTER TABLE `admin`
    ADD CONSTRAINT UK_qn02jc8p8fyoluh1eaa2rlk73 UNIQUE (chat_id);

