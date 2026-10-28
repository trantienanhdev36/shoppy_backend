BEGIN;

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS orders_status_check;

ALTER TABLE orders
    ADD CONSTRAINT orders_status_check
        CHECK (status IN (
            'PENDING',
            'WAITING_FOR_PAYMENT',
            'PROCESSING',
            'READY_TO_SHIP',
            'SHIPPING',
            'DELIVERED',
            'CANCELLED'
        ));

COMMIT;
