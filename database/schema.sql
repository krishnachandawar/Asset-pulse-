CREATE DATABASE IF NOT EXISTS assetpulse_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE assetpulse_db;

CREATE TABLE IF NOT EXISTS assets (
    asset_id INT PRIMARY KEY AUTO_INCREMENT,
    asset_tag VARCHAR(30) NOT NULL UNIQUE,
    asset_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    assigned_to VARCHAR(100) NULL,
    status ENUM('AVAILABLE', 'ASSIGNED', 'MAINTENANCE', 'RETIRED')
        NOT NULL DEFAULT 'AVAILABLE',
    purchase_cost DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    purchase_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_purchase_cost CHECK (purchase_cost >= 0)
);

CREATE INDEX idx_assets_status ON assets(status);
CREATE INDEX idx_assets_category ON assets(category);
