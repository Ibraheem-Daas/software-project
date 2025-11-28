-- Add reservation table to support item reservation feature
-- Users can reserve items that are currently unavailable

CREATE TABLE IF NOT EXISTS reservation (
    reservation_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_item FOREIGN KEY (item_id) REFERENCES media_item(item_id) ON DELETE CASCADE,
    CONSTRAINT chk_reservation_dates CHECK (expiry_date >= reservation_date),
    CONSTRAINT chk_reservation_status CHECK (status IN ('ACTIVE', 'FULFILLED', 'EXPIRED', 'CANCELLED'))
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_reservation_user ON reservation(user_id);
CREATE INDEX IF NOT EXISTS idx_reservation_item ON reservation(item_id);
CREATE INDEX IF NOT EXISTS idx_reservation_status ON reservation(status);
CREATE INDEX IF NOT EXISTS idx_reservation_expiry ON reservation(expiry_date);

-- Create composite index for common query pattern
CREATE INDEX IF NOT EXISTS idx_reservation_item_status ON reservation(item_id, status, reservation_date);

COMMENT ON TABLE reservation IS 'Stores reservations for library items that are currently unavailable';
COMMENT ON COLUMN reservation.status IS 'Reservation status: ACTIVE, FULFILLED (notified and waiting for pickup), EXPIRED (time limit passed), CANCELLED (user cancelled)';
COMMENT ON COLUMN reservation.expiry_date IS 'Date/time when the reservation expires. For ACTIVE: max wait time. For FULFILLED: pickup deadline.';
