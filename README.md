# 🎙️ Real-Time Media Transcription Platform

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A production-ready, scalable platform for automatic speech-to-text transcription of audio and video files using AI-powered Whisper models. Built with modern microservices architecture and event-driven design.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Monitoring](#monitoring)
- [Contributing](#contributing)
- [License](#license)

---

## ✨ Features

### Core Capabilities
- 🎤 **AI-Powered Transcription** - OpenAI Whisper integration for accurate speech-to-text
- 📁 **Multi-Format Support** - Handles audio (MP3, WAV, M4A) and video (MP4, AVI, MOV) files
- 🌍 **Multi-Language** - Automatic language detection, supports 90+ languages
- ⚡ **Event-Driven Architecture** - Asynchronous processing using Apache Kafka
- 💾 **Intelligent Caching** - Redis-based caching for fast retrieval
- 📊 **Real-Time Status** - Track transcription progress in real-time
- 🔄 **Automatic Retry** - Built-in error handling and retry mechanisms
- 📦 **Object Storage** - MinIO (S3-compatible) for scalable file storage

### Technical Highlights
- **Microservices Architecture** - Loosely coupled, independently deployable services
- **RESTful APIs** - Clean, well-documented API endpoints
- **Containerized Deployment** - Full Docker Compose orchestration
- **Production Ready** - Health checks, metrics, and monitoring
- **Scalable Design** - Horizontal scaling support with Kafka partitioning

---

## 🏗️ Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTP
       ▼
┌─────────────────────────────────────────────────────────┐
│            Spring Boot Application                      │
│  ┌────────────┐  ┌─────────────┐  ┌──────────────┐      │
│  │ REST APIs  │  │ Kafka       │  │ File Upload  │      │
│  │ Controller │  │ Consumer    │  │ Service      │      │
│  └────────────┘  └─────────────┘  └──────────────┘      │
└────┬─────────┬────────────┬───────────┬──────────────── ┘
     │         │            │           │
     ▼         ▼            ▼           ▼
┌─────────┐ ┌──────┐  ┌──────────┐ ┌─────────┐
│ MinIO   │ │Kafka │  │PostgreSQL│ │ Redis   │
│ Storage │ │Queue │  │ Database │ │ Cache   │
└─────────┘ └──┬───┘  └──────────┘ └─────────┘
               │
               ▼
       ┌───────────────┐
       │ Whisper AI    │
       │ Service       │
       │ (Python)      │
       └───────────────┘
```

### Data Flow

1. **Upload** → User uploads media file via REST API
2. **Store** → File saved to MinIO, metadata to PostgreSQL
3. **Event** → Kafka event published to `media.uploaded` topic
4. **Process** → Consumer picks up event, updates status to `PROCESSING`
5. **Transcribe** → Whisper AI service performs speech-to-text
6. **Save** → Result stored in PostgreSQL and cached in Redis
7. **Notify** → Completion event published to `media.transcribed` topic
8. **Retrieve** → User fetches transcription via API (from cache or DB)

---

## 🛠️ Technology Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 17 | Programming language |
| **Spring Boot** | 3.2.0 | Application framework |
| **Spring Data JPA** | 3.2.0 | Database access |
| **Spring Kafka** | 3.1.0 | Event streaming |
| **PostgreSQL** | 15 | Relational database |
| **Redis** | 7 | Caching layer |
| **Apache Kafka** | 7.5.0 | Message broker |
| **MinIO** | Latest | Object storage |

### AI/ML
| Technology | Purpose |
|------------|---------|
| **OpenAI Whisper** | Speech-to-text transcription |
| **Python** | 3.10 | AI service runtime |
| **Flask** | REST API wrapper |
| **PyTorch** | Deep learning framework |

### DevOps
| Technology | Purpose |
|------------|---------|
| **Docker** | Containerization |
| **Docker Compose** | Orchestration |
| **Maven** | Build automation |

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Docker** and **Docker Compose**
- **Git**
- At least **4GB RAM** available

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/media-transcription-platform.git
   cd media-transcription-platform
   ```

2. **Start infrastructure services**
   ```bash
   docker compose up -d
   ```
   
   This starts:
   - PostgreSQL (port 5432)
   - Redis (port 6379)
   - Kafka + Zookeeper (port 9092)
   - MinIO (ports 9000, 9001)
   - Whisper AI Service (port 8001)

3. **Wait for services to be ready** (~2 minutes)
   ```bash
   # Check all services are running
   docker compose ps
   
   # Verify Whisper service
   curl http://localhost:8001/health
   ```

4. **Build the application**
   ```bash
   mvn clean install
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

6. **Verify installation**
   ```bash
   curl http://localhost:8080/api/v1/health
   ```

### Quick Test

```bash
# Upload a media file
curl -X POST http://localhost:8080/api/v1/media/upload \
  -F "file=@sample.mp3;type=audio/mpeg"

# Check transcription status (replace {id} with returned ID)
curl http://localhost:8080/api/v1/transcription/media/{id}/status

# Get transcription result
curl http://localhost:8080/api/v1/transcription/media/{id}

# Download transcription as text file
curl http://localhost:8080/api/v1/transcription/media/{id}/download -o transcript.txt
```

---

## 📡 API Documentation

### Base URL
```
http://localhost:8080/api/v1
```

### Endpoints

#### Media Upload

**Upload File**
```http
POST /media/upload
Content-Type: multipart/form-data

Body: file (audio/video file)
```

**Response:**
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "id": 1,
    "originalFilename": "audio.mp3",
    "mediaType": "AUDIO",
    "status": "UPLOADED",
    "fileSize": 2048576,
    "uploadedAt": "2025-10-18T10:30:00"
  }
}
```

#### Transcription

**Get Transcription**
```http
GET /transcription/media/{mediaFileId}
```

**Response:**
```json
{
  "success": true,
  "message": "Transcription retrieved successfully",
  "data": {
    "id": 1,
    "mediaFileId": 1,
    "transcript": "This is the transcribed text...",
    "language": "en",
    "confidence": 0.95,
    "wordCount": 150,
    "processingTimeMs": 2500,
    "completedAt": "2025-10-18T10:32:00"
  }
}
```

**Check Status**
```http
GET /transcription/media/{mediaFileId}/status
```

**Download Transcription**
```http
GET /transcription/media/{mediaFileId}/download
```

**Get All Media Files**
```http
GET /media
```

**Get Media by Status**
```http
GET /media/status/{status}
```
Status values: `UPLOADED`, `PROCESSING`, `COMPLETED`, `FAILED`

### Health & Monitoring

**Application Health**
```http
GET /health
```

**Actuator Endpoints**
```http
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

---

## ⚙️ Configuration

### Application Configuration

Edit `src/main/resources/application.yml`:

```yaml
app:
  upload:
    temp-dir: /tmp/media-uploads
  transcription:
    max-retries: 3
    ai-service-url: http://localhost:8001/transcribe
  cache:
    ttl: 86400  # 1 day in seconds
```

### Whisper Model Selection

Edit `docker-compose.yml`:

```yaml
whisper-service:
  environment:
    WHISPER_MODEL: base  # Options: tiny, base, small, medium, large
```

| Model | Size | RAM | Speed | Accuracy |
|-------|------|-----|-------|----------|
| tiny | 39M | ~1GB | Very Fast | Low |
| **base** | 74M | ~1GB | Fast | Good |
| small | 244M | ~2GB | Medium | Better |
| medium | 769M | ~5GB | Slow | High |
| large | 1550M | ~10GB | Very Slow | Best |

**Recommended:** `base` for production (good balance of speed and accuracy)

### Database Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/transcription_db
    username: postgres
    password: postgres
```

### Kafka Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: transcription-consumer-group
```

---

## 🚢 Deployment

### Docker Deployment

**Production docker-compose:**
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - postgres
      - kafka
      - redis
      - minio
      - whisper-service
```

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=transcription_db
DB_USER=postgres
DB_PASSWORD=your-password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

### Kubernetes Deployment

See `k8s/` directory for Kubernetes manifests (coming soon)

---

## 📊 Monitoring

### Metrics

The application exposes Prometheus metrics at:
```
http://localhost:8080/actuator/prometheus
```

**Key Metrics:**
- `kafka_consumer_records_consumed_total` - Kafka messages processed
- `http_server_requests_seconds_count` - API request count
- `cache_gets_total` - Cache access statistics
- `jvm_memory_used_bytes` - Memory usage

### Logging

Application logs are available at:
```bash
# Application logs
tail -f logs/application.log

# Docker logs
docker compose logs -f whisper-service
docker compose logs -f kafka
```

### Service Monitoring

**MinIO Console:** http://localhost:9001
- Login: minioadmin / minioadmin
- View uploaded files and storage metrics

**Kafka UI:** http://localhost:8082
- View topics, messages, and consumer groups

---

## 🧪 Testing

### Run Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Load Testing
```bash
# Using Apache Bench
ab -n 100 -c 10 http://localhost:8080/api/v1/health
```

---

## 🔧 Troubleshooting

### Common Issues

**Issue: Whisper service not starting**
```bash
# Check logs
docker compose logs whisper-service

# Solution: Use smaller model
# Edit docker-compose.yml: WHISPER_MODEL: tiny
docker compose restart whisper-service
```

**Issue: Transcription stuck in PROCESSING**
```bash
# Check Kafka consumer
docker compose logs kafka

# Reset Kafka offset
docker exec transcription-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group transcription-consumer-group \
  --reset-offsets --to-earliest --topic media.uploaded --execute
```

**Issue: Out of memory**
```bash
# Increase Docker memory limit
# Docker Desktop → Settings → Resources → Memory: 6GB+
```

### Debug Mode

Run with debug logging:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.root=DEBUG"
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Java coding conventions
- Write unit tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting PR

---

## 🙏 Acknowledgments

- [OpenAI Whisper](https://github.com/openai/whisper) - Speech recognition model
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [Apache Kafka](https://kafka.apache.org/) - Event streaming platform
- [MinIO](https://min.io/) - Object storage

---

## 🗺️ Roadmap

- [ ] Real-time streaming transcription (WebSocket)
- [ ] Speaker diarization (identify multiple speakers)
- [ ] Subtitle generation (SRT, VTT formats)
- [ ] GPU acceleration support
- [ ] Kubernetes deployment manifests
- [ ] Web-based admin dashboard
- [ ] Multi-tenant support
- [ ] Advanced audio preprocessing

---

<div align="center">

**⭐ Star this repository if you find it helpful!**

</div>
