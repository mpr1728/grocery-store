
CREATE TABLE orders (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  product_id INT,
  quantity INT,
  total_price DECIMAL(10, 2),
  order_date DATETIME DEFAULT CURRENT_TIMESTAMP
);
