-- Inserimento ETF di esempio
INSERT INTO etfs (id, name, ticker, expense, risk, sector, aum, dividend, beta, sharpe, max_drawdown, ytd, one_year, three_year, five_year) VALUES
('world_equity', 'FTSE Developed World UCITS ETF', 'VWCE', 0.22, 'HIGH', 'Global Equity', '4.2B', '1.8%', 0.98, 0.85, -34.2, 12.5, 18.3, 8.7, 11.2),
('sp500', 'S&P 500 UCITS ETF', 'VUAA', 0.07, 'HIGH', 'US Large Cap', '8.1B', '1.5%', 1.00, 0.91, -33.8, 15.2, 22.1, 10.1, 13.8),
('europe', 'FTSE Developed Europe UCITS ETF', 'VEUR', 0.12, 'MEDIUM', 'European Equity', '1.8B', '2.8%', 0.85, 0.72, -28.5, 8.7, 14.2, 6.9, 8.4),
('bonds', 'Global Aggregate Bond UCITS ETF', 'VAGF', 0.10, 'LOW', 'Fixed Income', '2.5B', '2.1%', 0.05, 0.45, -8.2, -1.2, 2.8, 1.9, 2.4),
('emerging', 'FTSE Emerging Markets UCITS ETF', 'VFEM', 0.22, 'VERY_HIGH', 'Emerging Markets', '3.2B', '2.4%', 1.15, 0.65, -42.1, 8.9, 12.5, 3.2, 6.8),
('real_estate', 'Global Real Estate UCITS ETF', 'VGRE', 0.25, 'MEDIUM', 'Real Estate', '1.1B', '3.2%', 0.75, 0.58, -35.6, 5.4, 8.9, 4.1, 7.2),
('japan', 'FTSE Japan UCITS ETF', 'VJPN', 0.15, 'MEDIUM', 'Japanese Equity', '2.3B', '2.1%', 0.82, 0.68, -31.2, 10.3, 16.8, 5.4, 9.1),
('small_cap', 'FTSE Developed Small Cap UCITS ETF', 'VSML', 0.25, 'HIGH', 'Small Cap', '0.8B', '1.9%', 1.12, 0.76, -38.9, 14.7, 19.4, 7.8, 10.5),
('commodities', 'Commodities UCITS ETF', 'VCOM', 0.30, 'VERY_HIGH', 'Commodities', '1.5B', '0.0%', 0.65, 0.42, -45.3, 6.8, 11.2, -2.1, 4.3),
('china', 'FTSE China UCITS ETF', 'VCHN', 0.25, 'VERY_HIGH', 'Chinese Equity', '1.9B', '2.8%', 1.25, 0.58, -48.5, -2.3, 8.7, -1.8, 3.9);