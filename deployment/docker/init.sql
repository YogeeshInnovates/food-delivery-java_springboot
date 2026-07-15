-- Initial seed data for the Food Delivery Application
-- Note: This script runs AFTER Hibernate auto-ddl has created the schema.
-- It only contains INSERT statements to seed initial data safely.

-- Seed Restaurants
-- Uses WHERE NOT EXISTS instead of ON CONFLICT because there is no unique constraint on name
INSERT INTO restaurant (name, city, cuisine, rating, status) 
SELECT 'Pizza Palace', 'New York', 'Italian', 4.5, 'OPEN'
WHERE NOT EXISTS (SELECT 1 FROM restaurant WHERE name = 'Pizza Palace');

INSERT INTO restaurant (name, city, cuisine, rating, status) 
SELECT 'Spice Garden', 'London', 'Indian', 4.8, 'OPEN'
WHERE NOT EXISTS (SELECT 1 FROM restaurant WHERE name = 'Spice Garden');

INSERT INTO restaurant (name, city, cuisine, rating, status) 
SELECT 'Burger Barn', 'Chicago', 'Fast Food', 4.2, 'OPEN'
WHERE NOT EXISTS (SELECT 1 FROM restaurant WHERE name = 'Burger Barn');

-- Seed Menu Items
INSERT INTO menu_items (restaurant_id, category, name, description, price, status, is_deleted) 
SELECT r.id, 'Italian', 'Margherita Pizza', 'Classic cheese and tomato', 12.99, 'AVAILABLE', false
FROM restaurant r WHERE r.name = 'Pizza Palace'
AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Margherita Pizza');

INSERT INTO menu_items (restaurant_id, category, name, description, price, status, is_deleted) 
SELECT r.id, 'Indian', 'Chicken Tikka Masala', 'Spicy and creamy curry', 14.50, 'AVAILABLE', false
FROM restaurant r WHERE r.name = 'Spice Garden'
AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Chicken Tikka Masala');

INSERT INTO menu_items (restaurant_id, category, name, description, price, status, is_deleted) 
SELECT r.id, 'Fast Food', 'Cheeseburger', 'Double beef patty with cheese', 8.99, 'AVAILABLE', false
FROM restaurant r WHERE r.name = 'Burger Barn'
AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Cheeseburger');
