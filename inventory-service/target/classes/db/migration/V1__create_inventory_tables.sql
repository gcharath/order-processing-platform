CREATE TABLE inventory_items (
    id                  UUID PRIMARY KEY,
    product_id          UUID NOT NULL UNIQUE,
    product_name        VARCHAR(255) NOT NULL,
    quantity_available  INT NOT NULL DEFAULT 0 CHECK (quantity_available >= 0),
    quantity_reserved   INT NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    updated_at          TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_inventory_items_product_id ON inventory_items(product_id);
