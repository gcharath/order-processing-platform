CREATE TABLE products (
    id      UUID PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    sku     VARCHAR(100) NOT NULL UNIQUE,
    price   NUMERIC(10, 2) NOT NULL,
    active  BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE orders (
    id            UUID PRIMARY KEY,
    customer_id   VARCHAR(255) NOT NULL,
    status        VARCHAR(50) NOT NULL,
    total_amount  NUMERIC(10, 2) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  UUID NOT NULL,
    quantity    INT NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(10, 2) NOT NULL,
    subtotal    NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
