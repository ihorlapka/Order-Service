CREATE TYPE order_status AS ENUM (
    'PENDING',
    'INVENTORY_RESERVED',
    'PENDING_PAYMENT',
    'PAID',
    'SHIPPED',
    'CANCELED'
);

CREATE TYPE currency AS ENUM ('EUR', 'USD', 'UAH');

CREATE TYPE order_event_type AS ENUM (
    'ORDER_CREATED',
    'ORDER_CANCELED',
    'INVENTORY_RESERVED',
    'INVENTORY_FAILED',
    'PAYMENT_COMPLETED',
    'PAYMENT_FAILED',
    'SHIPMENT_CREATED',
    'SHIPMENT_FAILED',
    'SHIPMENT_COMPLETED'
);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    status order_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    currency currency,
    total_price DECIMAL(9,6)
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL,
    order_id UUID NOT NULL,
    description VARCHAR(255),
    quantity INTEGER,
    price DECIMAL(9,6),
    image_data BYTEA,
    item_url VARCHAR(255),
    CONSTRAINT fk_order
        FOREIGN KEY (order_id)
        REFERENCES orders (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE order_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type order_event_type,
    order_id UUID NOT NULL,
    payload JSONB
);