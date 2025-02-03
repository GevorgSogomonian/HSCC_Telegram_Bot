-- liquibase formatted sql

-- changeset gs:1738601856045-1
CREATE TABLE `admin`
(
    is_bot        BIT(1)                NULL,
    is_premium    BIT(1)                NULL,
    user_mode     BIT(1)                NULL,
    chat_id       BIGINT                NOT NULL,
    id            BIGINT AUTO_INCREMENT NOT NULL,
    language_code VARCHAR(255)          NULL,
    username      VARCHAR(255)          NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738601856045-2
CREATE TABLE event
(
    duration         DECIMAL(21)           NULL,
    created_at       datetime              NULL,
    creator_chat_id  BIGINT                NULL,
    id               BIGINT AUTO_INCREMENT NOT NULL,
    start_time       datetime              NULL,
    updated_at       datetime              NULL,
    `description`    VARCHAR(1024)         NULL,
    event_location   VARCHAR(255)          NULL,
    event_name       VARCHAR(255)          NULL,
    image_url        VARCHAR(255)          NULL,
    telegram_file_id VARCHAR(255)          NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738601856045-3
CREATE TABLE event_destructor
(
    destruction_time datetime              NULL,
    event_id         BIGINT                NULL,
    id               BIGINT AUTO_INCREMENT NOT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738601856045-4
CREATE TABLE event_missing
(
    chat_id  BIGINT                NULL,
    event_id BIGINT                NULL,
    id       BIGINT AUTO_INCREMENT NOT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738601856045-5
CREATE TABLE event_notification
(
    event_id          BIGINT                NULL,
    id                BIGINT AUTO_INCREMENT NOT NULL,
    notification_time datetime              NULL,
    notification_text VARCHAR(255)          NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738601856045-6
CREATE TABLE event_subscription
(
    chat_id  BIGINT                NULL,
    event_id BIGINT                NULL,
    id       BIGINT AUTO_INCREMENT NOT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738601856045-7
CREATE TABLE event_visit
(
    chat_id  BIGINT                NULL,
    event_id BIGINT                NULL,
    id       BIGINT AUTO_INCREMENT NOT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738601856045-8
CREATE TABLE unique_number_seq
(
    number BIGINT AUTO_INCREMENT NOT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (number)
);

-- changeset gs:1738601856045-9
CREATE TABLE usr
(
    is_admin_clone BIT(1)                NULL,
    is_bot         BIT(1)                NULL,
    is_premium     BIT(1)                NULL,
    ishsestudent   BIT(1)                NULL,
    chat_id        BIGINT                NOT NULL,
    id             BIGINT AUTO_INCREMENT NOT NULL,
    user_id        BIGINT                NULL,
    first_name     VARCHAR(255)          NULL,
    language_code  VARCHAR(255)          NULL,
    last_name      VARCHAR(255)          NULL,
    username       VARCHAR(255)          NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738601856045-10
CREATE TABLE usr_extra_info
(
    chat_id      BIGINT                NOT NULL,
    id           BIGINT AUTO_INCREMENT NOT NULL,
    email        VARCHAR(255)          NULL,
    middle_name  VARCHAR(255)          NULL,
    phone_number VARCHAR(255)          NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

-- changeset gs:1738601856045-11
ALTER TABLE usr
    ADD CONSTRAINT UK_blhwp61ksqcuubvckl0ck2942 UNIQUE (chat_id);

-- changeset gs:1738601856045-12
ALTER TABLE usr_extra_info
    ADD CONSTRAINT UK_df6evvf967rtd25kn9ndxqgoj UNIQUE (chat_id);

-- changeset gs:1738601856045-13
ALTER TABLE `admin`
    ADD CONSTRAINT UK_qn02jc8p8fyoluh1eaa2rlk73 UNIQUE (chat_id);

