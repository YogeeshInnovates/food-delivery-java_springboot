# Docker Setup and Run Guide

This project is configured to run entirely inside Docker containers. The Docker environment includes:
1. **Spring Boot Application** (Java 17)
2. **PostgreSQL Database** (Version 15)
3. **Redis** (Version 7)

## Prerequisites
- **Docker Desktop** installed and running on your machine.
- Your IDE (like IntelliJ, VS Code, or Eclipse) set up for Java development.

---

## 1. How to Start the Application

To start the application and all databases, open your terminal (Command Prompt, PowerShell, or your IDE's built-in terminal) in this project folder and run:

```bash
docker-compose up -d
```

- The `-d` flag runs the containers in the "detached" background mode, so you can continue using your terminal.
- **You do NOT need to run `--build`** or use the `Dockerfile` anymore. The `docker-compose.yml` is configured for **Development Mode**, which maps your local files directly into the container.

---

## 2. How Hot-Reloading Works (Live Updates)

We have configured **Spring Boot DevTools** and mapped your local project folder to the Docker container. This means you can write code and test it without ever restarting Docker!

1. Make sure your Docker containers are running.
2. Open any `.java` file and make your changes.
3. Save the file.
4. **Compile the file:** 
   - **IntelliJ**: Press `Ctrl+F9` (Build Project).
   - **VS Code**: Just save the file (the Java Extension auto-compiles it to the `target` folder).
5. Spring Boot DevTools inside the Docker container will instantly detect the changed `.class` files and restart the server in under 2 seconds.

---

## 3. How to View Logs

If you want to see the application's output (like error messages or `System.out.println`), run this command:

```bash
docker-compose logs -f app
```
*(Press `Ctrl+C` to stop watching the logs).*

---

## 4. How to Stop the Application

When you are done working for the day and want to turn off the servers, run:

```bash
docker-compose down
```
This safely stops and removes the containers. Your database data is saved permanently in a Docker Volume, so you won't lose your PostgreSQL tables or data.
