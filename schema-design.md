## MySQL Database Design

### Table: patients
- id: INT, Primary Key, Auto Increment
- first_name: VARCHAR(100), Not Null
- last_name: VARCHAR(100), Not Null
- date_of_birth: DATE, Not Null
- gender: VARCHAR(10) -- e.g., 'Male', 'Female', 'Other'
- email: VARCHAR(255), Unique, Not Null
- phone_number: VARCHAR(20), Not Null
- address: VARCHAR(255)
- created_at: DATETIME, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  -- Justification: Stores core patient demographic information. Email and phone for communication, marked UNIQUE for email to prevent duplicate patient entries by email. Timestamps for audit.

### Table: doctors
- id: INT, Primary Key, Auto Increment
- first_name: VARCHAR(100), Not Null
- last_name: VARCHAR(100), Not Null
- specialty: VARCHAR(100), Not Null -- e.g., 'Cardiology', 'Pediatrics'
- email: VARCHAR(255), Unique, Not Null
- phone_number: VARCHAR(20)
- license_number: VARCHAR(50), Unique, Not Null
- created_at: DATETIME, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  -- Justification: Stores doctor's professional and contact details. License number is critical and unique.

### Table: appointments
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key -> doctors(id), Not Null
- patient_id: INT, Foreign Key -> patients(id), Not Null
- appointment_time: DATETIME, Not Null -- The exact start time of the appointment
- status: INT (0 = Scheduled, 1 = Completed, 2 = Cancelled, 3 = No-show), Not Null
- reason_for_visit: TEXT -- Optional, patient's stated reason
- created_at: DATETIME, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  -- Justification: Core transactional table linking patients and doctors to specific time slots.
  -- Foreign keys ensure referential integrity. Status helps track the lifecycle of an appointment.
  -- Data retention: On patient/doctor deletion, it's advisable to use 'soft delete' (an `is_active` flag)
  -- on patients/doctors table to preserve appointment history, rather than directly deleting records or setting FKs to NULL,
  -- due to the critical nature of medical history. If hard deletion is required, `ON DELETE NO ACTION` and
  -- application-level checks to prevent deletion of associated records would be safer.

### Table: admin
- id: INT, Primary Key, Auto Increment
- username: VARCHAR(50), Unique, Not Null
- password_hash: VARCHAR(255), Not Null -- Stores hashed passwords for security
- email: VARCHAR(255), Unique
- first_name: VARCHAR(100)
- last_name: VARCHAR(100)
- role: VARCHAR(50) -- e.g., 'SUPER_ADMIN', 'CLINIC_MANAGER', 'RECEPTIONIST'
- created_at: DATETIME, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  -- Justification: Manages administrative users and their access levels within the system. Password should always be hashed.

### Table: available_time_slots
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key -> doctors(id), Not Null
- start_time: DATETIME, Not Null
- end_time: DATETIME, Not Null
- is_booked: BOOLEAN, Default FALSE -- True if an appointment has been made for this slot
- clinic_location_id: INT, Foreign Key -> clinic_locations(id) -- Optional, if doctors work at multiple locations
- created_at: DATETIME, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  -- Justification: Allows doctors to define their specific working hours and available slots, preventing overbooking and improving scheduling accuracy.

### Table: clinic_locations
- id: INT, Primary Key, Auto Increment
- name: VARCHAR(255), Not Null -- e.g., 'Main Clinic', 'Downtown Branch'
- address: VARCHAR(255), Not Null
- phone_number: VARCHAR(20)
- email: VARCHAR(255)
- created_at: DATETIME, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  -- Justification: Stores information about different physical locations of the clinic, useful for larger practices.

### Table: payments
- id: INT, Primary Key, Auto Increment
- appointment_id: INT, Foreign Key -> appointments(id), Unique, Not Null
  -- Assuming one payment record per appointment for simplicity. If partial payments, remove UNIQUE.
- patient_id: INT, Foreign Key -> patients(id), Not Null -- Redundant but useful for direct lookups
- amount: DECIMAL(10, 2), Not Null
- payment_date: DATETIME, Not Null, Default CURRENT_TIMESTAMP
- payment_method: VARCHAR(50) -- e.g., 'Credit Card', 'Cash', 'Insurance', 'Bank Transfer'
- status: VARCHAR(20) -- e.g., 'Paid', 'Pending', 'Refunded', 'Failed'
- transaction_id: VARCHAR(255), Unique -- Optional, for external payment gateway reference
- created_at: DATETIME, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  -- Justification: Tracks financial transactions related to appointments.




## MongoDB Collection Design

This section outlines the design for MongoDB collections, chosen for their flexibility in handling less structured or rapidly evolving data that complements the structured data in MySQL.

## Collection: `prescriptions`

This collection stores detailed information about prescriptions issued to patients. It leverages MongoDB's flexibility to include complex structures, varying fields, and large text blocks like doctor's notes.

### Example Document (JSON)

```json
{
  "_id": ObjectId("666113b2c3d4e5f6a7b8c9d0"),
  "patient_id": 101, // Foreign Key to MySQL patients.id - Ensures link to the authoritative patient record
  "doctor_id": 201, // Foreign Key to MySQL doctors.id - Links to the prescribing doctor
  "appointment_id": 501, // Foreign Key to MySQL appointments.id (Optional: Prescription might be a follow-up, not tied to a single appointment)
  "issue_date": ISODate("2025-06-05T22:30:00Z"), // Date and time the prescription was issued
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