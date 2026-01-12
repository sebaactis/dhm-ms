-- Tabla de transacciones
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

-- Índice para mejorar performance de queries de últimas transacciones
CREATE INDEX IF NOT EXISTS idx_transactions_account_created 
    ON transactions(account_id, created_at DESC);

-- Comentarios
COMMENT ON TABLE transactions IS 'Tabla de transacciones asociadas a cuentas';
COMMENT ON COLUMN transactions.type IS 'Tipos: DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT';
COMMENT ON COLUMN transactions.status IS 'Estados: PENDING, COMPLETED, FAILED, CANCELLED';
