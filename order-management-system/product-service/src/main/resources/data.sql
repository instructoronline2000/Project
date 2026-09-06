INSERT INTO products (name, description, price, stock_quantity, version)
SELECT 'Wireless Mouse', 'Ergonomic 2.4GHz wireless mouse', 19.99, 100, 0
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Wireless Mouse');

INSERT INTO products (name, description, price, stock_quantity, version)
SELECT 'Mechanical Keyboard', 'RGB backlit mechanical keyboard', 59.99, 50, 0
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Mechanical Keyboard');

INSERT INTO products (name, description, price, stock_quantity, version)
SELECT '27-inch Monitor', '4K UHD IPS monitor', 249.99, 25, 0
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = '27-inch Monitor');
