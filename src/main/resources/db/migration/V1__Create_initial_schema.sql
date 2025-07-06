-- Creazione tabella users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    risk_profile VARCHAR(50) NOT NULL CHECK (risk_profile IN ('CONSERVATIVO', 'MODERATO', 'AGGRESSIVO')),
    experience VARCHAR(50) NOT NULL CHECK (experience IN ('PRINCIPIANTE', 'INTERMEDIO', 'AVANZATO')),
    total_portfolio DECIMAL(15,2),
    active_simulations INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Creazione tabella etfs
CREATE TABLE etfs (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    ticker VARCHAR(20) NOT NULL,
    expense DECIMAL(5,3) NOT NULL,
    risk VARCHAR(20) NOT NULL CHECK (risk IN ('LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH')),
    sector VARCHAR(100),
    aum VARCHAR(20),
    dividend VARCHAR(10),
    beta DECIMAL(5,3),
    sharpe DECIMAL(5,3),
    max_drawdown DECIMAL(6,2),
    ytd DECIMAL(6,2),
    one_year DECIMAL(6,2),
    three_year DECIMAL(6,2),
    five_year DECIMAL(6,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Creazione tabella simulations
CREATE TABLE simulations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    initial_amount DECIMAL(15,2) NOT NULL,
    monthly_amount DECIMAL(15,2) NOT NULL,
    investment_period INTEGER NOT NULL,
    frequency VARCHAR(20) NOT NULL CHECK (frequency IN ('MONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'ANNUAL')),
    strategy VARCHAR(30) NOT NULL CHECK (strategy IN ('DCA', 'VALUE_AVERAGING', 'MOMENTUM', 'CONTRARIAN', 'SMART_BETA', 'TACTICAL')),
    risk_tolerance VARCHAR(20) NOT NULL CHECK (risk_tolerance IN ('CONSERVATIVE', 'MODERATE', 'AGGRESSIVE')),
    rebalance_frequency VARCHAR(20) NOT NULL CHECK (rebalance_frequency IN ('MONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'ANNUAL')),
    automatic_rebalance BOOLEAN DEFAULT TRUE,
    stop_loss DECIMAL(5,2),
    take_profit_target DECIMAL(5,2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    final_value DECIMAL(15,2),
    total_invested DECIMAL(15,2),
    cumulative_return DECIMAL(8,3),
    volatility DECIMAL(8,3),
    max_drawdown DECIMAL(8,3),
    sharpe_ratio DECIMAL(8,3),
    win_rate DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Creazione tabella simulation_allocations
CREATE TABLE simulation_allocations (
    id BIGSERIAL PRIMARY KEY,
    simulation_id BIGINT NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
    etf_id VARCHAR(50) NOT NULL REFERENCES etfs(id) ON DELETE CASCADE,
    percentage DECIMAL(5,2) NOT NULL CHECK (percentage >= 0 AND percentage <= 100),
    UNIQUE(simulation_id, etf_id)
);

-- Creazione tabella simulation_data
CREATE TABLE simulation_data (
    id BIGSERIAL PRIMARY KEY,
    simulation_id BIGINT NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
    month INTEGER NOT NULL,
    total_value DECIMAL(15,2) NOT NULL,
    total_invested DECIMAL(15,2) NOT NULL,
    monthly_investment DECIMAL(15,2),
    monthly_return DECIMAL(8,4),
    cumulative_return DECIMAL(8,3),
    inflation_adjusted_value DECIMAL(15,2),
    sharpe_ratio DECIMAL(8,3),
    UNIQUE(simulation_id, month)
);

-- Creazione indici per performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_simulations_user_id ON simulations(user_id);
CREATE INDEX idx_simulations_status ON simulations(status);
CREATE INDEX idx_simulations_created_at ON simulations(created_at);
CREATE INDEX idx_simulation_allocations_simulation_id ON simulation_allocations(simulation_id);
CREATE INDEX idx_simulation_data_simulation_id ON simulation_data(simulation_id);
CREATE INDEX idx_simulation_data_month ON simulation_data(simulation_id, month);