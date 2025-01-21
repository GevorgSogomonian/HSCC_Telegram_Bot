--
-- PostgreSQL database dump
--

-- Dumped from database version 15.10
-- Dumped by pg_dump version 15.10

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: admin; Type: TABLE; Schema: public; Owner: hse_user
--

CREATE TABLE public.admin (
    id bigint NOT NULL,
    chat_id bigint NOT NULL,
    is_bot boolean,
    is_premium boolean,
    language_code character varying(255),
    username character varying(255),
    user_mode boolean
);


ALTER TABLE public.admin OWNER TO hse_user;

--
-- Name: admin_id_seq; Type: SEQUENCE; Schema: public; Owner: hse_user
--

CREATE SEQUENCE public.admin_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.admin_id_seq OWNER TO hse_user;

--
-- Name: admin_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hse_user
--

ALTER SEQUENCE public.admin_id_seq OWNED BY public.admin.id;


--
-- Name: event; Type: TABLE; Schema: public; Owner: hse_user
--

CREATE TABLE public.event (
    id bigint NOT NULL,
    description character varying(5000),
    event_name character varying(255),
    creator_chat_id bigint,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    duration numeric(21,0),
    start_time timestamp(6) without time zone,
    image_url character varying(255),
    telegram_file_id character varying(255),
    event_location character varying(255)
);


ALTER TABLE public.event OWNER TO hse_user;

--
-- Name: event_id_seq; Type: SEQUENCE; Schema: public; Owner: hse_user
--

CREATE SEQUENCE public.event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.event_id_seq OWNER TO hse_user;

--
-- Name: event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hse_user
--

ALTER SEQUENCE public.event_id_seq OWNED BY public.event.id;


--
-- Name: unique_numbers_for_user_id; Type: SEQUENCE; Schema: public; Owner: hse_user
--

CREATE SEQUENCE public.unique_numbers_for_user_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.unique_numbers_for_user_id OWNER TO hse_user;

--
-- Name: usr; Type: TABLE; Schema: public; Owner: hse_user
--

CREATE TABLE public.usr (
    id bigint NOT NULL,
    chat_id bigint NOT NULL,
    first_name character varying(255),
    is_bot boolean,
    is_premium boolean,
    language_code character varying(255),
    last_name character varying(255),
    role smallint,
    username character varying(255),
    user_state smallint,
    registered boolean,
    user_id bigint,
    number_of_missed_events integer,
    number_of_visited_events integer,
    subscribed_event_ids character varying(255),
    is_admin_clone boolean,
    ishsestudent boolean,
    CONSTRAINT usr_role_check CHECK (((role >= 0) AND (role <= 1))),
    CONSTRAINT usr_user_state_check CHECK (((user_state >= 0) AND (user_state <= 6)))
);


ALTER TABLE public.usr OWNER TO hse_user;

--
-- Name: usr_id_seq; Type: SEQUENCE; Schema: public; Owner: hse_user
--

CREATE SEQUENCE public.usr_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.usr_id_seq OWNER TO hse_user;

--
-- Name: usr_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hse_user
--

ALTER SEQUENCE public.usr_id_seq OWNED BY public.usr.id;


--
-- Name: admin id; Type: DEFAULT; Schema: public; Owner: hse_user
--

ALTER TABLE ONLY public.admin ALTER COLUMN id SET DEFAULT nextval('public.admin_id_seq'::regclass);


--
-- Name: event id; Type: DEFAULT; Schema: public; Owner: hse_user
--

ALTER TABLE ONLY public.event ALTER COLUMN id SET DEFAULT nextval('public.event_id_seq'::regclass);


--
-- Name: usr id; Type: DEFAULT; Schema: public; Owner: hse_user
--

ALTER TABLE ONLY public.usr ALTER COLUMN id SET DEFAULT nextval('public.usr_id_seq'::regclass);


--
-- Name: admin admin_pkey; Type: CONSTRAINT; Schema: public; Owner: hse_user
--

ALTER TABLE ONLY public.admin
    ADD CONSTRAINT admin_pkey PRIMARY KEY (id);


--
-- Name: event event_pkey; Type: CONSTRAINT; Schema: public; Owner: hse_user
--

ALTER TABLE ONLY public.event
    ADD CONSTRAINT event_pkey PRIMARY KEY (id);


--
-- Name: usr uk_blhwp61ksqcuubvckl0ck2942; Type: CONSTRAINT; Schema: public; Owner: hse_user
--

ALTER TABLE ONLY public.usr
    ADD CONSTRAINT uk_blhwp61ksqcuubvckl0ck2942 UNIQUE (chat_id);


--
-- Name: admin uk_qn02jc8p8fyoluh1eaa2rlk73; Type: CONSTRAINT; Schema: public; Owner: hse_user
--

ALTER TABLE ONLY public.admin
    ADD CONSTRAINT uk_qn02jc8p8fyoluh1eaa2rlk73 UNIQUE (chat_id);


--
-- Name: usr usr_pkey; Type: CONSTRAINT; Schema: public; Owner: hse_user
--

ALTER TABLE ONLY public.usr
    ADD CONSTRAINT usr_pkey PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

