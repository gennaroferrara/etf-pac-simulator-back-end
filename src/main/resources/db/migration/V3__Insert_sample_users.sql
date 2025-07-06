-- V3__insert_sample_users.sql

INSERT INTO users (
    email, name, risk_profile, experience, total_portfolio, active_simulations
) VALUES
('marco.rossi@studente.it',   'Marco Rossi',       'MODERATO',   'INTERMEDIO', 45000.00, 3),
('anna.ferrari@example.com',  'Anna Ferrari',      'CONSERVATIVO','PRINCIPIANTE',25000.00, 1),
('luigi.bianchi@example.com', 'Luigi Bianchi',     'AGGRESSIVO', 'AVANZATO',   120000.00, 5),
('giulia.verde@example.com',  'Giulia Verde',      'MODERATO',   'INTERMEDIO', 67000.00, 2),
('francesco.neri@example.com','Francesco Neri',    'AGGRESSIVO', 'AVANZATO',   89000.00, 4);