-- Apple's id for the person. Their email may be an Apple relay address that forwards to them.
ALTER TABLE customers ADD COLUMN apple_subject VARCHAR(255);
ALTER TABLE customers ADD CONSTRAINT uq_customers_apple_subject UNIQUE (apple_subject);
