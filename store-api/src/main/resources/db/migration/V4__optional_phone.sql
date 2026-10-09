-- An account can be made with an email address alone, and numbers now carry a country code (+91, +1).
ALTER TABLE customers MODIFY COLUMN phone VARCHAR(20) NULL;
ALTER TABLE signup_requests MODIFY COLUMN phone VARCHAR(20) NULL;

-- The number to call for this delivery. It can differ from the account's own number.
ALTER TABLE orders ADD COLUMN contact_phone VARCHAR(20);
