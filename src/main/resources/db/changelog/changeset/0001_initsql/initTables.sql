CREATE TABLE messageservice.`draftmessage` (
  `id` bigint(21) NOT NULL,
  `user_id` varchar(36) COLLATE utf8_unicode_ci NOT NULL,
  `rc_group_id` varchar(255) NOT NULL,
  `draft_message` longtext COLLATE utf8_unicode_ci NOT NULL,
  `create_date` datetime NOT NULL DEFAULT (UTC_TIMESTAMP),
  `update_date` datetime NOT NULL DEFAULT (UTC_TIMESTAMP),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
CREATE SEQUENCE messageservice.sequence_draftmessage
INCREMENT BY 1
MINVALUE = 0
NOMAXVALUE
START WITH 0
CACHE 0;
