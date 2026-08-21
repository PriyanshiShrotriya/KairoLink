# 🚗 KairoLink

### A Smart & Reliable CarPooling System

**KairoLink** is a web-based carpooling platform that connects **drivers and passengers** to make everyday travel more affordable, convenient, and sustainable.

It follows a **layered Spring MVC architecture** with separate presentation, controller, service, repository, and database layers.

> **Share the ride. Split the cost. Travel smarter. 🌱**

---

## ✨ Key Features

* 👤 **User Authentication & Authorization**
* 🚗 **Offer & Manage Rides**
* 🔍 **Search & Match Rides**
* 🎟️ **Ride Booking & Cancellation**
* 💰 **Fare Calculation & Cost Sharing**
* ⭐ **Ratings & Reviews**
* 🛡️ **Role-Based Access Control**
* ⚠️ **Centralized Exception Handling**

---

## 🏗️ Architecture

```text
Presentation Layer
Thymeleaf + HTML/CSS/JS + Bootstrap
             │
             ▼
Controller Layer
Spring MVC Controllers
             │
             ▼
Service Layer
Business Logic & Transactions
             │
             ▼
Repository / DAO Layer
Spring Data JPA
             │
             ▼
Database Layer
PostgreSQL
```

### Cross-Cutting Concerns

* 🔐 Spring Security — Authentication & Role-Based Access
* ⚠️ `@ControllerAdvice` — Centralized Exception Handling
* 📝 SLF4J + Logback — Application Logging
* 🔄 DTOs & Mappers — Entity/API/View Separation
* 💳 `@Transactional` — Transaction Management

---

## 🛠️ Tech Stack

| Category        | Technologies                                                       |
| --------------- | ------------------------------------------------------------------ |
| Language        | Java 21 (project baseline per Spring Initializr selection) |
| Frontend        | Thymeleaf, HTML5, CSS3, JavaScript, Bootstrap 5                   |
| Backend         | Spring MVC + Spring Boot 4.1.x                                     |
| ORM             | Spring Data JPA / Hibernate                                        |
| Database        | PostgreSQL (chosen DB for this project)                             |
| Security        | Spring Security                                                     |
| Validation      | Jakarta Bean Validation                                            |
| Logging         | SLF4J, Logback                                                     |
| Build Tool      | Maven                                                              |
| Packaging       | JAR (Spring Boot default, embedded Tomcat)                         |
| API Testing     | Postman                                                            |
| Version Control | Git & GitHub                                                       |
| Deployment      | Docker + Render / Railway / Fly.io                                  |

---

## 📂 Project Structure

```text
KairoLink/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/kairolink/
│       │       ├── controller/
│       │       ├── service/
│       │       ├── repository/
│       │       ├── entity/
│       │       ├── dto/
│       │       ├── mapper/
│       │       ├── security/
│       │       └── exception/
│       │
│       └── resources/
│           ├── application.properties
│           ├── templates/          # Thymeleaf views (.html)
│           └── static/
│               ├── css/
│               ├── js/
│               └── images/
│
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

* Java 21 as the project baseline (per Spring Initializr selection)
* Maven
* PostgreSQL (this project uses PostgreSQL consistently)
* Git
* (Optional) Docker, for containerized deployment
* **VS Code**, with:
  * Extension Pack for Java
  * Spring Boot Extension Pack
  * Lombok Annotations Support for VS Code
  * GitHub Copilot + GitHub Copilot Chat

### Clone the Repository

```bash
git clone https://github.com/your-username/KairoLink.git
cd KairoLink
```

### Configure the Database

Create a database and update the database configuration in:

```text
src/main/resources/application.properties
```

### Build the Project

```bash
mvn clean install
```

### Run the Application

```bash
java -jar target/kairolink-0.0.1-SNAPSHOT.jar
```

Or, for local development in VS Code, use the **Spring Boot Dashboard** panel, or run `mvn spring-boot:run` from the integrated terminal — embedded Tomcat starts automatically, no separate server install needed. Then open `http://localhost:8080`.

### Deploying

KairoLink packages as a single self-contained JAR (embedded Tomcat), so deployment is just "run the JAR" — no external servlet container to configure:

* **Docker** — build with the included `Dockerfile` (`docker build -t kairolink .`) and run/push anywhere that runs containers.
* **Render / Railway / Fly.io** — all support "deploy a Spring Boot app" directly from a GitHub repo or via the Dockerfile, with free/low-cost tiers.
* Set `application-prod.properties` (or environment variables) for the production DB connection before deploying — see `phases.md` Phase 8.

---

## 🔮 Future Enhancements

* 🗺️ Real-time GPS & route tracking
* 📱 Dedicated mobile application
* 🔔 Real-time ride notifications
* 💳 Online payment integration
* 🤖 AI-based ride matching
* 🪪 Enhanced driver verification

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Commit your changes
5. Push the branch
6. Open a Pull Request

---

## 👥 Team

**KairoLink Team**

Built with ❤️ to make everyday commuting **smarter, cheaper, and more sustainable.**

---

⭐ **If you find KairoLink useful, consider giving the repository a star!**
