
# FresherRadar 🎯

**AI-Powered Job Discovery & Classification Platform for Freshers**

FresherRadar is a job discovery platform designed to help fresh graduates and early-career professionals find relevant entry-level opportunities. It collects job listings, classifies them using Grok AI, and presents structured job information through a simple web interface.

## 🚀 Features

- **Automated Job Collection** – Collects job listings from supported sources.
- **AI-Powered Classification** – Uses Grok AI to classify job opportunities.
- **Fresher-Focused Discovery** – Helps identify entry-level and early-career roles.
- **Job Filtering** – Explore jobs based on available categories and information.
- **REST API** – Spring Boot backend exposes endpoints for job data and system health.
- **Cost Metrics** – Tracks AI-related evaluation and processing costs.
- **PostgreSQL Database** – Stores jobs, collection runs, and classification records.
- **Cloud Deployment** – Backend and frontend deployed independently on Render.

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.3.5 |
| Build Tool | Apache Maven |
| Frontend | HTML, CSS, JavaScript |
| Database | PostgreSQL (Neon) |
| AI Integration | Grok API |
| Database Migrations | Flyway |
| Deployment | Render |
| Version Control | Git & GitHub |

## 🌐 Live Demo

- **Frontend:** https://fresherradar.onrender.com
- **Backend:** https://fresherradar-backend.onrender.com
- **Health Check:** https://fresherradar-backend.onrender.com/api/health
- **Jobs API:** https://fresherradar-backend.onrender.com/api/jobs?size=1000

## 📂 Project Structure

```text
FresherRadar/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fresherradar/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   ├── service/
│   │   │   │   ├── classifier/
│   │   │   │   └── exception/
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── db/migration/
│   │   └── test/
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/
│   ├── index.html
│   ├── style.css
│   ├── script.js
│   └── README.md
│
├── .gitignore
└── README.md
```

## ⚙️ Local Setup

### Prerequisites

Make sure you have the following installed:

- Java 21
- Apache Maven
- PostgreSQL
- Git

### 1. Clone the Repository

```bash
git clone https://github.com/karanpatole283/FresherRadar.git
cd FresherRadar
```

### 2. Configure Environment Variables

Configure the following environment variables before starting the backend:

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `LLM_API_KEY` | Google Gemini API key |
| `ADMIN_TOKEN` | Secret token for admin endpoints |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins |

Example PowerShell configuration:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/fresherradar"
$env:DB_USERNAME = "your_db_username"
$env:DB_PASSWORD = "your_db_password"
$env:LLM_API_KEY = "your_gemini_api_key"
$env:ADMIN_TOKEN = "your_admin_token"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5500,http://127.0.0.1:5500"
```

**Security:** Never commit API keys, database passwords, admin tokens, or `.env` files to GitHub.

### 3. Start the Backend

Navigate to the backend directory:

```powershell
cd backend
```

Run the Spring Boot application:

```powershell
mvn spring-boot:run
```

Backend runs at:

```text
http://localhost:8080
```

### 4. Start the Frontend

Open a second PowerShell terminal from the project root:

```powershell
cd frontend
```

Start a local static server using PowerShell:

```powershell
$listener = [System.Net.HttpListener]::new()
$listener.Prefixes.Add("http://localhost:5500/")
$listener.Start()

while ($listener.IsListening) {
    $context = $listener.GetContext()
    $file = Join-Path (Get-Location) $context.Request.Url.LocalPath.TrimStart('/')

    if (Test-Path $file -PathType Leaf) {
        $bytes = [System.IO.File]::ReadAllBytes($file)
        $context.Response.StatusCode = 200
        $context.Response.ContentType = switch ([System.IO.Path]::GetExtension($file)) {
            ".html" { "text/html" }
            ".css"  { "text/css" }
            ".js"   { "application/javascript" }
            default { "application/octet-stream" }
        }
        $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
    } else {
        $context.Response.StatusCode = 404
    }

    $context.Response.Close()
}
```

Open:

```text
http://localhost:5500
```

To stop the local server, press `Ctrl + C`.

## 🔌 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/health` | Checks backend and database health |
| GET | `/api/jobs` | Retrieves available jobs |
| GET | `/api/jobs?size=1000` | Retrieves up to 1000 jobs |

Admin endpoints require the `X-Admin-Token` request header.

## 🤖 AI Integration

FresherRadar integrates Google's Gemini API for AI-powered job classification.

- **Provider:** Google Gemini
- **Default Model:** `gemini-2.5-flash`
- **Configuration:** `LLM_API_KEY` environment variable

The AI integration helps process and classify job information into structured categories.

## ☁️ Deployment

The project uses separate Render services:

| Service | Deployment |
|---|---|
| Frontend | Render Static Site |
| Backend | Render Web Service (Docker) |
| Database | Neon PostgreSQL |

The backend connects to Neon PostgreSQL using environment-based configuration. The frontend communicates with the deployed backend through its REST API.

## 🔒 Security

- Credentials are managed through environment variables.
- Database passwords and API keys are not intended to be committed.
- Admin endpoints are protected using an admin token.
- CORS is configured through allowed frontend origins.
- Sensitive local and backup files are excluded using `.gitignore`.

## 🎯 Project Objective

The goal of FresherRadar is to simplify job discovery for freshers by combining automated job collection, AI-powered classification, and a centralized interface.

## 👨‍💻 Author

**Chandrakant Patole**

GitHub: [@karanpatole283](https://github.com/karanpatole283)

---

<p align="center">
  Built to make fresher job discovery smarter and simpler.
</p>
