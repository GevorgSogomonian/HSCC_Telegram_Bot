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

--
-- Data for Name: admin; Type: TABLE DATA; Schema: public; Owner: hse_user
--

COPY public.admin (id, chat_id, is_bot, is_premium, language_code, username, user_mode) FROM stdin;
3	997976128	f	t	ru	gevorgsogomonian	f
\.


--
-- Data for Name: event; Type: TABLE DATA; Schema: public; Owner: hse_user
--

COPY public.event (id, description, event_name, creator_chat_id, created_at, updated_at, duration, start_time, image_url, telegram_file_id, event_location) FROM stdin;
136	Вдвду	Анана	997976128	2025-01-18 18:00:39.800873	2025-01-18 18:01:44.612336	10800000000000	2025-03-12 12:44:00	ceb7d845-ec9b-4cb0-8587-a88f4aed779e-documents/file_129.JPG	AgACAgIAAxkDAAIYVWeLwlewYlvRc-MehZkbNMpHGn7MAAK96TEbDeBgSIY99onSH43IAQADAgADcwADNgQ	Очень далеко
137	Вдщущу	Адажду	997976128	2025-01-18 18:35:04.886521	2025-01-18 18:38:48.542849	10800000000000	2025-11-12 12:23:00	6c78f6ef-44fa-4e21-8a52-ace4da959a89-documents/file_130.JPG	AgACAgIAAxkDAAIYv2eLywfwUq9r-uJeo5zdOfWeetHoAALw6TEbDeBgSOsmdOnlk5UgAQADAgADcwADNgQ	Природа
138	Готовы начать год с новых возможностей?❤️‍🔥\n✔️ 17 января HSCC проводит эксклюзивное мероприятие для тех, кто хочет освоить навык решения бизнес-кейсов гарвардского формата! \n\nВас ждет:\n🔵 Что такое образовательная программа Case Study?\n🔵 Что нужно, чтобы выигрывать на кейс-чемпионатах любого уровня?\n🔵 Как получить фаст-треки и другие бонусы от партнёров (Changellenge, GRP)?\n\n🔹Ответы на эти вопросы, а также возможность послушать историю победителей Cup Moscow ждут вас на мероприятии.\n\nИ это еще не все! ❤️‍🔥\nНа встрече будут присутствовать представители наших партнеров – Changellenge и GRP, которым вы сможете задать интересующие вопросы!\n\n🖇Гостям, которые не являются студентами НИУ ВШЭ, необходимо заполнить форму для оформления пропуска до 13.01\n\nHSE SPb Case Club💜	Hsjekwkns siekwniw	997976128	2025-01-18 18:38:43.725625	2025-01-18 18:49:32.947131	10800000000000	2025-02-20 15:30:00	54855908-79a5-4294-8636-876b3bf12a9c-documents/file_131.JPG	AgACAgIAAxkDAAIYwGeLywjOyt2BtdMTMAee0YT7dAoBAALx6TEbDeBgSD0iy3s_kWi5AQADAgADcwADNgQ	📌 НИУ ВШЭ, Кантемировская улица, 3к1, 345 аудитория
\.


--
-- Data for Name: usr; Type: TABLE DATA; Schema: public; Owner: hse_user
--

COPY public.usr (id, chat_id, first_name, is_bot, is_premium, language_code, last_name, role, username, user_state, registered, user_id, number_of_missed_events, number_of_visited_events, subscribed_event_ids, is_admin_clone, ishsestudent) FROM stdin;
31	997976128	Геворг	f	t	ru	Согомонян	1	gevorgsogomonian	\N	\N	163178	0	0	136_137	t	f
\.


--
-- Name: admin_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hse_user
--

SELECT pg_catalog.setval('public.admin_id_seq', 3, true);


--
-- Name: event_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hse_user
--

SELECT pg_catalog.setval('public.event_id_seq', 138, true);


--
-- Name: unique_numbers_for_user_id; Type: SEQUENCE SET; Schema: public; Owner: hse_user
--

SELECT pg_catalog.setval('public.unique_numbers_for_user_id', 21, true);


--
-- Name: usr_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hse_user
--

SELECT pg_catalog.setval('public.usr_id_seq', 31, true);


--
-- PostgreSQL database dump complete
--

