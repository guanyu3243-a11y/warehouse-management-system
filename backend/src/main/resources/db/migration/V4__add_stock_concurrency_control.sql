ALTER TABLE stock
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic concurrency version' AFTER locked_quantity;
