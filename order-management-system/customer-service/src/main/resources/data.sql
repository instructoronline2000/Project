INSERT INTO customers (name, email, phone, address)
SELECT 'Alice Johnson', 'alice@example.com', '555-0100', '12 Baker Street'
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'alice@example.com');

INSERT INTO customers (name, email, phone, address)
SELECT 'Bob Smith', 'bob@example.com', '555-0101', '99 High Street'
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'bob@example.com');
