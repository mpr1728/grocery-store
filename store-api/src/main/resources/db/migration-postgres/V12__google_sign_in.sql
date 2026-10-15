-- Google's id for the person, so the link survives a change of email address on their Google account.
ALTER TABLE customers ADD COLUMN google_subject VARCHAR(255);
ALTER TABLE customers ADD CONSTRAINT uq_customers_google_subject UNIQUE (google_subject);
