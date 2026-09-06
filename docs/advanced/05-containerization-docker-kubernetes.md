# Advanced · Lesson 05 — Containerization: Docker & Kubernetes

## Goal

Package every service as a container image and understand the leap from
Docker Compose (single host, this course's default) to Kubernetes
(multi-node, production-grade orchestration).

## Dockerizing a service

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

See e.g. [`order-service/Dockerfile`](../../order-management-system/order-service/Dockerfile) —
every service has an identical one. `eclipse-temurin:21-jre-alpine` is a
**JRE-only** (not full JDK) Java 21 base image on Alpine Linux — smaller
attack surface and image size than shipping a full JDK or `latest` tag (never
use floating tags like `latest` for anything you deploy — pin versions).

Build and run one image manually:

```bash
cd order-management-system
mvn -pl order-service -am clean package -DskipTests
cd order-service
docker build -t oms-training/order-service:1.0.0 .
docker run -p 8083:8083 oms-training/order-service:1.0.0
```

## The whole system: Docker Compose

See [`docker-compose.yml`](../../order-management-system/docker-compose.yml).
It defines Kafka, every microservice (built from its own `Dockerfile`), and
wires them together with container-name-based hostnames
(`http://eureka-server:8761/eureka/` — Docker's embedded DNS resolves
`eureka-server` to the right container automatically, no Eureka needed for
*this* particular piece of discovery).

```bash
cd order-management-system
mvn clean package -DskipTests          # produces target/*.jar for every module
docker compose up -d --build
```

## From Compose to Kubernetes

Docker Compose is great for a single machine (your laptop, a demo, a small
CI job). It has no answer for: what happens when a node dies? How do I run 3
replicas of `order-service` behind a load balancer? How do I roll out a new
version with zero downtime? That's what Kubernetes (K8s) is for.

See the sample manifests in [`k8s/`](../../order-management-system/k8s):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: oms-training
spec:
  replicas: 2
  template:
    spec:
      containers:
        - name: order-service
          image: oms-training/order-service:1.0.0
          envFrom:
            - configMapRef:
                name: order-service-config
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8083 }
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8083 }
```

See [`k8s/order-service.yaml`](../../order-management-system/k8s/order-service.yaml).

* `replicas: 2` — Kubernetes keeps two pods running, restarting any that crash, and a `Service` load-balances across whichever are healthy.
* `readinessProbe`/`livenessProbe` point at Actuator's **separate** liveness/readiness health groups (auto-configured by Spring Boot) — readiness controls whether a pod *receives traffic*; liveness controls whether Kubernetes *restarts* the pod. This is exactly the health data from [Lesson 04](04-observability.md), now driving orchestration decisions automatically.
* `ConfigMap` externalizes environment-specific config (Eureka URL, Kafka bootstrap servers) from the image itself — the same image is deployable to any environment by changing only the `ConfigMap`.

Apply them (requires a cluster, e.g. `minikube` or `kind`, and images pushed
somewhere the cluster can pull from):

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/eureka-server.yaml
kubectl apply -f k8s/order-service.yaml
kubectl -n oms-training get pods -w
```

## Try it yourself

Write `k8s/customer-service.yaml` and `k8s/product-service.yaml` following the
`order-service.yaml` pattern (`ConfigMap` + `Deployment` + `Service`), then
add an `Ingress` resource that exposes only `api-gateway` outside the
cluster — reinforcing the Intermediate Lesson 03 principle that internal
services should never be directly reachable from outside.

## Key takeaways

* Pin base image versions; use JRE-only images for smaller, more secure runtime images.
* Docker Compose suits single-host/demo use; Kubernetes adds self-healing, rolling deploys, and multi-node scaling.
* Kubernetes readiness/liveness probes consume the exact Actuator health data introduced in Lesson 04.
* `ConfigMap`s keep images environment-agnostic — same image, different config, per environment.

Next: [Lesson 06 — Testing Strategies with Testcontainers](06-testing-strategies-testcontainers.md)
