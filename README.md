# Sentiment API Orchestration

Spring Boot REST API for sentiment analysis using Stanford CoreNLP, packaged for local Docker runs and Kubernetes deployment. Includes health checks and Prometheus metrics.

## Project Layout

- `sentiment-app/` Spring Boot application (Java 21, Maven)
- `sentiment-app/src/main/java/com/example/sentimentapi/` application code
- `sentiment-app/src/main/resources/application.yml` runtime configuration
- `sentiment-app/Dockerfile` multi-stage build for container image
- `k8s/` Kubernetes manifests (Deployment, Service, Ingress, HPA, ServiceMonitor)

## Requirements

- Java 21 (local runs)
- Maven 3.9+ (local builds)
- Docker (container builds/runs)
- Kubernetes cluster (for `k8s/` manifests)

## Build and Run (Local)

From the repo root:

```bash
cd sentiment-app
mvn -DskipTests package
```

Run the app:

```bash
java -jar target/sentiment-api-0.0.1-SNAPSHOT.jar
```

The server listens on `http://localhost:8080`.

## Docker

Build image:

```bash
docker build -t sentiment-api:latest sentiment-app
```

Run container:

```bash
docker run --rm -p 8080:8080 sentiment-api:latest
```

## API Endpoints

Base path: `/api`

### 1) Simple sentiment

`GET /api/sentiment?text=...`

Example:

```bash
curl "http://localhost:8080/api/sentiment?text=I%20love%20this%20product"
```

Response:

```json
{
  "sentiment": "positive",
  "text": "I love this product"
}
```

### 2) Detailed sentiment

`GET /api/sentiment/detailed?text=...`

Example:

```bash
curl "http://localhost:8080/api/sentiment/detailed?text=This%20is%20okay"
```

Response (shape):

```json
{
  "text": "This is okay",
  "sentiment": "neutral",
  "confidence": "55.12%",
  "scores": {
    "veryNegative": "1.20%",
    "negative": "8.30%",
    "neutral": "55.12%",
    "positive": "30.45%",
    "veryPositive": "4.93%"
  }
}
```

### 3) Batch sentiment

`POST /api/sentiment/batch`

Body:

```json
{
  "texts": ["I love it", "This is bad", "It is fine"]
}
```

Example:

```bash
curl -X POST "http://localhost:8080/api/sentiment/batch" \
  -H "Content-Type: application/json" \
  -d '{"texts":["I love it","This is bad","It is fine"]}'
```

Response (shape):

```json
{
  "results": [
    { "text": "I love it", "sentiment": "positive", "confidence": 0.73 },
    { "text": "This is bad", "sentiment": "negative", "confidence": 0.81 },
    { "text": "It is fine", "sentiment": "neutral", "confidence": 0.58 }
  ],
  "count": 3
}
```

## Health and Metrics

Spring Boot Actuator is enabled.

- Liveness: `GET /actuator/health/liveness`
- Readiness: `GET /actuator/health/readiness`
- Metrics (Prometheus): `GET /actuator/prometheus`

## Configuration

`sentiment-app/src/main/resources/application.yml`:

- Server port: `8080`
- Actuator exposure: `health`, `info`, `prometheus`
- Health probes enabled for Kubernetes liveness/readiness

## CORS

The REST controller allows browser requests from:

- `http://127.0.0.1:5500`
- `http://localhost:5500`

## Kubernetes Deployment

Manifests are in `k8s/`:

- `deployment.yaml` 3 replicas, probes, resource requests/limits, Prometheus annotations
- `service.yaml` LoadBalancer on port 80 -> 8080
- `ingress.yaml` routes `/api` to the service
- `hpa.yaml` CPU-based autoscaling (3–10 replicas)
- `serviceMonitor.yaml` Prometheus Operator scrape config

Apply (example):

```bash
kubectl apply -f k8s/
```

## Notes

- Sentiment labels are derived from Stanford CoreNLP’s 0–4 scale and mapped to `negative`, `neutral`, `positive`.
- The CoreNLP English model artifacts are included via Maven dependencies, so first startup can be slower.
