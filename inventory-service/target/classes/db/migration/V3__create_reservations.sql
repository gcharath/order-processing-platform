CREATE TABLE reservations (
    id          UUID PRIMARY KEY,
    order_id    UUID NOT NULL,
    product_id  UUID NOT NULL,
    quantity    INT NOT NULL CHECK (quantity > 0),
    status      VARCHAR(50) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reservations_order_id ON reservations(order_id);
CREATE UNIQUE INDEX idx_reservations_order_product ON reservations(order_id, product_id);
