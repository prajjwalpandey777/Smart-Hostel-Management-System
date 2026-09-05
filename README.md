# Smart Hostel Management System

A standalone Java Swing desktop application for managing the day-to-day operations of a college hostel. It provides a single-window workspace for student records, rooms, fees, complaints, visitors, attendance, mess planning, outpasses, inventory, notices, staff, reports, and settings.

## Problem statement

Hostel administration often relies on spreadsheets, registers, and disconnected records. This makes room assignment, fee tracking, visitor security, complaint follow-up, and attendance monitoring slow and error-prone. Smart Hostel Management System centralizes these workflows in one local desktop application.

## Objectives

- Maintain accurate hostel records through an easy-to-use desktop interface.
- Reduce manual effort in student, room, fee, visitor, and attendance management.
- Demonstrate core Programming in Java concepts in a meaningful real-world application.
- Support validation, live CRUD operations, reports, file export, logging, and JDBC-ready database access.

## Target users

- Hostel warden and assistant warden
- Hostel office staff
- Security staff
- Mess and maintenance staff
- College administrators

## Major modules and features

### 1. Student and room administration

- Add, edit, search, delete, and view student records.
- Maintain student ID, course, room, phone number, and status.
- Add and manage room capacity, occupancy, available beds, and status.
- Automatically allocate a student to a room with an available bed.
- Load sample student records for demonstration and testing.

### 2. Fees, complaints, visitors, and attendance

- Collect fees and track paid or pending fee status.
- Generate a receipt preview for fee payments.
- Register, assign, update, and track complaint status.
- Record visitor entry and exit times; display a visitor-pass preview.
- Mark attendance with entry time, exit time, and late-entry status.

### 3. Hostel operations and communication

- Manage mess menu and meal plans.
- Create and approve leave/outpass requests.
- Track inventory, reorder levels, and low-stock status.
- Publish, search, and manage notices.
- Add and manage staff records.
- Generate CSV exports and operational reports.

### 4. Dashboard and administration

- Dashboard with hostel metrics, charts, recent activities, and visitor overview.
- Fixed Smart Hostel sidebar with scrollable module navigation.
- Dark professional Swing interface with accessible scrolling and validation.
- Settings for password, JDBC connection details, backup/restore controls, theme, and syllabus concept demo.

## Functional requirements

| ID | Requirement |
|---|---|
| FR-01 | The system shall allow users to add, edit, delete, search, and export module records. |
| FR-02 | The system shall manage students, rooms, fees, complaints, visitors, attendance, notices, staff, mess plans, outpasses, and inventory. |
| FR-03 | The system shall validate required form fields before saving a record. |
| FR-04 | The system shall generate receipt and visitor-pass previews. |
| FR-05 | The system shall export current table data to CSV files. |
| FR-06 | The system shall provide JDBC connection, query, and update abstractions for MySQL integration. |

## Non-functional requirements

| Area | Requirement |
|---|---|
| Usability | The application uses a single-window Swing interface with consistent navigation, visible scrollbars, and clear validation messages. |
| Reliability | Exceptions are handled for validation, file I/O, and JDBC operations; data actions require confirmation where appropriate. |
| Maintainability | The source is organized into focused nested domain, repository, database, file-service, UI, and utility classes. |
| Performance | Tables use `TableRowSorter` for responsive in-memory filtering; a lightweight background thread avoids blocking the UI. |
| Security | Password and connection controls are isolated in Settings; production deployment should use hashed passwords and protected database credentials. |
| Logging | A synchronized singleton audit log records selected application actions. |

## Technologies and Java concepts used

- **Java Swing**: `JFrame`, `JPanel`, `CardLayout`, `JTable`, `JScrollPane`, `JOptionPane`, and form controls.
- **Java Collections**: `ArrayList`, `Vector`, `Stack`, `Map`, `LinkedHashMap`, and generics.
- **OOP**: classes, objects, constructors, encapsulation, inheritance, abstraction, interfaces, method overriding, polymorphism, enums, nested classes, and singleton pattern.
- **Error handling**: custom checked exception, validation, `try/catch`, multi-catch, `throws`, `SQLException`, and `IOException`.
- **Multithreading**: a daemon `DashboardClock` thread and synchronized audit logging.
- **I/O streams**: character streams for CSV export/text reading and byte streams for backup copying.
- **JDBC**: `Connection`, `Statement`, `ResultSet`, query, update, and close methods ready for MySQL configuration.
- **Reflection and annotations**: Java reflection and a custom runtime annotation for syllabus-concept examples.

## Architecture

```mermaid
flowchart TD
    U[Hostel Administrator] --> UI[Java Swing UI]
    UI --> NAV[CardLayout Navigation]
