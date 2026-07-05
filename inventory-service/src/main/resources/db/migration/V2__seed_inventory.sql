INSERT INTO inventory_items (id, product_id, product_name, quantity_available, quantity_reserved, updated_at) VALUES
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440001', 'Widget A',     100, 0, NOW()),
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440002', 'Widget B',     150, 0, NOW()),
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440003', 'Gadget X',      75, 0, NOW()),
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440004', 'Gadget Y',      50, 0, NOW()),
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440005', 'Super Widget',  25, 0, NOW());
