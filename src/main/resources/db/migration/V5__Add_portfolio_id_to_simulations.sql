-- Aggiungi colonna portfolio_id alla tabella simulations
ALTER TABLE simulations ADD COLUMN portfolio_id BIGINT REFERENCES portfolios(id);

-- Crea indice per performance
CREATE INDEX idx_simulations_portfolio_id ON simulations(portfolio_id);