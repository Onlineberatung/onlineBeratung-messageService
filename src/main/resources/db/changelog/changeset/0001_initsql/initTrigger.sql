CREATE TRIGGER messageservice.`message_update` BEFORE UPDATE ON messageservice.`draftmessage` FOR EACH
ROW BEGIN
set new.update_date=utc_timestamp();
END //
