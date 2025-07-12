# ETF PAC Simulator Backend 🚀

> Un simulatore avanzato per Piani di Accumulo Capitale (PAC) con ETF, sviluppato con Spring Boot 3.2.2 e Java 17.

## 📖 Sommario

- [Panoramica](#-panoramica)
- [Caratteristiche Principali](#-caratteristiche-principali)
- [Architettura](#️-architettura)
- [Prerequisiti](#-prerequisiti)
- [Installazione](#️-installazione)
- [Configurazione](#-configurazione)
- [API Endpoints](#-api-endpoints)
- [Strategie di Investimento](#-strategie-di-investimento)
- [Testing](#-testing)
- [Deploy](#-deploy)
- [Documentazione](#-documentazione)
- [Contribuire](#-contribuire)
- [Licenza](#-licenza)

## 🚀 Panoramica

ETF PAC Simulator Backend è un'API REST completa per simulare e analizzare Piani di Accumulo Capitale con ETF. Il sistema permette di:

- **Configurare portfolio diversificati** con allocazioni personalizzate
- **Simulare performance future** utilizzando diverse strategie di investimento
- **Eseguire backtesting** su dati storici
- **Confrontare strategie** per identificare quella ottimale
- **Analizzare rischio e rendimento** con metriche avanzate

## ✨ Caratteristiche Principali

### 📊 Gestione Portfolio
- Creazione e gestione di portfolio multi-ETF
- Allocazione percentuale personalizzabile
- Validazione automatica delle allocazioni (somma 100%)
- Template predefiniti per profili di rischio

### 📈 Simulazioni Avanzate
- 6 strategie di investimento disponibili (DCA, Value Averaging, Momentum, etc.)
- Calcolo metriche finanziarie (Sharpe Ratio, Volatilità, Max Drawdown)
- Simulazioni Monte Carlo per analisi probabilistiche
- Supporto per stop loss e take profit

### 🔄 Backtesting Storico
- Test delle strategie su dati storici
- Confronto con benchmark di mercato
- Analisi performance annuale
- Identificazione dei periodi migliori/peggiori

### 🎯 Ottimizzazione Portfolio
- Suggerimenti di allocazione basati sul profilo di rischio
- Analisi di diversificazione
- Calcolo del ribilanciamento necessario
- Confronto tra portfolio multipli

## 🏗️ Architettura

### Stack Tecnologico

- **Backend Framework**: Spring Boot 3.2.2
- **Linguaggio**: Java 17
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Build Tool**: Maven 3.6+
- **Containerization**: Docker & Docker Compose

### Struttura del Progetto

```
etf-pac-simulator-backend/
├── src/main/java/it/university/etfpac/
│   ├── config/           # Configurazioni Spring
│   ├── controller/       # REST Controllers
│   ├── dto/             # Data Transfer Objects
│   ├── entity/          # JPA Entities
│   ├── exception/       # Gestione eccezioni
│   ├── repository/      # JPA Repositories
│   └── service/         # Business Logic
├── src/main/resources/
│   ├── application.yml  # Configurazione principale
│   ├── db/migration/    # Script Flyway
│   └── static/openapi/  # Documentazione API
├── docker/
│   ├── docker/          # Dockerfile
│   └── nginx/           # Configurazione Nginx
└── docker-compose.yml   # Orchestrazione servizi
```

## 📋 Prerequisiti

- Java 17 o superiore
- Maven 3.6+
- Docker e Docker Compose (per l'ambiente completo)
- PostgreSQL 15 (se non usi Docker)
- Redis 7 (se non usi Docker)

## 🛠️ Installazione

### Con Docker (Consigliato)

```bash
# Clona il repository
git clone https://github.com/tuouser/etf-pac-simulator-backend.git
cd etf-pac-simulator-backend

# Avvia tutti i servizi
docker-compose up -d

# Verifica lo stato dei servizi
docker-compose ps

# Visualizza i log
docker-compose logs -f app
```

### Sviluppo Locale

```bash
# Clona il repository
git clone https://github.com/tuouser/etf-pac-simulator-backend.git
cd etf-pac-simulator-backend

# Installa le dipendenze
./mvnw clean install

# Configura il database PostgreSQL
createdb etf_pac_db
psql etf_pac_db -c "CREATE USER etf_user WITH PASSWORD 'etf_password';"
psql etf_pac_db -c "GRANT ALL PRIVILEGES ON DATABASE etf_pac_db TO etf_user;"

# Avvia l'applicazione
./mvnw spring-boot:run -Dspring.profiles.active=local
```

## 🔧 Configurazione

### Profili Spring

L'applicazione supporta diversi profili di configurazione:

- **local**: Sviluppo locale
- **docker**: Esecuzione in container Docker
- **test**: Esecuzione test con H2 in-memory
- **prod**: Produzione

### Variabili d'Ambiente

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=etfpac_db
DB_USERNAME=etfpac_user
DB_PASSWORD=etfpac_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Application
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=docker
```

## 📡 API Endpoints

### ETF Management

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/v1/etfs` | Lista tutti gli ETF disponibili |
| GET | `/api/v1/etfs/{id}` | Dettagli ETF specifico |
| GET | `/api/v1/etfs/filter/risk/{level}` | Filtra ETF per rischio |
| GET | `/api/v1/etfs/top-performing` | Top ETF per performance |
| GET | `/api/v1/etfs/low-cost` | ETF a basso costo |

### Portfolio Management

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/api/v1/portfolios` | Crea nuovo portfolio |
| GET | `/api/v1/portfolios/{id}` | Dettagli portfolio |
| PUT | `/api/v1/portfolios/{id}` | Aggiorna portfolio |
| GET | `/api/v1/portfolios/user/{userId}` | Portfolio utente |
| POST | `/api/v1/portfolios/validate-allocation` | Valida allocazione |
| POST | `/api/v1/portfolios/optimize` | Ottimizza portfolio |

### Simulazioni

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/api/v1/simulations/run` | Esegui simulazione |
| GET | `/api/v1/simulations` | Lista simulazioni |
| GET | `/api/v1/simulations/{id}` | Dettagli simulazione |
| POST | `/api/v1/simulations/compare` | Confronta simulazioni |

### Backtesting

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/api/v1/backtest/run` | Esegui backtest |
| POST | `/api/v1/backtest/compare-strategies` | Confronta strategie |
| GET | `/api/v1/backtest/{id}` | Risultati backtest |

## 📊 Strategie di Investimento

### 1. Dollar Cost Averaging (DCA)
Investimento di importo fisso a intervalli regolari, indipendentemente dal prezzo di mercato.

### 2. Value Averaging
Adatta l'importo investito per raggiungere un valore target predefinito del portfolio.

### 3. Momentum
Aumenta gli investimenti durante i trend positivi e li riduce in quelli negativi.

### 4. Contrarian
Strategia opposta al momentum: compra di più quando i prezzi scendono.

### 5. Smart Beta
Combina elementi di investimento passivo e attivo basandosi su fattori specifici.

### 6. Tactical Asset Allocation
Aggiusta dinamicamente le allocazioni basandosi sulle condizioni di mercato.

## 🧪 Testing

```bash
# Esegui tutti i test
./mvnw test

# Test con coverage
./mvnw test jacoco:report

# Test di integrazione
./mvnw test -Dspring.profiles.active=test

# Test specifici
./mvnw test -Dtest=SimulationServiceTest
```

## 🚀 Deploy

### Deploy con Docker

```bash
# Build dell'immagine
docker build -t etf-pac-simulator:latest .

# Push su registry
docker tag etf-pac-simulator:latest your-registry/etf-pac-simulator:latest
docker push your-registry/etf-pac-simulator:latest

# Deploy su server
docker-compose -f docker-compose.prod.yml up -d
```

### Health Check

```bash
# Verifica stato applicazione
curl http://localhost:8080/actuator/health

# Metriche
curl http://localhost:8080/actuator/metrics

# Info applicazione
curl http://localhost:8080/actuator/info
```

## 📚 Documentazione

### Swagger UI
Disponibile all'indirizzo: `http://localhost:8080/swagger-ui.html`

### OpenAPI Specification
Disponibile all'indirizzo: `http://localhost:8080/v3/api-docs`

### Esempi di Richieste

#### Creazione Portfolio

```bash
curl -X POST http://localhost:8080/api/v1/portfolios \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Portfolio Bilanciato",
    "userId": 1,
    "initialAmount": 10000,
    "monthlyAmount": 500,
    "investmentPeriodMonths": 60,
    "frequency": "MONTHLY",
    "strategy": "DCA",
    "rebalanceFrequency": "QUARTERLY",
    "etfAllocations": {
      "world_equity": 60,
      "bonds": 20,
      "emerging": 15,
      "real_estate": 5
    }
  }'
```

#### Esecuzione Simulazione

```bash
curl -X POST http://localhost:8080/api/v1/simulations/run \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Simulazione DCA 5 anni",
    "initialAmount": 10000,
    "monthlyAmount": 500,
    "investmentPeriod": 60,
    "frequency": "MONTHLY",
    "strategy": "DCA",
    "etfAllocation": {
      "world_equity": 60,
      "bonds": 20,
      "emerging": 15,
      "real_estate": 5
    },
    "riskTolerance": "MODERATE",
    "rebalanceFrequency": "QUARTERLY",
    "userId": 1
  }'
```

## 🔒 Sicurezza

- CORS configurato per ambiente di sviluppo
- Validazione input su tutti gli endpoint
- Protezione contro SQL injection tramite JPA
- Rate limiting configurato su Nginx
- Health check endpoints protetti in produzione

## 📊 Monitoraggio

- **Actuator Endpoints**: Health, metrics, info
- **Logging**: Logback con rotazione giornaliera
- **Cache Monitoring**: Redis stats disponibili
- **Database Monitoring**: Connection pool metrics

## 🐛 Troubleshooting

### Problemi Comuni

#### Database Connection Error
```bash
# Verifica connessione PostgreSQL
docker-compose exec postgres psql -U etfpac_user -d etfpac_db

# Reset database
docker-compose down -v
docker-compose up -d
```

#### Redis Connection Error
```bash
# Verifica Redis
docker-compose exec redis redis-cli ping

# Clear cache
docker-compose exec redis redis-cli FLUSHALL
```

#### Build Error
```bash
# Clean build
./mvnw clean
./mvnw install -DskipTests

# Force update dependencies
./mvnw dependency:purge-local-repository
```

## 🤝 Contribuire

1. Fork il repository
2. Crea un feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit delle modifiche (`git commit -m 'Add some AmazingFeature'`)
4. Push sul branch (`git push origin feature/AmazingFeature`)
5. Apri una Pull Request

### Guidelines

- Segui le convenzioni di codice Java
- Aggiungi test per nuove funzionalità
- Aggiorna la documentazione
- Mantieni il codice pulito e commentato

## 📄 Licenza

Distribuito sotto licenza MIT. Vedi `LICENSE` per maggiori informazioni.

## 👥 Autori

- **Gennaro Ferrara** - *Initial work* - [gennaro.ferrara@pegaso.it](mailto:gennaro.ferrara@pegaso.it)

## 🙏 Ringraziamenti

- Spring Boot Team per l'eccellente framework
- PostgreSQL per il database affidabile
- Redis per la cache performante
- Tutti i contributori del progetto