# 🏥 Spring Boot Healthcare Application Architecture

## 📘 Section 1: Architecture Summary

This **Spring Boot application** uses both **MVC (Thymeleaf)** and **REST controllers**.

- **Thymeleaf templates** are used for the `AdminDashboard` and `DoctorDashboard`.
- **REST APIs** are used for modules like `Appointments`, `PatientDashboard`, and `PatientRecord`.

The application connects to **two databases**:
- **MySQL** for structured data (Patient, Doctor, Appointment, Admin)
- **MongoDB** for unstructured data (Prescriptions)

All controllers route requests through a centralized **Service Layer**, which:
- Delegates to the appropriate repository (MySQL or MongoDB)
- Handles business logic
- Uses **JPA entities** for MySQL and **document models** for MongoDB.

---

## 🔁 Section 2: Numbered Flow of Data and Control

1. **User Interaction**  
   The user accesses either:
   - `AdminDashboard` or `DoctorDashboard` (via web UI)
   - `Appointments`, `PatientDashboard`, or `PatientRecord` (via REST API)

2. **Controller Routing**  
   Requests are handled by:
   - **Thymeleaf Controllers** for UI dashboards
   - **REST Controllers** for API-based modules using JSON

3. **Business Logic Delegation**  
   Both controller types delegate requests to the **Service Layer**.

4. **Repository Access**  
   The Service Layer interacts with:
   - **MySQL Repositories** for relational data
   - **MongoDB Repository** for document-based data

5. **Database Communication**  
   - MySQL Repositories access the **MySQL Database**
   - MongoDB Repository accesses the **MongoDB Database**

6. **Entity Mapping (MySQL)**  
   Relational data is mapped using **JPA Entities**:
   - `Patient`, `Doctor`, `Appointment`, `Admin`  
   These entities are defined in the **MySQL Models**.

7. **Document Mapping (MongoDB)**  
   Non-relational data like **Prescriptions** is:
   - Stored in the **MongoDB Database**
   - Mapped using **MongoDB Models** as document objects

---