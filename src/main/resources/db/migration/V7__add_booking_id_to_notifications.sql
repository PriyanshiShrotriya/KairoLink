-- V7: Add optional booking_id column to notifications for linking to bookings
ALTER TABLE notifications ADD COLUMN booking_id BIGINT;

-- Add foreign key constraint (optional, nullable)
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_booking
    FOREIGN KEY (booking_id) REFERENCES bookings(id);

-- Create index for faster lookups by booking
CREATE INDEX idx_notifications_booking_id ON notifications(booking_id);