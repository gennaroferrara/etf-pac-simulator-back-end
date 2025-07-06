-- V1__init_schema.sql

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    risk_profile VARCHAR(50) NOT NULL CHECK (risk_profile IN ('CONSERVATIVO', 'MODERATO', 'AGGRESSIVO')),
    experience VARCHAR(50) NOT NULL CHECK (experience IN ('PRINCIPIANTE', 'INTERMEDIO', 'AVANZATO')),
    total_portfolio DOUBLE PRECISION,
    active_simulations INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE etfs (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    ticker VARCHAR(20) NOT NULL,
    expense DOUBLE PRECISION NOT NULL,
    risk VARCHAR(20) NOT NULL CHECK (risk IN ('LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH')),
    sector VARCHAR(100),
    aum VARCHAR(20),
    dividend VARCHAR(10),
    beta DOUBLE PRECISION,
    sharpe DOUBLE PRECISION,
    max_drawdown DOUBLE PRECISION,
    ytd DOUBLE PRECISION,
    one_year DOUBLE PRECISION,
    three_year DOUBLE PRECISION,
    five_year DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    initial_amount DECIMAL(19,2) NOT NULL,
    monthly_amount DECIMAL(19,2) NOT NULL,
    investment_period_months INTEGER NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    strategy VARCHAR(30) NOT NULL,
    rebalance_frequency VARCHAR(20) NOT NULL,
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

CREATE TABLE portfolio_etf_allocations (
    portfolio_id BIGINT NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    etf_id VARCHAR(50) NOT NULL REFERENCES etfs(id) ON DELETE CASCADE,
    allocation_percentage DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (portfolio_id, etf_id)
);

CREATE TABLE simulations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    portfolio_id BIGINT REFERENCES portfolios(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    initial_amount DOUBLE PRECISION NOT NULL,
    monthly_amount DOUBLE PRECISION NOT NULL,
    investment_period INTEGER NOT NULL,
    frequency VARCHAR(20) NOT NULL CHECK (frequency IN ('MONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'ANNUAL')),
    strategy VARCHAR(30) NOT NULL CHECK (strategy IN ('DCA', 'VALUE_AVERAGING', 'MOMENTUM', 'CONTRARIAN', 'SMART_BETA', 'TACTICAL')),
    risk_tolerance VARCHAR(20) NOT NULL CHECK (risk_tolerance IN ('CONSERVATIVE', 'MODERATE', 'AGGRESSIVE')),
    rebalance_frequency VARCHAR(20) NOT NULL CHECK (rebalance_frequency IN ('MONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'ANNUAL')),
    automatic_rebalance BOOLEAN DEFAULT TRUE,
    stop_loss DOUBLE PRECISION,
    take_profit_target DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    final_value DOUBLE PRECISION,
    total_invested DOUBLE PRECISION,
    cumulative_return DOUBLE PRECISION,
    volatility DOUBLE PRECISION,
    max_drawdown DOUBLE PRECISION,
    sharpe_ratio DOUBLE PRECISION,
    win_rate DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE simulation_allocations (
    id BIGSERIAL PRIMARY KEY,
    simulation_id BIGINT NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
    etf_id VARCHAR(50) NOT NULL REFERENCES etfs(id) ON DELETE CASCADE,
    percentage DOUBLE PRECISION NOT NULL CHECK (percentage >= 0 AND percentage <= 100),
    UNIQUE(simulation_id, etf_id)
);

CREATE TABLE simulation_data (
    id BIGSERIAL PRIMARY KEY,
    simulation_id BIGINT NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
    month INTEGER NOT NULL,
    total_value DOUBLE PRECISION NOT NULL,
    total_invested DOUBLE PRECISION NOT NULL,
    monthly_investment DOUBLE PRECISION,
    monthly_return DOUBLE PRECISION,
    cumulative_return DOUBLE PRECISION,
    inflation_adjusted_value DOUBLE PRECISION,
    sharpe_ratio DOUBLE PRECISION,
    UNIQUE(simulation_id, month)
);

-- Indici generali
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_simulations_user_id ON simulations(user_id);
CREATE INDEX idx_simulations_status ON simulations(status);
CREATE INDEX idx_simulations_created_at ON simulations(created_at);
CREATE INDEX idx_simulation_allocations_simulation_id ON simulation_allocations(simulation_id);
CREATE INDEX idx_simulation_data_simulation_id ON simulation_data(simulation_id);
CREATE INDEX idx_simulation_data_month ON simulation_data(simulation_id, month);
CREATE INDEX idx_portfolios_user_id ON portfolios(user_id);
CREATE INDEX idx_portfolios_name ON portfolios(name);
CREATE INDEX idx_portfolio_etf_allocations_portfolio_id ON portfolio_etf_allocations(portfolio_id);
