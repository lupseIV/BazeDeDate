-- 1. Pallet Trucks (Base)
CREATE TABLE "PalletTrucks" (
                                "truck_id" BIGINT PRIMARY KEY ,
                                "serial_number" VARCHAR(255) NOT NULL UNIQUE,
                                "type" VARCHAR(255) NOT NULL,
                                "model" VARCHAR(255) NOT NULL,
                                "capacity_kg" BIGINT NOT NULL,
                                "status" VARCHAR(255) CHECK ("status" IN ('Available', 'Rented', 'In Maintenance', 'Retired')) NOT NULL
);

-- 2. Pallet Truck Details (1:1)
CREATE TABLE "PalletTruckDetails" (
                                      "truck_id" BIGINT PRIMARY KEY,
                                      "wheels_id" BIGINT NOT NULL,
                                      "purchase_date" DATE NOT NULL,
                                      "notes" varchar(max),
                                      "manufacturer" VARCHAR(255) NOT NULL,
                                      FOREIGN KEY ("truck_id") REFERENCES "PalletTrucks"("truck_id") ON DELETE CASCADE
);

-- 3. Wheels
CREATE TABLE "Wheels" (
                          "wheels_id" BIGINT PRIMARY KEY,
                          "materials_id" BIGINT NOT NULL,
                          "max_weight" BIGINT NOT NULL,
                          "bid" BIGINT NOT NULL,
                          "truck_id" BIGINT NOT NULL,
                          FOREIGN KEY ("materials_id") REFERENCES "WheelMaterials"("materials_id"),
                          FOREIGN KEY ("bid") REFERENCES "Bearings"("bid"),
                          FOREIGN KEY ("truck_id") REFERENCES  "PalletTrucks"("truck_id")
);

-- 4. Bearings
CREATE TABLE "Bearings" (
                            "bid" BIGINT PRIMARY KEY,
                            "diameter" BIGINT NOT NULL,
                            "mid" BIGINT
);

-- 5. Wheel Materials
CREATE TABLE "WheelMaterials" (
                                  "materials_id" BIGINT PRIMARY KEY,
                                  "type" VARCHAR(255) NOT NULL,
                                  "max_weight" BIGINT NOT NULL
);

-- 6. Customers
CREATE TABLE "Customers" (
                             "customer_id" BIGINT PRIMARY KEY,
                             "company_name" VARCHAR(255) NOT NULL,
                             "contact_name" VARCHAR(255) NOT NULL,
                             "phone" VARCHAR(255) NOT NULL,
                             "email" VARCHAR(255) NOT NULL,
                             "address" varchar(max) NOT NULL
);


-- 7. Rentals (1:N  Customers, N:1  Trucks)
CREATE TABLE "Rentals" (
                           "rental_id" BIGINT PRIMARY KEY,
                           "customer_id" BIGINT NOT NULL,
                           "truck_id" BIGINT NOT NULL,
                           "start_date" DATE NOT NULL,
                           "end_date" DATE,
                           "daily_rate" DECIMAL(10, 2) NOT NULL,
                           "total_cost" DECIMAL(10, 2),
                           "return_status" VARCHAR(255) CHECK ("return_status" IN ('Active', 'Returned', 'Overdue')) NOT NULL,
                           FOREIGN KEY ("customer_id") REFERENCES "Customers"("customer_id"),
                           FOREIGN KEY ("truck_id") REFERENCES "PalletTrucks"("truck_id")
);

-- 8. Invoices (1:1 with Rentals)
CREATE TABLE "Invoices" (
                            "invoice_id" BIGINT PRIMARY KEY,
                            "rental_id" BIGINT UNIQUE NOT NULL,
                            "issue_date" DATE NOT NULL,
                            "status" VARCHAR(255) CHECK ("status" IN ('Pending', 'Paid', 'Overdue')) NOT NULL,
                            FOREIGN KEY ("rental_id") REFERENCES "Rentals"("rental_id") ON DELETE CASCADE
);

-- 9. Employees
CREATE TABLE "Employees" (
                             "employee_id" BIGINT PRIMARY KEY,
                             "first_name" VARCHAR(255) NOT NULL,
                             "last_name" VARCHAR(255) NOT NULL,
                             "role" VARCHAR(255) CHECK ("role" IN ('Technician', 'Driver', 'Sales', 'Manager', 'Admin')) NOT NULL,
                             "hire_date" DATE NOT NULL,
                             "phone" VARCHAR(255) NOT NULL,
                             "email" VARCHAR(255) NOT NULL
);

-- 10. Maintenance Records (1:N  Trucks, N:1  Employees)
CREATE TABLE "MaintenanceRecords" (
                                      "record_id" BIGINT PRIMARY KEY,
                                      "truck_id" BIGINT NOT NULL,
                                      "service_date" DATE NOT NULL,
                                      "description" VARCHAR(max),
                                      "cost" DECIMAL(10, 2),
                                      "employee_id" BIGINT,
                                      FOREIGN KEY ("truck_id") REFERENCES "PalletTrucks"("truck_id"),
                                      FOREIGN KEY ("employee_id") REFERENCES "Employees"("employee_id")
);

-- 11. Suppliers
CREATE TABLE "Suppliers" (
                             "supplier_id" BIGINT PRIMARY KEY,
                             "name" VARCHAR(255) NOT NULL,
                             "contact_name" VARCHAR(255) NOT NULL,
                             "phone" VARCHAR(255) NOT NULL,
                             "email" VARCHAR(255) NOT NULL,
                             "address" varchar(max) NOT NULL
);

-- 12. Parts Inventory
CREATE TABLE "PartsInventory" (
                                  "part_id" BIGINT PRIMARY KEY,
                                  "name" VARCHAR(255) NOT NULL,
                                  "supplier_id" BIGINT NOT NULL,
                                  "quantity" BIGINT NOT NULL,
                                  "unit_cost" DECIMAL(8, 2) NOT NULL,
                                  FOREIGN KEY ("supplier_id") REFERENCES "Suppliers"("supplier_id")
);

-- 13. Maintenance Parts (M:N  MaintenanceRecords PartsInventory)
CREATE TABLE "MaintenanceParts" (
                                    "record_id" BIGINT NOT NULL,
                                    "part_id" BIGINT NOT NULL,
                                    "quantity_used" BIGINT NOT NULL,
                                    PRIMARY KEY ("record_id", "part_id"),
                                    FOREIGN KEY ("record_id") REFERENCES "MaintenanceRecords"("record_id") ON DELETE CASCADE,
                                    FOREIGN KEY ("part_id") REFERENCES "PartsInventory"("part_id") ON DELETE CASCADE
);
