-- Script de datos de prueba para tarjetas
-- NOTA: Esto es solo para desarrollo/testing. En producción NO usar migrations para datos de prueba.

-- Insertar tarjetas de ejemplo para cuentas existentes
-- Asumiendo que existe al menos una cuenta con ID 1 (ajustar según tu BD)

INSERT INTO cards (account_id, last_four_digits, card_holder_name, expiration_date, card_type, card_brand, status, created_at)
VALUES 
    -- Tarjetas para cuenta ID 1
    (1, '1234', 'Juan Perez', '2025-12-31', 'DEBIT', 'VISA', 'ACTIVE', CURRENT_TIMESTAMP),
    (1, '5678', 'Juan Perez', '2026-06-30', 'CREDIT', 'MASTERCARD', 'ACTIVE', CURRENT_TIMESTAMP),
    (1, '9012', 'Juan Perez', '2024-03-31', 'DEBIT', 'MAESTRO', 'EXPIRED', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Comentario: 
-- Si quieres más tarjetas de prueba, descomenta y ajusta los account_id:
/*
INSERT INTO cards (account_id, last_four_digits, card_holder_name, expiration_date, card_type, card_brand, status, created_at)
VALUES 
    (2, '3456', 'Maria Garcia', '2027-01-31', 'CREDIT', 'VISA', 'ACTIVE', CURRENT_TIMESTAMP),
    (2, '7890', 'Maria Garcia', '2025-08-31', 'DEBIT', 'MASTERCARD', 'ACTIVE', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
*/
