-- CREATE DATABASE proiect_sem2;
-- GO
-- USE proiect_sem2;
-- GO
USE proiect_sem2;
GO

-- 1. Bearings
CREATE TABLE "Bearings" (
                            "bid" UNIQUEIDENTIFIER CONSTRAINT "DF_Bearings_bid" DEFAULT NEWSEQUENTIALID(),
                            "diameter" BIGINT NOT NULL,
                            "mid" BIGINT,
                            CONSTRAINT "PK_Bearings" PRIMARY KEY ("bid")
);

-- 2. Wheel Materials
CREATE TABLE "WheelMaterials" (
                                  "materials_id" UNIQUEIDENTIFIER CONSTRAINT "DF_WheelMaterials_id" DEFAULT NEWSEQUENTIALID(),
                                  "type" VARCHAR(255) NOT NULL,
                                  "max_weight" BIGINT NOT NULL,
                                  CONSTRAINT "PK_WheelMaterials" PRIMARY KEY ("materials_id")
);

-- 3. Wheels (Created BEFORE Trucks)
CREATE TABLE "Wheels" (
                          "wheels_id" UNIQUEIDENTIFIER CONSTRAINT "DF_Wheels_id" DEFAULT NEWSEQUENTIALID(),
                          "materials_id" UNIQUEIDENTIFIER NOT NULL,
                          "max_weight" BIGINT NOT NULL,
                          "bid" UNIQUEIDENTIFIER NOT NULL,
                          CONSTRAINT "PK_Wheels" PRIMARY KEY ("wheels_id"),
                          CONSTRAINT "FK_Wheels_WheelMaterials" FOREIGN KEY ("materials_id") REFERENCES "WheelMaterials"("materials_id"),
                          CONSTRAINT "FK_Wheels_Bearings" FOREIGN KEY ("bid") REFERENCES "Bearings"("bid")
);

-- 4. Pallet Trucks (Contains the FK to Wheels)
CREATE TABLE "PalletTrucks" (
                                "truck_id" UNIQUEIDENTIFIER CONSTRAINT "DF_PalletTrucks_id" DEFAULT NEWSEQUENTIALID(),
                                "serial_number" VARCHAR(255) NOT NULL,
                                "type" VARCHAR(255) NOT NULL,
                                "model" VARCHAR(255) NOT NULL,
                                "capacity_kg" BIGINT NOT NULL,
                                "status" VARCHAR(255) NOT NULL,
                                "wheels_id" UNIQUEIDENTIFIER NOT NULL,
                                CONSTRAINT "PK_PalletTrucks" PRIMARY KEY ("truck_id"),
                                CONSTRAINT "UQ_PalletTrucks_SN" UNIQUE ("serial_number"),
                                CONSTRAINT "CHK_PalletTrucks_Status" CHECK ("status" IN ('Available', 'Rented', 'In Maintenance', 'Retired')),
                                CONSTRAINT "FK_PalletTrucks_Wheels" FOREIGN KEY ("wheels_id") REFERENCES "Wheels"("wheels_id")
);

-- 5. Pallet Truck Details
CREATE TABLE "PalletTruckDetails" (
                                      "truck_id" UNIQUEIDENTIFIER NOT NULL,
                                      "purchase_date" DATE NOT NULL,
                                      "notes" VARCHAR(MAX),
                                      "manufacturer" VARCHAR(255) NOT NULL,
                                      CONSTRAINT "PK_PalletTruckDetails" PRIMARY KEY ("truck_id"),
                                      CONSTRAINT "FK_PalletTruckDetails_Trucks" FOREIGN KEY ("truck_id") REFERENCES "PalletTrucks"("truck_id") ON DELETE CASCADE
);

-- 6. Customers
CREATE TABLE "Customers" (
                             "customer_id" UNIQUEIDENTIFIER CONSTRAINT "DF_Customers_id" DEFAULT NEWSEQUENTIALID(),
                             "company_name" VARCHAR(255) NOT NULL,
                             "contact_name" VARCHAR(255) NOT NULL,
                             "phone" VARCHAR(255) NOT NULL,
                             "email" VARCHAR(255) NOT NULL,
                             "address" VARCHAR(MAX) NOT NULL,
                             CONSTRAINT "PK_Customers" PRIMARY KEY ("customer_id")
);

-- 7. Rentals
CREATE TABLE "Rentals" (
                           "rental_id" UNIQUEIDENTIFIER CONSTRAINT "DF_Rentals_id" DEFAULT NEWSEQUENTIALID(),
                           "customer_id" UNIQUEIDENTIFIER NOT NULL,
                           "truck_id" UNIQUEIDENTIFIER NOT NULL,
                           "start_date" DATE NOT NULL,
                           "end_date" DATE,
                           "daily_rate" DECIMAL(10, 2) NOT NULL,
                           "total_cost" DECIMAL(10, 2),
                           "return_status" VARCHAR(255) NOT NULL,
                           CONSTRAINT "PK_Rentals" PRIMARY KEY ("rental_id"),
                           CONSTRAINT "CHK_Rentals_ReturnStatus" CHECK ("return_status" IN ('Active', 'Returned', 'Overdue')),
                           CONSTRAINT "FK_Rentals_Customers" FOREIGN KEY ("customer_id") REFERENCES "Customers"("customer_id"),
                           CONSTRAINT "FK_Rentals_Trucks" FOREIGN KEY ("truck_id") REFERENCES "PalletTrucks"("truck_id")
);

-- 8. Invoices
CREATE TABLE "Invoices" (
                            "invoice_id" UNIQUEIDENTIFIER CONSTRAINT "DF_Invoices_id" DEFAULT NEWSEQUENTIALID(),
                            "rental_id" UNIQUEIDENTIFIER NOT NULL,
                            "issue_date" DATE NOT NULL,
                            "status" VARCHAR(255) NOT NULL,
                            CONSTRAINT "PK_Invoices" PRIMARY KEY ("invoice_id"),
                            CONSTRAINT "UQ_Invoices_Rental" UNIQUE ("rental_id"),
                            CONSTRAINT "CHK_Invoices_Status" CHECK ("status" IN ('Pending', 'Paid', 'Overdue')),
                            CONSTRAINT "FK_Invoices_Rentals" FOREIGN KEY ("rental_id") REFERENCES "Rentals"("rental_id") ON DELETE CASCADE
);

-- 9. Employees
CREATE TABLE "Employees" (
                             "employee_id" UNIQUEIDENTIFIER CONSTRAINT "DF_Employees_id" DEFAULT NEWSEQUENTIALID(),
                             "first_name" VARCHAR(255) NOT NULL,
                             "last_name" VARCHAR(255) NOT NULL,
                             "role" VARCHAR(255) NOT NULL,
                             "hire_date" DATE NOT NULL,
                             "phone" VARCHAR(255) NOT NULL,
                             "email" VARCHAR(255) NOT NULL,
                             CONSTRAINT "PK_Employees" PRIMARY KEY ("employee_id"),
                             CONSTRAINT "CHK_Employees_Role" CHECK ("role" IN ('Technician', 'Driver', 'Sales', 'Manager', 'Admin'))
);

-- 10. Maintenance Records
CREATE TABLE "MaintenanceRecords" (
                                      "record_id" UNIQUEIDENTIFIER CONSTRAINT "DF_MaintenanceRecords_id" DEFAULT NEWSEQUENTIALID(),
                                      "truck_id" UNIQUEIDENTIFIER NOT NULL,
                                      "service_date" DATE NOT NULL,
                                      "description" VARCHAR(MAX),
                                      "cost" DECIMAL(10, 2),
                                      "employee_id" UNIQUEIDENTIFIER,
                                      CONSTRAINT "PK_MaintenanceRecords" PRIMARY KEY ("record_id"),
                                      CONSTRAINT "FK_MaintenanceRecords_Trucks" FOREIGN KEY ("truck_id") REFERENCES "PalletTrucks"("truck_id"),
                                      CONSTRAINT "FK_MaintenanceRecords_Employees" FOREIGN KEY ("employee_id") REFERENCES "Employees"("employee_id")
);

-- 11. Suppliers
CREATE TABLE "Suppliers" (
                             "supplier_id" UNIQUEIDENTIFIER CONSTRAINT "DF_Suppliers_id" DEFAULT NEWSEQUENTIALID(),
                             "name" VARCHAR(255) NOT NULL,
                             "contact_name" VARCHAR(255) NOT NULL,
                             "phone" VARCHAR(255) NOT NULL,
                             "email" VARCHAR(255) NOT NULL,
                             "address" VARCHAR(MAX) NOT NULL,
                             CONSTRAINT "PK_Suppliers" PRIMARY KEY ("supplier_id")
);

-- 12. Parts Inventory
CREATE TABLE "PartsInventory" (
                                  "part_id" UNIQUEIDENTIFIER CONSTRAINT "DF_PartsInventory_id" DEFAULT NEWSEQUENTIALID(),
                                  "name" VARCHAR(255) NOT NULL,
                                  "supplier_id" UNIQUEIDENTIFIER NOT NULL,
                                  "quantity" BIGINT NOT NULL,
                                  "unit_cost" DECIMAL(8, 2) NOT NULL,
                                  CONSTRAINT "PK_PartsInventory" PRIMARY KEY ("part_id"),
                                  CONSTRAINT "FK_PartsInventory_Suppliers" FOREIGN KEY ("supplier_id") REFERENCES "Suppliers"("supplier_id")
);

-- 13. Maintenance Parts
CREATE TABLE "MaintenanceParts" (
                                    "record_id" UNIQUEIDENTIFIER NOT NULL,
                                    "part_id" UNIQUEIDENTIFIER NOT NULL,
                                    "quantity_used" BIGINT NOT NULL,
                                    CONSTRAINT "PK_MaintenanceParts" PRIMARY KEY ("record_id", "part_id"),
                                    CONSTRAINT "FK_MaintenanceParts_Records" FOREIGN KEY ("record_id") REFERENCES "MaintenanceRecords"("record_id") ON DELETE CASCADE,
                                    CONSTRAINT "FK_MaintenanceParts_Parts" FOREIGN KEY ("part_id") REFERENCES "PartsInventory"("part_id") ON DELETE CASCADE
);