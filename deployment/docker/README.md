# Food Delivery API - Docker Deployment

This directory contains the production-ready Docker configuration for the Spring Boot application, following a structured layout.

## Structure

- `Dockerfile`: A multi-stage build that compiles the app with Maven and packages it in an Alpine JRE container as a non-root user.
- `docker-compose.yml`: Development environment setup. It mounts your local source code, maps port 8080 directly, and uses DevTools for hot reloading.
- `docker-compose.prod.yml`: Production environment setup. It builds the Dockerfile, places Nginx in front of the application, uses health checks, and relies on a `.env` file for secrets.
- `nginx/`: Configuration files for the Nginx reverse proxy.
  - `start-with-nginx.sh`: Helper script to launch the production stack.
- `init.sql`: Contains `INSERT` statements for initial seed data. It is executed by a separate one-time `seeder` service (defined in `docker-compose.prod.yml`) that runs only after both the database and application are healthy.

## How to Run - Development Environment

The development environment mounts your local source code into the container. Any changes to your code will be hot-reloaded by Spring Boot DevTools (if configured in your `pom.xml`).

1. Make sure you are in the `deployment/docker/` directory.
2. Run the stack:
   ```bash
   docker-compose up -d
   ```
3. The app is accessible at `http://localhost:8080`.

## How to Run - Production Environment

The production environment isolates the application behind an Nginx reverse proxy. The application itself is not exposed directly to the host machine.

1. Ensure you have a `.env` file in the **root** of the project (two directories up from `nginx/`).
2. Navigate to `deployment/docker/nginx/` and run the helper script:
   ```bash
   ./start-with-nginx.sh
   ```
   Or run the compose file manually from the `deployment/docker/` directory:
   ```bash
   docker-compose -f docker-compose.prod.yml --env-file ../../.env up -d --build
   ```
   *(Note: The `--build` flag handles both building the Docker image from the Dockerfile and starting all containers together in one command.)*

   **Alternative for CI/CD Pipelines (Separate Build & Run):**
   If you need to build the image separately before running (e.g., in a deployment pipeline), you can run:
   ```bash
   docker-compose -f docker-compose.prod.yml --env-file ../../.env build
   docker-compose -f docker-compose.prod.yml --env-file ../../.env up -d
   ```

3. The app is accessible via Nginx at `http://localhost`.

## Database Seeding

The database is seeded using a one-time `seeder` service configured in `docker-compose.prod.yml`. 
This service runs automatically during `docker-compose -f docker-compose.prod.yml up`. Crucially, it waits until *both* the database and the Spring Boot application are fully healthy. This ensures that Hibernate has already created the database schema before any seed data (from `init.sql`) is inserted.

The `init.sql` script exclusively contains `INSERT` statements using safe `ON CONFLICT DO NOTHING` clauses, meaning the seeder service will safely skip existing data if it runs again on subsequent startups.

## SSL Configuration (Nginx)

To enable HTTPS:
1. Place your SSL certificates (`fullchain.pem` and `privkey.pem`) in the `deployment/docker/nginx/ssl/` directory. For local testing, you can generate self-signed certificates using OpenSSL.
2. Open `deployment/docker/nginx/conf.d/app.conf`.
3. Uncomment the HTTPS server block at the bottom of the file.
4. Restart the Nginx container: `docker-compose -f docker-compose.prod.yml restart nginx`
