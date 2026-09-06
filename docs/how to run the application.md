# How to Run the Application

Step-by-step guide to build, package, and run the full `order-management-system`
microservices stack (Eureka, API Gateway, Customer/Product/Order/Payment/Notification
services, Kafka + Kafka UI) locally with Docker.

## Prerequisites

- **Docker Desktop** installed and running.
- **JDK 21** (the project's root `pom.xml` sets `java.version=21`; JDK 17 or older
  will fail to compile).
- **Maven 3.9+**.

If you don't have JDK 21 / Maven installed, install them first (examples for Windows):

```powershell
# Only needed once — install JDK 21 (Adoptium/Microsoft build)
# and Maven if not already available on your machine.
```

## 1. Set JAVA_HOME and PATH for this terminal session

Every new terminal needs JDK 21 and Maven on the `PATH` before building. Adjust the
paths below to match where JDK 21 and Maven are installed on your machine:

```powershell
$env:JAVA_HOME = "C:\Users\Admin\AppData\Local\jdks\jdk-21.0.10(1)"
$env:Path = "D:\Program Files\apache-maven-3.9.16\bin;$env:JAVA_HOME\bin;" + $env:Path

# Verify
java -version
mvn -version
```

You should see `openjdk version "21..."` and a Maven version in the output.

## 2. Build and package all services

From the `order-management-system` folder, run a full Maven build. This compiles
every module (`common-events`, `eureka-server`, `api-gateway`, `customer-service`,
`product-service`, `order-service`, `payment-service`, `notification-service`) and
produces an executable Spring Boot fat jar (`target/*.jar`) for each — required
before `docker compose --build` can bake them into images.

```powershell
Set-Location "d:\downloads\TRAINING\Project\order-management-system"
mvn clean package -DskipTests
```

Look for `BUILD SUCCESS` at the end. If any module is missing the line
`spring-boot:*:repackage` in its build output, that module's jar won't be
runnable (`no main manifest attribute` error later) — re-run `mvn clean package`
after checking the module's `pom.xml` has the `spring-boot-maven-plugin`
correctly configured.

## 3. Build images and start every container

Still inside `order-management-system/`:

```powershell
docker compose up -d --build
```

This builds a Docker image per service (using each service's `Dockerfile`, which
copies the jar built in step 2) and starts everything in the background:
`kafka`, `kafka-ui`, `eureka-server`, `api-gateway`, `customer-service`,
`product-service`, `order-service`, `payment-service`, `notification-service`.

## 4. Check that everything is up

```powershell
docker compose ps
```

All 9 containers should show `Up`. Spring Boot services take up to ~50 seconds
to fully start (Eureka registration + JPA/H2 init), so give it a minute before
testing. To watch a specific service's logs while it starts:

```powershell
docker compose logs -f customer-service
```

Press `Ctrl+C` to stop following logs (containers keep running).

## 5. Test the services

| Service | URL |
|---|---|
| Eureka dashboard (service registry) | http://localhost:8761 |
| API Gateway — aggregated Swagger UI (all services) | http://localhost:8080/swagger-ui.html |
| customer-service Swagger UI | http://localhost:8081/swagger-ui.html |
| product-service Swagger UI | http://localhost:8082/swagger-ui.html |
| order-service Swagger UI | http://localhost:8083/swagger-ui.html |
| payment-service Swagger UI | http://localhost:8084/swagger-ui.html |
| notification-service health | http://localhost:8085/actuator/health |
| Kafka UI | http://localhost:8090 |

Start with the gateway's Swagger UI (`:8080/swagger-ui.html`) — it lets you pick
each service from a dropdown and call every endpoint through the same routing
your app would use in production.

## 6. Stop everything

```powershell
Set-Location "d:\downloads\TRAINING\Project\order-management-system"
docker compose down
```

This stops and removes all containers and the network (data in the in-memory
H2 databases is lost — that's expected, they reset on every restart).

## Rebuilding after code changes

Whenever you change Java source code, repeat steps 2 and 3:

```powershell
mvn clean package -DskipTests
docker compose up -d --build
```

`docker compose up -d --build` only rebuilds images whose Dockerfile/context
changed and recreates the affected containers, leaving the rest untouched.

## Troubleshooting

- **`no main manifest attribute` when a container starts**: the jar wasn't
  repackaged into an executable Spring Boot jar. Re-run `mvn clean package`
  and confirm `spring-boot:*:repackage` runs for every module.
- **A service can't reach Eureka / Kafka**: make sure `eureka-server` and
  `kafka` show `Up` in `docker compose ps` before the dependent services
  finish starting — `docker compose up -d --build` starts them in the right
  order automatically via `depends_on`, but first-time image pulls can be slow.
- **Port already in use**: another process is using one of `8080-8085, 8090,
  8761, 9092`. Stop that process or edit the port mapping in `docker-compose.yml`.
