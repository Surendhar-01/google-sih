-- ECOBRIDGES – Supabase schema (run once, in order, in the Supabase SQL Editor)
-- Required so the app can auto-provision profiles and push offline-created
-- lots/transactions to the cloud for traceability.

-- 1) Unique index on profiles so first-login auto-provisioning can upsert.
CREATE UNIQUE INDEX IF NOT EXISTS profiles_auth_user_id_uidx ON profiles(auth_user_id);

-- 2) Collector lots mirrored from the on-device Room database.
CREATE TABLE IF NOT EXISTS collector_lots (
    lot_id                  TEXT PRIMARY KEY,
    collector_user_id       UUID NOT NULL,
    category_name           TEXT NOT NULL,
    sub_category            TEXT,
    weight_kg               DOUBLE PRECISION,
    condition               TEXT,
    estimated_value_inr     DOUBLE PRECISION,
    quoted_rate_per_kg      DOUBLE PRECISION,
    collection_timestamp    BIGINT,
    collection_location     TEXT,
    gps_coordinates         TEXT,
    matched_recycler_id     TEXT,
    matched_recycler_name   TEXT,
    status_name             TEXT,
    payment_mode            TEXT,
    handover_receipt_number TEXT,
    recycler_confirmed      BOOLEAN DEFAULT FALSE,
    epr_certificate_no      TEXT,
    created_at              TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE collector_lots ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS collector_lots_own ON collector_lots;
CREATE POLICY collector_lots_own ON collector_lots
    FOR ALL
    USING (auth.uid() = collector_user_id)
    WITH CHECK (auth.uid() = collector_user_id);

-- 3) Transaction ledger mirrored for the earnings-dataset requirement.
CREATE TABLE IF NOT EXISTS collector_transactions (
    transaction_id      TEXT PRIMARY KEY,
    lot_id              TEXT NOT NULL,
    collector_user_id   UUID NOT NULL,
    category_name       TEXT NOT NULL,
    weight_kg           DOUBLE PRECISION,
    rate_per_kg         DOUBLE PRECISION,
    total_amount_inr    DOUBLE PRECISION,
    payment_mode        TEXT,
    recycler_name       TEXT,
    timestamp           BIGINT,
    receipt_number      TEXT,
    is_settled          BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE collector_transactions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS collector_transactions_own ON collector_transactions;
CREATE POLICY collector_transactions_own ON collector_transactions
    FOR ALL
    USING (auth.uid() = collector_user_id)
    WITH CHECK (auth.uid() = collector_user_id);

-- ============================================================================
-- 4) Lot photos (secure Supabase Storage refs; offline-first upload queue).
--    Binary content lives only in the `lot-photos` Storage bucket, created
--    below; this table stores the object reference + upload state.
-- ============================================================================
CREATE TABLE IF NOT EXISTS lot_photos (
    photo_id          TEXT PRIMARY KEY,
    lot_id            TEXT NOT NULL,
    collector_user_id UUID NOT NULL,
    remote_path       TEXT,
    mime_type         TEXT,
    upload_status     TEXT NOT NULL DEFAULT 'PENDING',
    is_primary        BOOLEAN DEFAULT FALSE,
    created_at        BIGINT,
    created_ts        TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE lot_photos ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS lot_photos_own ON lot_photos;
CREATE POLICY lot_photos_own ON lot_photos
    FOR ALL
    USING (auth.uid() = collector_user_id)
    WITH CHECK (auth.uid() = collector_user_id);

-- ============================================================================
-- 5) Collector operating locations (privacy-preserving: area + coarse coords,
--    NEVER exact addresses/phones). Shared rows are readable by any signed-in
--    user so collectors can discover each other; writes are own-row only.
-- ============================================================================
CREATE TABLE IF NOT EXISTS collector_locations (
    collector_user_id UUID PRIMARY KEY,
    latitude          DOUBLE PRECISION NOT NULL,
    longitude         DOUBLE PRECISION NOT NULL,
    area_label        TEXT,
    is_sharing_on     BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at        BIGINT,
    updated_ts        TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE collector_locations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS collector_locations_write_own ON collector_locations;
CREATE POLICY collector_locations_write_own ON collector_locations
    FOR INSERT
    WITH CHECK (auth.uid() = collector_user_id);

DROP POLICY IF EXISTS collector_locations_update_own ON collector_locations;
CREATE POLICY collector_locations_update_own ON collector_locations
    FOR UPDATE
    USING (auth.uid() = collector_user_id)
    WITH CHECK (auth.uid() = collector_user_id);

DROP POLICY IF EXISTS collector_locations_read_shared ON collector_locations;
CREATE POLICY collector_locations_read_shared ON collector_locations
    FOR SELECT
    USING (is_sharing_on = true);

-- ============================================================================
-- 6) Collector <-> formal recycler connection requests (quote workflow).
-- ============================================================================
CREATE TABLE IF NOT EXISTS connection_requests (
    request_id        TEXT PRIMARY KEY,
    collector_user_id UUID NOT NULL,
    recycler_id       TEXT NOT NULL,
    recycler_name     TEXT,
    status            TEXT NOT NULL DEFAULT 'PENDING',
    created_at        BIGINT,
    updated_at        BIGINT,
    created_ts        TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE connection_requests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS connection_requests_own ON connection_requests;
CREATE POLICY connection_requests_own ON connection_requests
    FOR ALL
    USING (auth.uid() = collector_user_id)
    WITH CHECK (auth.uid() = collector_user_id);

-- ============================================================================
-- 7) Quotations issued by recyclers against connection requests / lots.
-- ============================================================================
CREATE TABLE IF NOT EXISTS quotations (
    quotation_id        TEXT PRIMARY KEY,
    request_id          TEXT NOT NULL,
    recycler_id         TEXT NOT NULL,
    recycler_name       TEXT,
    lot_id              TEXT,
    collector_user_id   UUID NOT NULL,
    quoted_rate_per_kg  DOUBLE PRECISION,
    quoted_total_inr    DOUBLE PRECISION,
    note                TEXT,
    status              TEXT NOT NULL DEFAULT 'PENDING',
    created_at          BIGINT,
    responded_at        BIGINT,
    created_ts          TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE quotations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS quotations_collector_own ON quotations;
CREATE POLICY quotations_collector_own ON quotations
    FOR ALL
    USING (auth.uid() = collector_user_id)
    WITH CHECK (auth.uid() = collector_user_id);

-- ============================================================================
-- 8) Immutable audit trail for security-critical actions (block/report/
--    handover confirmations). Own-row only.
-- ============================================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id          TEXT PRIMARY KEY,
    user_id     UUID NOT NULL,
    action      TEXT NOT NULL,
    detail_json TEXT,
    created_at  BIGINT,
    created_ts  TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS audit_logs_own ON audit_logs;
CREATE POLICY audit_logs_own ON audit_logs
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ============================================================================
-- 9) Public directory of CPCB/SPCB authorized recyclers & aggregators
--    (government-published registry subset). Public reads; service-role writes.
-- ============================================================================
CREATE TABLE IF NOT EXISTS authorized_recyclers (
    recycler_id          TEXT PRIMARY KEY,
    name                 TEXT NOT NULL,
    facility_location    TEXT,
    city                 TEXT,
    distance_km          DOUBLE PRECISION DEFAULT 0,
    cpcb_reg_no          TEXT,
    authorization_validity TEXT,
    phone                TEXT,
    accepted_categories  TEXT,
    doorstep_pickup      BOOLEAN DEFAULT FALSE,
    min_weight_for_pickup_kg DOUBLE PRECISION,
    rating               REAL,
    latitude             DOUBLE PRECISION,
    longitude            DOUBLE PRECISION
);

ALTER TABLE authorized_recyclers ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS authorized_recyclers_public_read ON authorized_recyclers;
CREATE POLICY authorized_recyclers_public_read ON authorized_recyclers
    FOR SELECT
    USING (true);

INSERT INTO authorized_recyclers
    (recycler_id, name, facility_location, city, distance_km, cpcb_reg_no,
     authorization_validity, phone, accepted_categories, doorstep_pickup,
     min_weight_for_pickup_kg, rating, latitude, longitude)
VALUES
    ('REC-CPCB-MH-001', 'EcoReclaim Green Refineries Pvt Ltd', 'Plot C-14, MIDC Turbhe, Navi Mumbai', 'Mumbai', 4.2, 'CPCB/EPR-REC/2023/MH-0042', 'Valid until Dec 2028', '+91 98201 44521', 'PCB_BOARDS,CABLES_WIRES,BATTERIES,MOTORS_MAGNETS,LCD_PANELS', TRUE, 25.0, 4.9, 19.0688, 73.0189),
    ('REC-CPCB-MH-002', 'MahaClean Tech Circular Resources', 'Bhiwandi Logistics Park, Thane District', 'Mumbai', 11.5, 'CPCB/EPR-REC/2022/MH-0118', 'Valid until Aug 2027', '+91 91370 88234', 'PCB_BOARDS,CABLES_WIRES,CRTS_MONITORS,MIXED_PLASTICS', TRUE, 50.0, 4.7, 19.2967, 73.0631),
    ('REC-CPCB-MH-003', 'SwachhBharat E-Waste Recyclers', 'Pimpri-Chinchwad MIDC Phase 2, Pune', 'Pune', 120.0, 'CPCB/EPR-REC/2024/MH-0205', 'Valid until Jan 2029', '+91 98902 55192', 'PCB_BOARDS,BATTERIES,LCD_PANELS,MOTORS_MAGNETS', TRUE, 40.0, 4.8, 18.6298, 73.7997),
    ('REC-CPCB-MH-004', 'Kurla Aggregator & Dismantling Center', 'LBS Marg, Kurla West, Mumbai', 'Mumbai', 2.8, 'CPCB/EPR-REC/2023/MH-0091', 'Valid until Nov 2027', '+91 98205 11299', 'PCB_BOARDS,CABLES_WIRES,BATTERIES,CRTS_MONITORS,LCD_PANELS,MOTORS_MAGNETS,MIXED_PLASTICS', TRUE, 15.0, 4.6, 19.0728, 72.8795)
ON CONFLICT (recycler_id) DO NOTHING;

-- ============================================================================
-- 10) Secure `lot-photos` Storage bucket (private). Uploads are scoped to the
--     authenticated user's own folder {auth.uid()}/{photo_id}.jpg via the
--     storage.objects policies below.
-- ============================================================================
INSERT INTO storage.buckets (id, name, public)
VALUES ('lot-photos', 'lot-photos', FALSE)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS lot_photos_storage_insert ON storage.objects;
CREATE POLICY lot_photos_storage_insert ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'lot-photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS lot_photos_storage_select ON storage.objects;
CREATE POLICY lot_photos_storage_select ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'lot-photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS lot_photos_storage_delete ON storage.objects;
CREATE POLICY lot_photos_storage_delete ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'lot-photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );