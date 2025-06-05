## MySQL Database Design

This document outlines the relational database schema for the clinic management system, using MySQL. It defines the tables, their columns, data types, primary keys, foreign key relationships, and essential constraints, reflecting the core operational data.

## Table: `patients`

* **Purpose:** Stores core demographic and contact information for each patient. This is the central repository for patient identification and essential details.
* **Columns:**
    * `id`: `INT`, **Primary Key**, `Auto Increment`
        * *Constraint:* Automatically assigned unique identifier for each patient.
    * `first_name`: `VARCHAR(100)`, `Not Null`
        * *Constraint:* Stores the patient's first name; must be provided.
    * `last_name`: `VARCHAR(100)`, `Not Null`
        * *Constraint:* Stores the patient's last name; must be provided.
    * `date_of_birth`: `DATE`, `Not Null`
        * *Constraint:* Stores the patient's birth date; essential for age-related medical considerations and must be provided.
    * `gender`: `VARCHAR(10)`
        * *Comment:* Stores biological sex or gender identity (e.g., 'Male', 'Female', 'Other'). Optional.
    * `email`: `VARCHAR(255)`, `Unique`, `Not Null`
        * *Constraint:* Stores the patient's email; must be unique across all patients and provided. Used for communication and login.
    * `phone_number`: `VARCHAR(20)`, `Not Null`
        * *Constraint:* Stores the primary phone number; must be provided for contact.
    * `address`: `VARCHAR(255)`
        * *Comment:* Stores the patient's physical address. Optional.
    * `created_at`: `DATETIME`, `Default CURRENT_TIMESTAMP`
        * *Comment:* Automatically records the timestamp of record creation.
    * `updated_at`: `DATETIME`, `Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
        * *Comment:* Automatically updates timestamp on any record modification.

## Table: `doctors`

* **Purpose:** Stores professional and contact information for each doctor associated with the clinic. This table includes their specialty and relevant identification.
* **Columns:**
    * `id`: `INT`, **Primary Key**, `Auto Increment`
        * *Constraint:* Unique identifier for each doctor.
    * `first_name`: `VARCHAR(100)`, `Not Null`
        * *Constraint:* Doctor's first name; must be provided.
    * `last_name`: `VARCHAR(100)`, `Not Null`
        * *Constraint:* Doctor's last name; must be provided.
    * `specialty`: `VARCHAR(100)`, `Not Null`
        * *Constraint:* The medical field of expertise (e.g., 'Cardiology', 'Pediatrics'); must be provided.
    * `email`: `VARCHAR(255)`, `Unique`, `Not Null`
        * *Constraint:* Doctor's professional email; must be unique and provided.
    * `phone_number`: `VARCHAR(20)`
        * *Comment:* Doctor's contact phone number. Optional.
    * `license_number`: `VARCHAR(50)`, `Unique`, `Not Null`
        * *Constraint:* Unique medical license number; critical for professional identification and must be provided.
    * `created_at`: `DATETIME`, `Default CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of record creation.
    * `updated_at`: `DATETIME`, `Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of last record modification.

## Table: `appointments`

* **Purpose:** Records every scheduled and completed interaction between a patient and a doctor. It links patients, doctors, and specific times, serving as a core transactional record.
* **Columns:**
    * `id`: `INT`, **Primary Key**, `Auto Increment`
        * *Constraint:* Unique identifier for each appointment.
    * `doctor_id`: `INT`, **Foreign Key** `-> doctors(id)`, `Not Null`
        * *Constraint:* Links to the doctor involved in the appointment; must be provided.
    * `patient_id`: `INT`, **Foreign Key** `-> patients(id)`, `Not Null`
        * *Constraint:* Links to the patient attending the appointment; must be provided.
    * `appointment_time`: `DATETIME`, `Not Null`
        * *Constraint:* The exact start date and time of the appointment; must be provided.
    * `end_time`: `DATETIME`, `Not Null`
        * *Constraint:* The calculated end date and time of the appointment; must be provided. This can be derived from `appointment_time` and a standard duration or a doctor-specific duration.
    * `status`: `INT`, `Not Null`
        * *Comment:* Current state of the appointment (e.g., `0 = Scheduled`, `1 = Completed`, `2 = Cancelled`, `3 = No-show`).
    * `reason_for_visit`: `TEXT`
        * *Comment:* Patient's stated reason or chief complaint for the visit. Optional, can be long.
    * `created_at`: `DATETIME`, `Default CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of appointment booking.
    * `updated_at`: `DATETIME`, `Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of last appointment modification.
* **Relationships & Considerations:**
    * **Patient/Doctor Deletion:** To preserve historical medical records (appointments), it's strongly recommended to use a "soft delete" mechanism for `patients` and `doctors` (e.g., an `is_active` boolean flag or `deleted_at` timestamp). Hard deletion or `ON DELETE CASCADE` is generally not advisable for this type of data due to legal and medical record retention requirements.

## Table: `admin`

* **Purpose:** Manages users with administrative privileges within the clinic system. This includes roles like clinic managers or receptionists who manage operations.
* **Columns:**
    * `id`: `INT`, **Primary Key**, `Auto Increment`
        * *Constraint:* Unique identifier for each admin user.
    * `username`: `VARCHAR(50)`, `Unique`, `Not Null`
        * *Constraint:* The login username for the admin; must be unique and provided.
    * `password_hash`: `VARCHAR(255)`, `Not Null`
        * *Constraint:* Stores the securely hashed version of the admin's password; must be provided. **Never store plain text passwords.**
    * `email`: `VARCHAR(255)`, `Unique`
        * *Constraint:* Admin's email address; unique for communication and recovery. Optional.
    * `first_name`: `VARCHAR(100)`
        * *Comment:* Admin's first name.
    * `last_name`: `VARCHAR(100)`
        * *Comment:* Admin's last name.
    * `role`: `VARCHAR(50)`
        * *Comment:* Defines the specific administrative role (e.g., 'SUPER_ADMIN', 'CLINIC_MANAGER', 'RECEPTIONIST'). Used for Role-Based Access Control (RBAC).
    * `created_at`: `DATETIME`, `Default CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of admin account creation.
    * `updated_at`: `DATETIME`, `Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of last admin account modification.

## Table: `available_time_slots`

* **Purpose:** Defines the specific time blocks when doctors are available for appointments. This table is crucial for managing doctor schedules, preventing overbooking, and facilitating appointment booking.
* **Columns:**
    * `id`: `INT`, **Primary Key**, `Auto Increment`
        * *Constraint:* Unique identifier for each defined time slot.
    * `doctor_id`: `INT`, **Foreign Key** `-> doctors(id)`, `Not Null`
        * *Constraint:* Links the time slot to a specific doctor; must be provided.
    * `start_time`: `DATETIME`, `Not Null`
        * *Constraint:* The starting date and time of the available slot; must be provided.
    * `end_time`: `DATETIME`, `Not Null`
        * *Constraint:* The ending date and time of the available slot; must be provided.
    * `is_booked`: `BOOLEAN`, `Default FALSE`
        * *Comment:* A flag indicating whether this specific slot has been taken by an appointment. Defaults to `FALSE` (available).
    * `clinic_location_id`: `INT`, **Foreign Key** `-> clinic_locations(id)`
        * *Comment:* Optional link to a specific clinic location if doctors work at multiple branches.
    * `created_at`: `DATETIME`, `Default CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of slot creation.
    * `updated_at`: `DATETIME`, `Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of last slot modification (e.g., when `is_booked` status changes).
* **Relationships & Considerations:**
    * This table complements the `appointments` table. An `appointment` consumes an `available_time_slot`. Application logic should ensure that an `available_time_slot` marked as `is_booked = TRUE` cannot be double-booked.

## Table: `clinic_locations`

* **Purpose:** Stores information about the different physical branches or locations where the clinic operates. This is useful for multi-branch practices.
* **Columns:**
    * `id`: `INT`, **Primary Key**, `Auto Increment`
        * *Constraint:* Unique identifier for each clinic location.
    * `name`: `VARCHAR(255)`, `Not Null`
        * *Constraint:* The name of the clinic location (e.g., 'Main Clinic', 'Downtown Branch'); must be provided.
    * `address`: `VARCHAR(255)`, `Not Null`
        * *Constraint:* The full physical address of the clinic; must be provided.
    * `phone_number`: `VARCHAR(20)`
        * *Comment:* Contact phone number for the specific location. Optional.
    * `email`: `VARCHAR(255)`
        * *Comment:* Contact email for the specific location. Optional.
    * `created_at`: `DATETIME`, `Default CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of record creation.
    * `updated_at`: `DATETIME`, `Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of last record modification.

## Table: `payments`

* **Purpose:** Tracks financial transactions related to appointments, recording payments made by patients to the clinic.
* **Columns:**
    * `id`: `INT`, **Primary Key**, `Auto Increment`
        * *Constraint:* Unique identifier for each payment record.
    * `appointment_id`: `INT`, **Foreign Key** `-> appointments(id)`, `Unique`, `Not Null`
        * *Constraint:* Links the payment to a specific appointment; must be provided. The `UNIQUE` constraint here implies a one-to-one relationship (one payment record per appointment), or a primary payment record. If multiple partial payments per appointment are allowed, this constraint would be removed.
    * `patient_id`: `INT`, **Foreign Key** `-> patients(id)`, `Not Null`
        * *Constraint:* Links the payment to the patient who made it; must be provided.
    * `amount`: `DECIMAL(10, 2)`, `Not Null`
        * *Constraint:* The monetary value of the payment; must be provided. `DECIMAL` ensures exact precision for financial data.
    * `payment_date`: `DATETIME`, `Not Null`, `Default CURRENT_TIMESTAMP`
        * *Constraint:* The date and time the payment was recorded; defaults to current timestamp.
    * `payment_method`: `VARCHAR(50)`
        * *Comment:* Describes how the payment was made (e.g., 'Credit Card', 'Cash', 'Insurance', 'Bank Transfer').
    * `status`: `VARCHAR(20)`
        * *Comment:* The current state of the payment (e.g., 'Paid', 'Pending', 'Refunded', 'Failed').
    * `transaction_id`: `VARCHAR(255)`, `Unique`
        * *Constraint:* Optional unique identifier from an external payment gateway.
    * `created_at`: `DATETIME`, `Default CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of payment record creation.
    * `updated_at`: `DATETIME`, `Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
        * *Comment:* Timestamp of last payment record modification.



## MongoDB Collection Design

This section outlines the design for MongoDB collections, chosen for their flexibility in handling less structured or rapidly evolving data that complements the structured data in MySQL.

## Collection: `prescriptions`

This collection stores detailed information about prescriptions issued to patients. It leverages MongoDB's flexibility to include complex structures, varying fields, and large text blocks like doctor's notes. This model aligns with the "Prescription model" described previously, allowing for patient name, medication, and appointment ID to be relevant, while also accommodating more dynamic data.

### Example Document (JSON)

```json
{
  "_id": ObjectId("666113b2c3d4e5f6a7b8c9d0"),
  "patient_id": 101, // Foreign Key to MySQL patients.id - Ensures link to the authoritative patient record
  "doctor_id": 201, // Foreign Key to MySQL doctors.id - Links to the prescribing doctor
  "appointment_id": 501, // Foreign Key to MySQL appointments.id (Optional: Prescription might be a follow-up, not tied to a single appointment)
  "issue_date": ISODate("2025-06-05T22:30:00Z"), // Date and time the prescription was issued
  "patientName": "John Smith", // Denormalized for convenience, but patient_id is the source of truth
  "medications": [ // An array to hold details for multiple medications within one prescription
    {
      "name": "Amoxicillin",
      "dosage": "500mg",
      "frequency": "Three times a day",
      "duration": "10 days",
      "instructions": "Take with food.",
      "refills_allowed": 1,
      "dispensed_quantity": 30
    },
    {
      "name": "Ibuprofen",
      "dosage": "200mg",
      "frequency": "As needed",
      "duration": null, // Can be null if indefinite or "as needed"
      "instructions": "Do not exceed 3 doses in 24 hours.",
      "refills_allowed": 0,
      "dispensed_quantity": null
    }
  ],
  "doctor_notes": "Patient presented with bacterial infection, prescribed broad-spectrum antibiotic. Advised rest and hydration.", // Free-form text for doctor's observations
  "status": "Active", // Current status: "Active", "Filled", "Expired", "Cancelled"
  "metadata": { // Embedded document for additional, flexible data points
    "prescription_type": "New", // "New", "Refill", "Renewal"
    "created_by_admin_id": null, // Optional: if an admin initiated the prescription, FK to MySQL admin.id
    "last_modified": ISODate("2025-06-05T22:35:00Z") // Timestamp for last modification
  },
  "pharmacy_details": { // Embedded document for specific pharmacy details where it was sent or filled
    "name": "MedCare Pharmacy",
    "address": "123 Main St, Anytown",
    "phone": "+1234567890"
  },
  "patient_feedback": { // Optional: Embedded document for post-prescription patient feedback
    "symptoms_improved": true,
    "side_effects": "Mild drowsiness",
    "notes": "Felt better after 3 days. Drowsiness was manageable."
  },
  "tags": ["antibiotic", "infection", "acute"], // Array of keywords for easy searching/categorization
  "attachments": [ // Array for links or references to associated files (e.g., lab results)
    {
      "file_name": "lab_results_20250605.pdf",
      "file_url": "[https://example.com/attachments/pres_123_lab.pdf](https://example.com/attachments/pres_123_lab.pdf)",
      "type": "PDF"
    }
  ]
}