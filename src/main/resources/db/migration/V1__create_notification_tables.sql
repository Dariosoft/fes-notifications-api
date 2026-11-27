CREATE TABLE feed_events (
    event_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX feed_events_tenant_created_idx ON feed_events (tenant_id, created_at DESC);

CREATE TABLE deliveries (
    message_id VARCHAR(100) PRIMARY KEY,
    order_id UUID NULL,
    tenant_id UUID NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
