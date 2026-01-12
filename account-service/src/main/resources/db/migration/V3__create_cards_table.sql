-- Tabla de tarjetas asociadas a cuentas
CREATE TABLE IF NOT EXISTS cards (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    last_four_digits VARCHAR(4) NOT NULL,
    card_holder_name VARCHAR(100) NOT NULL,
    expiration_date DATE NOT NULL,
    card_type VARCHAR(10) NOT NULL,
    card_brand VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_card_account FOREIGN KEY (account_id) 
        REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT chk_card_type CHECK (card_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_card_brand CHECK (card_brand IN ('VISA', 'MASTERCARD', 'AMEX', 'MAESTRO')),
    CONSTRAINT chk_card_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED'))
);

-- Índices para mejorar performance
CREATE INDEX IF NOT EXISTS idx_cards_account_id ON cards(account_id);
CREATE INDEX IF NOT EXISTS idx_cards_last_four_digits ON cards(last_four_digits);
CREATE INDEX IF NOT EXISTS idx_cards_status ON cards(status);

-- Comentarios
COMMENT ON TABLE cards IS 'Tabla de tarjetas de crédito y débito asociadas a cuentas';
COMMENT ON COLUMN cards.last_four_digits IS 'Últimos 4 dígitos de la tarjeta (seguridad PCI-DSS)';
COMMENT ON COLUMN cards.card_type IS 'Tipos: DEBIT, CREDIT';
COMMENT ON COLUMN cards.card_brand IS 'Marcas: VISA, MASTERCARD, AMEX, MAESTRO';
COMMENT ON COLUMN cards.status IS 'Estados: ACTIVE, BLOCKED, EXPIRED';
