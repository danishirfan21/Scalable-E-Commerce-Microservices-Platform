-- Sample data for Product Service (for development/testing)
-- This file will be executed if spring.jpa.defer-datasource-initialization=true

-- Insert sample products
INSERT INTO products (name, description, price, quantity, category, image_url, sku, created_at, updated_at)
VALUES
    ('Wireless Mouse', 'Ergonomic wireless mouse with precision tracking', 29.99, 150, 'Electronics', 'https://example.com/mouse.jpg', 'ELEC-MOUSE-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Mechanical Keyboard', 'RGB mechanical keyboard with cherry MX switches', 89.99, 75, 'Electronics', 'https://example.com/keyboard.jpg', 'ELEC-KB-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('USB-C Hub', '7-in-1 USB-C hub with HDMI and card reader', 49.99, 200, 'Electronics', 'https://example.com/hub.jpg', 'ELEC-HUB-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Laptop Stand', 'Adjustable aluminum laptop stand', 39.99, 120, 'Accessories', 'https://example.com/stand.jpg', 'ACC-STAND-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Webcam HD', '1080p HD webcam with built-in microphone', 59.99, 90, 'Electronics', 'https://example.com/webcam.jpg', 'ELEC-CAM-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Phone Case', 'Protective silicone phone case', 19.99, 300, 'Accessories', 'https://example.com/case.jpg', 'ACC-CASE-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Bluetooth Speaker', 'Portable waterproof Bluetooth speaker', 79.99, 60, 'Electronics', 'https://example.com/speaker.jpg', 'ELEC-SPEAK-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Screen Protector', 'Tempered glass screen protector', 14.99, 250, 'Accessories', 'https://example.com/protector.jpg', 'ACC-PROT-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Power Bank', '20000mAh portable power bank', 44.99, 180, 'Electronics', 'https://example.com/powerbank.jpg', 'ELEC-PB-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Cable Organizer', 'Magnetic cable management system', 24.99, 150, 'Accessories', 'https://example.com/organizer.jpg', 'ACC-ORG-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (sku) DO NOTHING;
