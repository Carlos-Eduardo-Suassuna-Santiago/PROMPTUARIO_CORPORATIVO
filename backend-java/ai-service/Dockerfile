FROM python:3.12-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc curl && rm -rf /var/lib/apt/lists/*

COPY backend-java/ai-service/requirements.txt .
RUN pip install --default-timeout=120 --no-cache-dir -r requirements.txt

# Shared contracts and infrastructure for the Java-migration stack.
COPY backend-java/shared /app/shared
COPY backend-java/ai-service/app ./app

EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
  CMD curl -f http://localhost:8000/healthz || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
