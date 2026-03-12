# Lab 1 Report – TranspaletiiApp

## 1. Design Decisions

### Domain Model
The application models a **pallet truck rental management** system. The core parent–child relationship chosen for this lab is:

- **Parent**: `Wheel` — represents a wheel assembly with a material type (Rubber, Polyurethane, Steel, etc.), a bearing (with diameter), and a maximum weight rating.
- **Child**: `PalletTruck` — represents a physical pallet truck with a serial number, type (Manual/Electric), model, capacity in kg, and operational status. Each truck references one wheel via the `wheels_id` FK.

The relationship is implemented via a foreign key `wheels_id` in the `PalletTrucks` table referencing `Wheels.wheels_id`. One wheel can be used by many pallet trucks (1-to-N). Clicking a wheel in the parent table shows all trucks that use it.

### Architecture
The project follows a **layered architecture** with clear separation of concerns:

1. **Domain layer** (`domain/`) — POJOs implementing `Identifiable<UUID>`, plus exception classes and validators.
2. **Repository layer** (`repository/`) — generic `CrudRepository<ID, E>` interface with JDBC implementations (`*DbRepository`) that use `PreparedStatement` for all queries.
3. **Service layer** (`service/`) — generic `IdentifiableService<ID, E>` that delegates to repositories and enforces validation via `Validator<T>` before every save.
4. **GUI layer** (`gui/controllers/`) — a single `MainController` wired to `hello-view.fxml`, implementing the master–detail pattern.

### Database
SQL Server was chosen because it was already used in the first semester. The schema contains 13 tables, including both 1-to-N relationships (e.g., Customers → Rentals) and an M-to-N relationship (MaintenanceRecords ↔ PartsInventory via the MaintenanceParts junction table).

### Connection Management
Each repository method opens its own JDBC connection via `JdbcUtils.getConnection()` inside a `try-with-resources` block, ensuring connections are always closed — even on exceptions. This avoids connection leaks and satisfies the lab requirement for proper resource management.

## 2. Challenges and Solutions

| Challenge | Solution |
|-----------|----------|
| **FXML not found at runtime** | Used class-relative resource loading (`TranspaletiiApp.class.getResource("hello-view.fxml")`) and opened the domain package in `module-info.java` for JavaFX reflection. |
| **PropertyValueFactory not working with modules** | Added `opens org.example.transpaletiiapp.domain to javafx.base;` in `module-info.java` so JavaFX can reflectively access getters. Used lambda-based `CellValueFactory` as an alternative. |
| **Injecting services into the FXML controller** | After calling `fxmlLoader.load()`, retrieved the controller via `fxmlLoader.getController()` and called a `setServices(...)` method to pass all service instances. |
| **Foreign key constraint errors on delete** | Wrapped delete operations in try-catch and displayed user-friendly Alert dialogs explaining the FK constraint, with a hint about which entity still references the record. |
| **Search/filter breaking column sorting** | Wrapped `ObservableList` in `FilteredList` → `SortedList`, and bound `SortedList.comparatorProperty()` to `TableView.comparatorProperty()` so both features work together. |

## 3. What I Learned

- **JDBC fundamentals**: manually writing SQL, using `PreparedStatement` to prevent SQL injection, and properly closing `Connection`, `PreparedStatement`, and `ResultSet` via `try-with-resources`.
- **JavaFX master–detail pattern**: listening to `TableView` selection changes to update a child view, and using `ObservableList` / `FilteredList` / `SortedList` for reactive data binding.
- **Layered architecture in practice**: how separating domain, repository, service, and UI layers makes it easy to change one layer without affecting others (e.g., switching from SQL Server to PostgreSQL only requires changing the JDBC driver and `db.config`).
- **Java module system**: configuring `module-info.java` with `opens` and `exports` so JavaFX FXML and property reflection work correctly.
- **User experience considerations**: confirmation dialogs before destructive operations, clear error messages, and input validation at both the GUI and service levels.

