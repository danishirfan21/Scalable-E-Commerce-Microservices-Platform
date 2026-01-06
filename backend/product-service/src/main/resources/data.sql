-- Initial product data for Product Service
-- This file will be executed if spring.sql.init.mode=always

-- Insert products across different categories
-- Electronics Category
INSERT INTO products (name, description, price, quantity, category, image_url, sku, created_at, updated_at)
VALUES 
('Gaming Laptop', 'High performance gaming laptop with RTX 4060 GPU, 16GB RAM, and 512GB SSD', 1299.99, 50, 'Electronics', 'https://picsum.photos/400/300?random=1', 'ELEC-LAP-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Smartphone Pro', 'Latest flagship smartphone with 6.7" AMOLED display, 5G capability, and 128GB storage', 899.99, 100, 'Electronics', 'https://picsum.photos/400/300?random=2', 'ELEC-PHN-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tablet 12.9"', 'Premium tablet with 12.9" Retina display, 256GB storage, and Apple Pencil support', 499.99, 75, 'Electronics', 'https://picsum.photos/400/300?random=3', 'ELEC-TAB-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Wireless Headphones', 'Noise-cancelling over-ear headphones with 30-hour battery life', 199.99, 200, 'Electronics', 'https://picsum.photos/400/300?random=4', 'ELEC-HDP-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Smartwatch Elite', 'Advanced fitness tracking smartwatch with GPS, heart rate monitor, and 7-day battery', 349.99, 120, 'Electronics', 'https://picsum.photos/400/300?random=5', 'ELEC-WTC-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (sku) DO NOTHING;

-- Accessories Category
INSERT INTO products (name, description, price, quantity, category, image_url, sku, created_at, updated_at)
VALUES 
('Wireless Mouse', 'Ergonomic wireless mouse with precision tracking and rechargeable battery', 29.99, 300, 'Accessories', 'https://picsum.photos/400/300?random=6', 'ACC-MOU-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Mechanical Keyboard', 'RGB mechanical gaming keyboard with customizable keys and macro support', 89.99, 150, 'Accessories', 'https://picsum.photos/400/300?random=7', 'ACC-KBD-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USB-C Charger', '65W fast charging USB-C power adapter compatible with laptops and phones', 19.99, 400, 'Accessories', 'https://picsum.photos/400/300?random=8', 'ACC-CHG-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Phone Case Pro', 'Durable protective phone case with military-grade drop protection', 15.99, 250, 'Accessories', 'https://picsum.photos/400/300?random=9', 'ACC-CAS-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Laptop Stand', 'Adjustable aluminum laptop stand with ventilation for improved ergonomics', 39.99, 180, 'Accessories', 'https://picsum.photos/400/300?random=10', 'ACC-STD-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (sku) DO NOTHING;

-- Home Category
INSERT INTO products (name, description, price, quantity, category, image_url, sku, created_at, updated_at)
VALUES 
('Smart Speaker', 'Voice-controlled smart speaker with premium sound quality and smart home integration', 129.99, 80, 'Home', 'https://picsum.photos/400/300?random=11', 'HOME-SPK-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Security Camera', 'Indoor/outdoor HD security camera with night vision and motion detection', 149.99, 60, 'Home', 'https://picsum.photos/400/300?random=12', 'HOME-CAM-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Smart Thermostat', 'Energy-efficient smart thermostat with learning capabilities and remote control', 199.99, 45, 'Home', 'https://picsum.photos/400/300?random=13', 'HOME-THR-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LED Light Bulb 4-Pack', 'Smart WiFi LED bulbs with color changing and voice control support', 49.99, 200, 'Home', 'https://picsum.photos/400/300?random=14', 'HOME-BLB-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Robot Vacuum', 'Automated robot vacuum cleaner with mapping technology and app control', 299.99, 40, 'Home', 'https://picsum.photos/400/300?random=15', 'HOME-VAC-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (sku) DO NOTHING;

