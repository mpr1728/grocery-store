-- Lasting facts a shopper has told the assistant, such as household size or diet, so it does not have
-- to be told again. The shopper can see and delete them on the account page.
CREATE TABLE assistant_memories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  fact VARCHAR(160) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_assistant_memories_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);
CREATE INDEX ix_assistant_memories_customer ON assistant_memories (customer_id, id);
