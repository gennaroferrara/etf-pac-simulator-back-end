ETF PAC Simulator Backend
Un simulatore avanzato per Piani di Accumulo Capitale (PAC) con ETF, sviluppato con Spring Boot 3.2.2 e Java 17.
🚀 Panoramica
Questo backend fornisce API REST per simulare strategie di investimento PAC con ETF, permettendo agli utenti di:

Configurare portfolio diversificati
Simulare performance future con diverse strategie
Eseguire backtesting su dati storici
Confrontare strategie di investimento
Analizzare rischio e rendimento

📋 Prerequisiti

Java 17 o superiore
Maven 3.6+
Docker e Docker Compose (per l'ambiente completo)
PostgreSQL 15 (se non usi Docker)
Redis 7 (se non usi Docker)

🛠️ Installazione
Con Docker (Consigliato)

Clona il repository:

bashgit clone https://github.com/tuouser/etf-pac-simulator-backend.git
cd etf-pac-simulator-backend

Avvia l'ambiente completo:

bashdocker-compose up -d
Questo avvierà:

PostgreSQL su porta 5432
Redis su porta 6379
Backend Spring Boot su porta 8080
Nginx su porta 80

Sviluppo Locale

Installa le dipendenze:

bashmvn clean install

Configura il database PostgreSQL:

sqlCREATE DATABASE etfpac_db;
CREATE USER etfpac_user WITH PASSWORD 'etfpac_password';
GRANT ALL PRIVILEGES ON DATABASE etfpac_db TO etfpac_user;

Avvia Redis:

bashdocker run -d -p 6379:6379 redis:7-alpine

Avvia l'applicazione:

bashmvn spring-boot:run
🏗️ Architettura
Stack Tecnologico

Framework: Spring Boot 3.2.2
Database: PostgreSQL 15 con Flyway per migrations
Cache: Redis per caching distribuito
API Doc: OpenAPI 3.0 (Swagger)
Security: Spring Security (attualmente in modalità demo)
Build: Maven

Struttura del Progetto
src/main/java/it/university/etfpac/
├── config/          # Configurazioni (Security, Cache, App)
├── controller/      # REST Controllers
├── dto/            # Data Transfer Objects
│   ├── request/    # DTOs di richiesta
│   └── response/   # DTOs di risposta
├── entity/         # Entità JPA
├── exception/      # Gestione errori custom
├── repository/     # Repository JPA
└── service/        # Logica di business
🔧 Configurazione
Profili Spring

default: Sviluppo locale
docker: Ambiente containerizzato
test: Test automatici

Variabili d'Ambiente
bash# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=etfpac_db
DB_USERNAME=etfpac_user
DB_PASSWORD=etfpac_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Spring
SPRING_PROFILES_ACTIVE=docker
📡 API Endpoints
ETF Management

GET /api/v1/etfs - Lista tutti gli ETF disponibili
GET /api/v1/etfs/{id} - Dettagli ETF specifico
GET /api/v1/etfs/filter/risk/{level} - Filtra per livello di rischio
GET /api/v1/etfs/top-performing - ETF con migliori performance
GET /api/v1/etfs/low-cost - ETF a basso costo

Portfolio Management

POST /api/v1/portfolios - Crea nuovo portfolio
GET /api/v1/portfolios/{id} - Recupera portfolio
PUT /api/v1/portfolios/{id} - Aggiorna portfolio
GET /api/v1/portfolios/user/{userId} - Portfolio per utente
POST /api/v1/portfolios/optimize - Ottimizza allocazione

Simulazioni

POST /api/v1/simulations/run - Esegui simulazione diretta
POST /api/v1/simulations - Salva simulazione
GET /api/v1/simulations/{id} - Recupera simulazione
POST /api/v1/simulations/compare - Confronta simulazioni

Backtesting

POST /api/v1/backtest/run - Esegui backtest storico
POST /api/v1/backtest/compare-strategies - Confronta strategie
GET /api/v1/backtest/{id} - Risultati backtest

📊 Strategie di Investimento
Il simulatore supporta diverse strategie:

DCA (Dollar Cost Averaging): Investimento costante mensile
Value Averaging: Aggiusta l'investimento per mantenere crescita target
Momentum: Investe di più quando il mercato sale
Contrarian: Investe di più quando il mercato scende
Smart Beta: Strategia basata su fattori quantitativi
Tactical: Allocazione tattica basata su condizioni di mercato

🧪 Testing
Unit Test
bashmvn test
Integration Test
bashmvn verify
Test Coverage
bashmvn jacoco:report
Il report sarà disponibile in target/site/jacoco/index.html
🚀 Deploy
Build per Produzione
bashmvn clean package -Pprod
Docker Image
bashdocker build -t etf-pac-simulator:latest -f docker/docker/Dockerfile .
Health Check
L'applicazione espone endpoint di health check:

/actuator/health - Stato generale
/actuator/metrics - Metriche applicative

📚 Documentazione API
La documentazione completa delle API è disponibile via Swagger UI:

Sviluppo: http://localhost:8080/swagger-ui.html
Specifica OpenAPI: http://localhost:8080/v3/api-docs

🔒 Sicurezza
Attualmente l'applicazione è in modalità demo senza autenticazione. Per un ambiente di produzione, attivare:

JWT Authentication
Rate limiting
CORS configuration
HTTPS

🐛 Troubleshooting
Database Connection
bash# Verifica connessione PostgreSQL
docker exec -it etfpac-postgres psql -U etfpac_user -d etfpac_db
Redis Connection
bash# Verifica Redis
docker exec -it etfpac-redis redis-cli ping
Logs
bash# Logs applicazione
docker logs etfpac-app

# Logs completi
docker-compose logs -f
🤝 Contribuire

Fork il progetto
Crea un branch feature (git checkout -b feature/AmazingFeature)
Commit le modifiche (git commit -m 'Add some AmazingFeature')
Push al branch (git push origin feature/AmazingFeature)
Apri una Pull Request

📄 Licenza
Questo progetto è distribuito sotto licenza MIT. Vedi LICENSE per maggiori informazioni.
👥 Autori

Gennaro Ferrara - Sviluppo iniziale - gennaro.ferrara@pegaso.it

🙏 Ringraziamenti

Spring Boot Team per l'eccellente framework
PostgreSQL per il database robusto
Redis per la cache performante
La community open source per le librerie utilizzate