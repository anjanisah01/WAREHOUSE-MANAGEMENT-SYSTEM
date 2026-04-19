-- ============================================================
-- WMS Database Schema (PostgreSQL)
-- Executed on startup by Spring Boot (spring.sql.init).
-- Hibernate ddl-auto is set to "validate", so this file
-- is the single source of truth for the DDL.
-- ============================================================

-- ── Warehouse: physical distribution centres ──
CREATE TABLE IF NOT EXISTS warehouse (
    id   BIGSERIAL   PRIMARY KEY,               -- Auto-increment PK
    code VARCHAR(50) UNIQUE NOT NULL,            -- Short code, e.g. "WH-MUM-01"
    name VARCHAR(120) NOT NULL                   -- Display name
);

-- ── Location: individual storage slots inside a warehouse ──
CREATE TABLE IF NOT EXISTS location (
    id            BIGSERIAL   PRIMARY KEY,
    warehouse_id  BIGINT      NOT NULL REFERENCES warehouse(id), -- FK → warehouse
    zone          VARCHAR(50) NOT NULL,          -- Zone label (A, B, C …)
    rack          VARCHAR(50) NOT NULL,          -- Rack within zone
    shelf         VARCHAR(50) NOT NULL,          -- Shelf level on rack
    bin           VARCHAR(50) NOT NULL,          -- Bin position on shelf
    location_type VARCHAR(30) NOT NULL,          -- STORAGE / PICK_FACE / STAGING / …
    capacity      INTEGER     NOT NULL,          -- Max units this slot can hold
    occupied      INTEGER     NOT NULL DEFAULT 0,-- Current occupancy
    UNIQUE (warehouse_id, zone, rack, shelf, bin) -- Composite uniqueness
);

-- ── Product: SKU catalogue ──
CREATE TABLE IF NOT EXISTS product (
    id            BIGSERIAL    PRIMARY KEY,
    sku           VARCHAR(80)  UNIQUE NOT NULL,  -- Internal product code
    name          VARCHAR(150) NOT NULL,         -- Human-readable name
    barcode       VARCHAR(120) UNIQUE NOT NULL,  -- Scannable barcode
    reorder_level INTEGER      NOT NULL DEFAULT 0, -- Low-stock threshold
    velocity_class VARCHAR(20),                  -- FAST / MEDIUM / SLOW
    weight_class   VARCHAR(20)                   -- LIGHT / MEDIUM / HEAVY
);

-- ── Lot/Batch: tracking for FEFO (First-Expiry-First-Out) ──
CREATE TABLE IF NOT EXISTS lot_batch (
    id          BIGSERIAL   PRIMARY KEY,
    product_id  BIGINT      NOT NULL REFERENCES product(id),  -- FK → product
    lot_no      VARCHAR(80) NOT NULL,           -- Lot/batch number string
    expiry_date DATE,                            -- Expiry (null = no expiry)
    received_at TIMESTAMP   NOT NULL             -- When this batch arrived
);

-- ── Inventory: on-hand stock per product × warehouse × lot ──
CREATE TABLE IF NOT EXISTS inventory (
    id           BIGSERIAL PRIMARY KEY,
    product_id   BIGINT NOT NULL REFERENCES product(id),     -- FK → product
    warehouse_id BIGINT NOT NULL REFERENCES warehouse(id),   -- FK → warehouse
    location_id  BIGINT REFERENCES location(id),             -- FK → location (nullable)
    lot_batch_id BIGINT REFERENCES lot_batch(id),            -- FK → lot_batch (nullable)
    quantity     INTEGER NOT NULL,               -- Available on-hand qty
    reserved_qty INTEGER NOT NULL DEFAULT 0      -- Qty reserved for open orders
);

-- ── Movement History: immutable audit trail of stock events ──
CREATE TABLE IF NOT EXISTS movement_history (
    id               BIGSERIAL   PRIMARY KEY,
    product_id       BIGINT      NOT NULL REFERENCES product(id),
    from_location_id BIGINT      REFERENCES location(id),    -- Null for inbound
    to_location_id   BIGINT      REFERENCES location(id),    -- Null for outbound
    quantity         INTEGER     NOT NULL,
    movement_type    VARCHAR(50) NOT NULL,       -- RECEIVE / PICK / SHIP / …
    event_time       TIMESTAMP   NOT NULL,
    reference_no     VARCHAR(120),               -- GRN or order number
    performed_by     VARCHAR(80)                 -- Username
);

-- ── Sales Order: outbound order header ──
CREATE TABLE IF NOT EXISTS sales_order (
    id           BIGSERIAL   PRIMARY KEY,
    order_no     VARCHAR(80) UNIQUE NOT NULL,    -- Order number
    warehouse_id BIGINT      NOT NULL REFERENCES warehouse(id),
    status       VARCHAR(40) NOT NULL,           -- CREATED → SHIPPED lifecycle
    created_at   TIMESTAMP   NOT NULL
);

-- ── Sales Order Line: line items on a sales order ──
CREATE TABLE IF NOT EXISTS sales_order_line (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT  NOT NULL REFERENCES sales_order(id), -- FK → order
    product_id    BIGINT  NOT NULL REFERENCES product(id),
    requested_qty INTEGER NOT NULL,              -- Qty customer ordered
    allocated_qty INTEGER NOT NULL DEFAULT 0     -- Qty allocated from stock
);

-- ── Picking Task: work assignment for warehouse workers ──
CREATE TABLE IF NOT EXISTS picking_task (
    id              BIGSERIAL   PRIMARY KEY,
    order_id        BIGINT      NOT NULL REFERENCES sales_order(id),
    worker_username VARCHAR(80),                 -- Assigned worker
    wave_no         VARCHAR(80),                 -- Wave number (null for single)
    strategy        VARCHAR(40) NOT NULL,        -- SINGLE / BATCH / WAVE
    status          VARCHAR(40) NOT NULL,        -- CREATED / IN_PROGRESS / COMPLETED
    created_at      TIMESTAMP   NOT NULL
);

-- ── Security: users and roles ──
CREATE TABLE IF NOT EXISTS app_user (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(80)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL          -- BCrypt hash
);

CREATE TABLE IF NOT EXISTS app_role (
    id   BIGSERIAL   PRIMARY KEY,
    name VARCHAR(40) UNIQUE NOT NULL             -- ADMIN / MANAGER / WORKER
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    role_id BIGINT NOT NULL REFERENCES app_role(id),
    PRIMARY KEY(user_id, role_id)                -- Many-to-many join table
);

-- ── Alerts: system-generated notifications ──
CREATE TABLE IF NOT EXISTS alert (
    id         BIGSERIAL    PRIMARY KEY,
    alert_type VARCHAR(40)  NOT NULL,            -- LOW_STOCK / EXPIRY / …
    message    VARCHAR(255) NOT NULL,
    severity   VARCHAR(20)  NOT NULL,            -- HIGH / MEDIUM / LOW
    resolved   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL
);

-- ── Audit Log: HTTP request audit trail ──
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL    PRIMARY KEY,
    username    VARCHAR(80),
    method      VARCHAR(20)  NOT NULL,           -- GET / POST / PUT / DELETE
    path        VARCHAR(255) NOT NULL,           -- Request URI
    status_code INTEGER      NOT NULL,           -- HTTP status
    event_time  TIMESTAMP    NOT NULL
);

-- ── Schema migrations (applied once via IF NOT EXISTS) ──

-- Add picking progress percentage column
ALTER TABLE picking_task
    ADD COLUMN IF NOT EXISTS progress_pct INTEGER NOT NULL DEFAULT 0;

-- Add unique constraint to prevent duplicate inventory rows
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_inventory_product_warehouse_lot'
    ) THEN
        ALTER TABLE inventory
            ADD CONSTRAINT uq_inventory_product_warehouse_lot
                UNIQUE (product_id, warehouse_id, lot_batch_id);
    END IF;
END $$;

-- ── Performance indexes ──
CREATE INDEX IF NOT EXISTS idx_movement_event_time ON movement_history(event_time);
CREATE INDEX IF NOT EXISTS idx_audit_username      ON audit_log(username);
CREATE INDEX IF NOT EXISTS idx_audit_event_time    ON audit_log(event_time);
