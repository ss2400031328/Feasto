BEGIN;

ALTER TABLE public.payments
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_idempotency_key
    ON public.payments (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

COMMIT;
