# 📋 User Stories – Smart Clinic Application

This document captures key user stories based on the architecture of the Smart Clinic Application. Each story outlines the perspective of a stakeholder and their interaction with system components.

---

## 🧑‍⚕️ Admin Dashboard

- **User Story 1**  
  *As an admin, I want to access the Admin Dashboard, so that I can manage doctors, patients, and appointments efficiently.*

- **User Story 2**  
  *As an admin, I want to view and update doctor schedules, so that I can ensure proper appointment allocations.*

---

## 👨‍⚕️ Doctor Dashboard

- **User Story 3**  
  *As a doctor, I want to access my personal dashboard, so that I can see my daily appointments and patient records.*

- **User Story 4**  
  *As a doctor, I want to retrieve patient records via a REST interface, so that I can prepare for consultations quickly.*

---

## 🧑‍💻 Patient Modules (REST)

- **User Story 5**  
  *As a patient, I want to schedule appointments via a REST API, so that I can easily book a consultation.*

- **User Story 6**  
  *As a patient, I want to view my medical records using a secure API, so that I can monitor my health history.*

---

## 📦 Prescription Module (MongoDB)

- **User Story 7**  
  *As a doctor, I want to store and retrieve prescription records, so that patients can access them digitally.*

- **User Story 8**  
  *As a patient, I want to access my prescriptions, so that I can present them to a pharmacy when needed.*

---

## ⚙️ System Behavior

- **User Story 9**  
  *As a developer, I want all modules to call a centralized service layer, so that logic is reusable and easier to maintain.*

- **User Story 10**  
  *As a developer, I want to persist relational data using MySQL and JPA entities, so that I ensure data integrity.*

- **User Story 11**  
  *As a developer, I want to use MongoDB for prescription storage, so that unstructured data is handled efficiently.*

---