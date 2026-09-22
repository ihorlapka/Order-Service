CREATE TYPE order_status AS ENUM (
    'PENDING',
    'RESERVED',
    'MODIFIED',
    'RESERVATION_FAILED',
    'PENDING_PAYMENT',
    'PAID',
    'PAYMENT_STUCK',
    'SHIPPED',
    'DELIVERED',
    'DELIVERY_FAILED',
    'CANCELLED'
);

CREATE TYPE currency AS ENUM ('EUR', 'USD', 'UAH');

CREATE TYPE outbox_event_type AS ENUM (
    'ORDER_CREATED',
    'ORDER_CANCELLED',
    'ORDER_MODIFIED',
    'INVENTORY_RESERVED',
    'INVENTORY_FAILED',
    'PAYMENT_COMPLETED',
    'PAYMENT_FAILED',
    'SHIPMENT_CREATED',
    'SHIPMENT_FAILED',
    'SHIPMENT_COMPLETED'
);

CREATE TYPE event_status AS ENUM ('NEW', 'PUBLISHED');

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

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY NOT NULL,
    event_type outbox_event_type,
    order_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    payload JSONB,
    status event_status,
    published_at TIMESTAMP WITH TIME ZONE,
    attempt_count INTEGER NOT NULL DEFAULT 0
);