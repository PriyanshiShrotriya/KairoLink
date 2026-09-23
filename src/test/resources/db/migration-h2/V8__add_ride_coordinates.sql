ALTER TABLE rides
    ADD COLUMN source_latitude NUMERIC(9, 6);

ALTER TABLE rides
    ADD COLUMN source_longitude NUMERIC(9, 6);

ALTER TABLE rides
    ADD COLUMN destination_latitude NUMERIC(9, 6);

ALTER TABLE rides
    ADD COLUMN destination_longitude NUMERIC(9, 6);
