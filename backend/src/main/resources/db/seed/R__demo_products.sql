-- Demo catalog so the app has data on first run. Only inserts into an empty table.
-- Lives outside db/migration so tests start from a clean schema.
INSERT INTO products (name, description, price, stock, category, active, created_at, updated_at)
SELECT v.name, v.description, v.price, v.stock, v.category, TRUE, NOW(), NOW()
FROM (VALUES
    ('Wireless Headphones', 'Over-ear Bluetooth headphones with noise cancelling', 189.99, 25, 'ELECTRONICS'),
    ('Mechanical Keyboard', '75% layout, hot-swappable switches', 129.50, 40, 'ELECTRONICS'),
    ('USB-C Charger 65W', 'GaN charger with two USB-C ports', 39.90, 100, 'ELECTRONICS'),
    ('Cotton T-Shirt', 'Basic unisex t-shirt, 100% organic cotton', 19.99, 200, 'CLOTHING'),
    ('Rain Jacket', 'Lightweight waterproof jacket', 79.00, 30, 'CLOTHING'),
    ('Ceramic Coffee Mug', '350 ml handmade mug', 14.50, 80, 'HOME'),
    ('Desk Lamp', 'LED lamp with adjustable brightness', 45.00, 35, 'HOME'),
    ('Clean Code', 'A Handbook of Agile Software Craftsmanship', 42.00, 15, 'BOOKS'),
    ('Yoga Mat', 'Non-slip mat, 6 mm', 29.99, 50, 'SPORTS'),
    ('Building Blocks Set', '500 pieces, ages 6+', 34.99, 60, 'TOYS'),
    ('Face Moisturizer', 'Daily moisturizer with SPF 30', 24.90, 70, 'BEAUTY'),
    ('Colombian Coffee Beans', '500 g medium roast', 18.00, 120, 'FOOD')
) AS v(name, description, price, stock, category)
WHERE NOT EXISTS (SELECT 1 FROM products);
