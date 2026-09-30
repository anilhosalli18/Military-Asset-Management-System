-- =============================================================================
-- Migration: V2__seed_data.sql
-- Description: Seed initial bases, equipment types, and default Administrator
-- =============================================================================

-- Seed Demo Bases
INSERT INTO bases (id, name, location, is_active) VALUES
(1, 'Fort Alpha', 'Northern Sector', TRUE),
(2, 'Fort Bravo', 'Eastern Sector', TRUE),
(3, 'Fort Charlie', 'Western Sector', TRUE);

-- Seed Demo Equipment Types
INSERT INTO equipment_types (id, name, category, unit) VALUES
(1, 'M4 Carbine', 'weapon', 'unit'),
(2, '5.56mm Ammunition', 'ammunition', 'rounds'),
(3, 'Humvee', 'vehicle', 'unit'),
(4, 'Body Armor Set', 'other', 'unit');

-- Seed Initial Admin User
-- Password plaintext: "Admin@123" (BCrypt hash with cost factor 12)
INSERT INTO users (id, username, email, password_hash, full_name, role, base_id, is_active) VALUES
(1, 'admin', 'admin@mams.local', '$2b$12$Z2d9wswa4nE2gRoBHGBZ4.cmo9nPx9DfTyjWIJ3/Y9WkAryS3OxGe', 'System Administrator', 'ADMIN', NULL, TRUE);
