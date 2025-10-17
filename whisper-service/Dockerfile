# -------------------------------
# Stage 1: Base Image
# -------------------------------
FROM python:3.10-slim

# Avoid Python buffering issues and create a dedicated user
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    WHISPER_MODEL=base \
    HOST=0.0.0.0 \
    PORT=8001

# Set working directory
WORKDIR /app

# Install system dependencies (for ffmpeg and audio processing)
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Upgrade pip
RUN pip install --upgrade pip

# Copy requirements or install directly
# (you can later replace with a requirements.txt)
RUN pip install torch==2.2.2 \
    torchvision==0.17.2 \
    torchaudio==2.2.2 \
    flask==3.0.3 \
    openai-whisper==20231117

# Copy application files
COPY . /app

# Expose service port
EXPOSE 8001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8001/health || exit 1

# Default command
CMD ["python", "app.py"]
