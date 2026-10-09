-- Details for packaged goods. Mostly filled when a product comes in through a catalog import.
ALTER TABLE products ADD COLUMN brand VARCHAR(100);
ALTER TABLE products ADD COLUMN barcode VARCHAR(20);
ALTER TABLE products ADD COLUMN ingredients VARCHAR(2000);
ALTER TABLE products ADD COLUMN image_url VARCHAR(300);
ALTER TABLE products ADD COLUMN source VARCHAR(20);
ALTER TABLE products ADD COLUMN source_url VARCHAR(300);

-- How the product was grown, and the certificate behind that claim.
ALTER TABLE products ADD COLUMN growing_method VARCHAR(30);
ALTER TABLE products ADD COLUMN certifier VARCHAR(100);
ALTER TABLE products ADD COLUMN certificate_number VARCHAR(60);
ALTER TABLE products ADD COLUMN certificate_document_id BIGINT;

-- Pesticide residue test for one batch, with the lab's report.
ALTER TABLE stock_batches ADD COLUMN test_lab VARCHAR(100);
ALTER TABLE stock_batches ADD COLUMN test_date DATE;
ALTER TABLE stock_batches ADD COLUMN test_result VARCHAR(20);
ALTER TABLE stock_batches ADD COLUMN test_document_id BIGINT;

-- Uploaded certificates and lab reports. Kept in the database while volumes are small.
CREATE TABLE documents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  file_name VARCHAR(200) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  size_bytes INT NOT NULL,
  content LONGBLOB NOT NULL,
  uploaded_at TIMESTAMP NOT NULL
);

-- Products found by a catalog import. A person approves each one and sets its price before it is sold.
CREATE TABLE catalog_candidates (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  barcode VARCHAR(20) NOT NULL,
  name VARCHAR(120) NOT NULL,
  brand VARCHAR(100),
  unit_label VARCHAR(30),
  category VARCHAR(50),
  diet VARCHAR(10),
  ingredients VARCHAR(2000),
  labels VARCHAR(300),
  image_url VARCHAR(300),
  source VARCHAR(20) NOT NULL,
  source_url VARCHAR(300),
  status VARCHAR(10) NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uq_catalog_candidates_barcode UNIQUE (barcode)
);
CREATE INDEX ix_catalog_candidates_status ON catalog_candidates (status, id);
