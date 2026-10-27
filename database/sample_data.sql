USE assetpulse_db;

INSERT INTO assets
    (asset_tag, asset_name, category, assigned_to, status, purchase_cost, purchase_date)
VALUES
    ('LT-001', 'ThinkPad E14', 'Laptop', 'Aarav Sharma', 'ASSIGNED', 62000.00, '2026-01-15'),
    ('MON-001', 'Dell 24-inch Monitor', 'Monitor', NULL, 'AVAILABLE', 14500.00, '2026-02-10'),
    ('SRV-001', 'Development Server', 'Server', NULL, 'MAINTENANCE', 95000.00, '2025-11-20')
ON DUPLICATE KEY UPDATE
    asset_name = VALUES(asset_name);
