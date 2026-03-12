# TranspaletiiApp – Lab 1: Parent–Child Desktop Application

A JavaFX desktop application that manages **Pallet Trucks** (parent) and their **Wheels** (child) stored in a SQL Server database via plain JDBC — no ORM.

## Technologies

| Layer        | Technology                       |
|--------------|----------------------------------|
| Language     | Java 21                         |
| GUI          | JavaFX 21                       |
| Build        | Gradle (Kotlin DSL)             |
| Database     | SQL Server (JDBC via mssql-jdbc)|
| Architecture | Domain → Repository → Service → UI |

## Domain Model

```
Wheel (parent)                 PalletTruck (child)
├── wheels_id  (PK, UUID)      ├── truck_id  (PK, UUID)
├── materials_id (FK)          ├── serial_number
├── max_weight                 ├── type
└── bid (FK → Bearings)        ├── model
                               ├── capacity_kg
                               ├── status
                               └── wheels_id (FK → Wheels) ──────►
```

One **Wheel** can be used by many **PalletTrucks** (1-to-N via `wheels_id` FK in PalletTrucks).

**Relationships present in the database:**
- **1-to-N**: Wheel → PalletTrucks, Customers → Rentals, PalletTrucks → MaintenanceRecords
- **M-to-N**: MaintenanceRecords ↔ PartsInventory (via MaintenanceParts junction table)

The app focuses on the **Wheel (parent) → PalletTruck (child)** pair.

## Project Structure

```
src/main/java/org/example/transpaletiiapp/
├── TranspaletiiApp.java                    # Application entry, service wiring
├── Launcher.java                           # Main class (non-JavaFX entry point)
├── domain/
│   ├── PalletTruck.java                    # Parent entity
│   ├── Wheel.java                          # Child entity
│   ├── Bearing.java, WheelMaterial.java    # Related entities
│   ├── exceptions/                         # RepositoryException, ValidationException, ServiceException
│   └── utils/validation/                   # Validator interface + per-entity validators
├── repository/
│   ├── CrudRepository.java                 # Generic CRUD interface
│   ├── PalletTruckRepository.java          # Parent repository interface
│   ├── WheelsRepository.java               # Child repository interface
│   └── implementation/                     # JDBC implementations (*DbRepository)
│       └── utils/JdbcUtils.java            # Connection helper
├── service/
│   ├── IdentifiableService.java            # Generic service with validation
│   ├── PalletTrucksService.java            # Parent service
│   └── WheelsService.java                  # Child service
└── gui/controllers/
    └── MainController.java                 # Master–detail controller with CRUD

src/main/resources/
├── config/db.config                        # JDBC connection properties
├── migrations/v1/
│   ├── initial_db_table_creation.sql       # DDL for all tables
│   └── db_table_mock_entities.sql          # Seed data (15 trucks, 15 wheels, etc.)
└── org/example/transpaletiiapp/
    └── hello-view.fxml                     # Main UI layout
```

## Database Setup

1. Install **SQL Server** (or SQL Server Express) and ensure it is running on `localhost:1433`.
2. Create the database:
   ```sql
   CREATE DATABASE proiect_sem2;
   ```
3. Run the migration scripts **in order**:
   - `src/main/resources/migrations/v1/initial_db_table_creation.sql` — creates all tables
   - `src/main/resources/migrations/v1/db_table_mock_entities.sql` — inserts sample data (15 pallet trucks, 15 wheels, bearings, materials, customers, rentals, etc.)

4. Edit `src/main/resources/config/db.config` with your credentials:
   ```properties
   jdbc.url=jdbc:sqlserver://localhost:1433;databaseName=proiect_sem2;integratedSecurity=false;encrypt=true;trustServerCertificate=true;
   jdbc.username=YOUR_USERNAME
   jdbc.password=YOUR_PASSWORD
   ```

## Running the Application

From the project root in PowerShell:

```powershell
.\gradlew.bat run
```

The main window opens and automatically loads data from the database.

## GUI Usage

### Parent Table (Wheels)
- Shows all wheels with their ID, material type, max weight, and bearing diameter.
- **Click a row** to select it — the child table updates to show all pallet trucks that use that wheel.
- **Search bar**: type to filter wheels by material type or max weight.
- **Refresh button**: reloads data from the database.
- **Column headers**: click to sort ascending/descending.

### Child Table (Pallet Trucks)
- Shows the pallet truck(s) that reference the selected parent wheel.
- When no wheel is selected, all pallet trucks are shown.
- **Search bar**: filter by serial number, type, model, or status.
- **Refresh** and **column sorting** work the same as parent.

### CRUD Operations
- **Add**: fill in the form fields and click "Add Wheel" / "Add Truck". Adding a truck requires selecting a parent wheel first.
- **Edit**: select a row (form auto-fills), modify fields, click "Update Wheel" / "Update Truck".
- **Delete**: select a row, click "Delete Wheel" / "Delete Truck" → a confirmation dialog appears.
- **Clear Fields**: resets the form without modifying the database.

### Validation
- All required fields are validated before saving.
- Capacity and max weight must be positive numbers.
- Status must be one of: Available, Rented, In Maintenance, Retired.
- Friendly error dialogs appear for validation failures and database constraint violations.

## Error Handling

- **Database errors** (connection failures, constraint violations) are caught and displayed via JavaFX Alert dialogs with user-friendly messages.
- **Validation errors** are surfaced from both the GUI layer (form checks) and the service layer (Validator classes).
- **Foreign key violations** (e.g., deleting a wheel still referenced by a truck) display a helpful hint.

## Bonus Features Implemented

- ✅ Search / filter on parent and child tables
- ✅ Data validation with specific rules (positive capacity, valid status enum, required fields)
- ✅ Refresh button for reloading data from the database
- ✅ Column sorting on all table columns
