-- Creazione tabella portfolios
CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    initial_amount DECIMAL(19,2) NOT NULL,
    monthly_amount DECIMAL(19,2) NOT NULL,
    investment_period_months INTEGER NOT NULL,
    frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    strategy VARCHAR(30) NOT NULL DEFAULT 'DCA',
    rebalance_frequency VARCHAR(20) NOT NULL DEFAULT 'QUARTERLY',
    automatic_rebalance BOOLEAN NOT NULL DEFAULT TRUE,
    stop_loss_percentage DECIMAL(5,2) DEFAULT 0.00,
    take_profit_percentage DECIMAL(5,2) DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_simulated_at TIMESTAMP
);

-- Creazione tabella portfolio_etf_allocations
CREATE TABLE portfolio_etf_allocations (
    portfolio_id BIGINT NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    etf_id VARCHAR(50) NOT NULL REFERENCES etfs(id) ON DELETE CASCADE,
    allocation_percentage DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (portfolio_id, etf_id)
);

-- Indici per performance
CREATE INDEX idx_portfolios_user_id ON portfolios(user_id);
CREATE INDEX idx_portfolios_name ON portfolios(name);
CREATE INDEX idx_portfolio_etf_allocations_portfolio_id ON portfolio_etf_allocations(portfolio_id);