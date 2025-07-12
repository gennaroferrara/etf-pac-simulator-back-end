# ETF PAC Simulator Backend 🚀

> Un simulatore avanzato per Piani di Accumulo Capitale (PAC) con ETF, sviluppato con Spring Boot 3.2.2 e Java 17.

---

## 📖 Sommario

- [Panoramica](#-panoramica)  
- [Prerequisiti](#-prerequisiti)  
- [Installazione](#-installazione)  
  - [Con Docker (Consigliato)](#con-docker-consigliato)  
  - [Sviluppo Locale](#sviluppo-locale)  
- [🏗️ Architettura](#️-architettura)  
  - [Stack Tecnologico](#stack-tecnologico)  
  - [Struttura del Progetto](#struttura-del-progetto)  
- [🔧 Configurazione Profili Spring](#-configurazione-profili-spring)  
- [📡 Variabili d’Ambiente](#-variabili-daambiente)  
- [📡 API Endpoints](#-api-endpoints)  
  - [ETF Management](#etf-management)  
  - [Portfolio Management](#portfolio-management)  
  - [Simulazioni](#simulazioni)  
  - [Backtesting](#backtesting)  
- [📊 Strategie di Investimento](#-strategie-di-investimento)  
- [🧪 Testing](#-testing)  
- [🚀 Deploy](#-deploy)  
- [✅ Health Check](#-health-check)  
- [📚 Documentazione API](#-documentazione-api)  
- [🔒 Sicurezza](#-sicurezza)  
- [🐛 Troubleshooting](#-troubleshooting)  
- [🤝 Contribuire](#-contribuire)  
- [📄 Licenza](#-licenza)  
- [👥 Autori](#-autori)  
- [🙏 Ringraziamenti](#-ringraziamenti)  

---

## 🚀 Panoramica

Un simulatore avanzato per Piani di Accumulo Capitale (PAC) con ETF, che fornisce API REST per:

- Configurare portfolio diversificati  
- Simulare performance future con diverse strategie  
- Eseguire backtesting su dati storici  
- Confrontare strategie di investimento  
- Analizzare rischio e rendimento  

---

## 📋 Prerequisiti

- Java 17 o superiore  
- Maven 3.6+  
- Docker e Docker Compose (per l’ambiente completo)  
- PostgreSQL 15 (se non usi Docker)  
- Redis 7 (se non usi Docker)  

---

## 🛠️ Installazione

### Con Docker (Consigliato)

```bash
git clone https://github.com/tuouser/etf-pac-simulator-backend.git
cd etf-pac-simulator-backend
docker-compose up -d
