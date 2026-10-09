-- An account can be made with an email address alone, and numbers now carry a country code (+91, +1).
ALTER TABLE customers ALTER COLUMN phone TYPE VARCHAR(20);
ALTER TABLE customers ALTER COLUMN phone DROP NOT NULL;
ALTER TABLE signup_requests ALTER COLUMN phone TYPE VARCHAR(20);
ALTER TABLE signup_requests ALTER COLUMN phone DROP NOT NULL;

-- The number to call for this delivery. It can differ from the account's own number.
ALTER TABLE orders ADD COLUMN contact_phone VARCHAR(20);
