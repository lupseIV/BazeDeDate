USE proiect_sem2;
GO

BEGIN
    -- ==========================================
    -- 1. Bearings & Materials (No Dependencies)
    -- ==========================================
    DECLARE @Map_Bearings TABLE (OldID INT, NewID UNIQUEIDENTIFIER);
    MERGE INTO "Bearings" AS target USING (VALUES
                                               (1, 25), (2, 30), (3, 28), (4, 35), (5, 32), (6, 29), (7, 27), (8, 26), (9, 33), (10, 31), (11, 24), (12, 36), (13, 29), (14, 34), (15, 25)
    ) AS src(OldID, dia) ON 1=0 WHEN NOT MATCHED THEN INSERT ("diameter") VALUES (src.dia)
        OUTPUT src.OldID, INSERTED.bid INTO @Map_Bearings(OldID, NewID);

    DECLARE @Map_Materials TABLE (OldID INT, NewID UNIQUEIDENTIFIER);
    MERGE INTO "WheelMaterials" AS target USING (VALUES
                                                     (1, 'Rubber', 1000), (2, 'Polyurethane', 1200), (3, 'Steel', 2000), (4, 'Nylon', 800), (5, 'Aluminum', 1500),
                                                     (6, 'Cast Iron', 2200), (7, 'Composite', 1800), (8, 'Rubber', 900), (9, 'Polyurethane', 1300), (10, 'Steel', 2500),
                                                     (11, 'Nylon', 700), (12, 'Aluminum', 1400), (13, 'Composite', 1600), (14, 'Cast Iron', 2100), (15, 'Rubber', 1100)
    ) AS src(OldID, type, mw) ON 1=0 WHEN NOT MATCHED THEN INSERT ("type", "max_weight") VALUES (src.type, src.mw)
        OUTPUT src.OldID, INSERTED.materials_id INTO @Map_Materials(OldID, NewID);

    -- ==========================================
    -- 2. Wheels (Depends on Bearings & Materials)
    -- ==========================================
    DECLARE @Map_Wheels TABLE (OldID INT, NewID UNIQUEIDENTIFIER);
    MERGE INTO "Wheels" AS target USING (
        SELECT src.OldID, m.NewID as mid, src.mw, b.NewID as bid FROM (VALUES
                                                                           (1, 1, 1000, 1), (2, 2, 1200, 2), (3, 3, 2000, 3), (4, 4, 800, 4), (5, 5, 1500, 5),
                                                                           (6, 6, 2200, 6), (7, 7, 1800, 7), (8, 8, 900, 8), (9, 9, 1300, 9), (10, 10, 2500, 10),
                                                                           (11, 11, 700, 11), (12, 12, 1400, 12), (13, 13, 1600, 13), (14, 14, 2100, 14), (15, 15, 1100, 15)
                                                                      ) AS src(OldID, mat_id, mw, bear_id)
                                                                          JOIN @Map_Materials m ON src.mat_id = m.OldID JOIN @Map_Bearings b ON src.bear_id = b.OldID
    ) AS src ON 1=0 WHEN NOT MATCHED THEN INSERT ("materials_id", "max_weight", "bid") VALUES (src.mid, src.mw, src.bid)
        OUTPUT src.OldID, INSERTED.wheels_id INTO @Map_Wheels(OldID, NewID);

    -- ==========================================
    -- 3. PalletTrucks (Depends on Wheels)
    -- ==========================================
    DECLARE @Map_Trucks TABLE (OldID INT, NewID UNIQUEIDENTIFIER);
    MERGE INTO "PalletTrucks" AS target USING (
        SELECT src.OldID, src.sn, src.type, src.model, src.cap, src.status, w.NewID as wid FROM (VALUES
                                                                                                     (1, 'SN1001', 'Manual', 'Model A1', 2500, 'Available', 1), (2, 'SN1002', 'Electric', 'Model E2', 3000, 'Available', 2),
                                                                                                     (3, 'SN1003', 'Manual', 'Model M3', 2000, 'Rented', 3), (4, 'SN1004', 'Electric', 'Model E4', 4000, 'In Maintenance', 4),
                                                                                                     (5, 'SN1005', 'Manual', 'Model M5', 1500, 'Available', 5), (6, 'SN1006', 'Electric', 'Model E6', 3200, 'Retired', 6),
                                                                                                     (7, 'SN1007', 'Manual', 'Model M7', 2800, 'Available', 7), (8, 'SN1008', 'Electric', 'Model E8', 3500, 'Available', 8),
                                                                                                     (9, 'SN1009', 'Manual', 'Model M9', 2500, 'Rented', 9), (10, 'SN1010', 'Electric', 'Model E10', 3700, 'Available', 10),
                                                                                                     (11, 'SN1011', 'Manual', 'Model M11', 2600, 'Available', 11), (12, 'SN1012', 'Electric', 'Model E12', 3400, 'Available', 12),
                                                                                                     (13, 'SN1013', 'Manual', 'Model M13', 2100, 'Available', 13), (14, 'SN1014', 'Electric', 'Model E14', 3300, 'Rented', 14),
                                                                                                     (15, 'SN1015', 'Manual', 'Model M15', 1800, 'Available', 15)
                                                                                                ) AS src(OldID, sn, type, model, cap, status, wheel_id)
                                                                                                    JOIN @Map_Wheels w ON src.wheel_id = w.OldID
    ) AS src ON 1=0 WHEN NOT MATCHED THEN
        INSERT ("serial_number", "type", "model", "capacity_kg", "status", "wheels_id") VALUES (src.sn, src.type, src.model, src.cap, src.status, src.wid)
        OUTPUT src.OldID, INSERTED.truck_id INTO @Map_Trucks(OldID, NewID);

    -- ==========================================
    -- 4. PalletTruckDetails (Depends on Trucks)
    -- ==========================================
    INSERT INTO "PalletTruckDetails" ("truck_id", "purchase_date", "notes", "manufacturer")
    SELECT t.NewID, src.pd, src.notes, src.mfg FROM (VALUES
                                                         (1, '2022-03-12', 'Initial purchase', 'Toyota'), (2, '2023-02-10', 'With upgraded control', 'Linde'),
                                                         (3, '2021-12-01', 'Heavy-duty model', 'Crown'), (4, '2022-07-15', 'Used in warehouse A', 'Yale'),
                                                         (5, '2023-04-22', 'Compact design', 'Nissan'), (6, '2022-01-10', 'Prototype model', 'Hyster'),
                                                         (7, '2022-05-09', 'Excellent performance', 'Toyota'), (8, '2023-03-18', 'Battery replaced', 'Crown'),
                                                         (9, '2022-06-11', 'Stable on slopes', 'Linde'), (10, '2023-01-19', 'Hydraulics upgraded', 'Yale'),
                                                         (11, '2022-04-05', 'Used for small pallets', 'Toyota'), (12, '2023-02-24', 'Fast charging', 'Nissan'),
                                                         (13, '2022-08-02', 'Serviced in 2023', 'Crown'), (14, '2023-06-10', 'High capacity battery', 'Linde'),
                                                         (15, '2022-09-12', 'Regular use', 'Toyota')
                                                    ) AS src(OldID, pd, notes, mfg) JOIN @Map_Trucks t ON src.OldID = t.OldID;

    -- ==========================================
    -- 5. Customers & Rentals & Invoices
    -- ==========================================
    DECLARE @Map_Customers TABLE (OldID INT, NewID UNIQUEIDENTIFIER);
    MERGE INTO "Customers" AS target USING (VALUES
                                                (1, 'LogiTrans', 'Andrei Pop'), (2, 'LiftCorp', 'Maria Ionescu'), (3, 'WareHousePro', 'Ion Stan'), (4, 'TransMove', 'George Radu'),
                                                (5, 'Loaders SRL', 'Diana Ene'), (6, 'Pick&Carry', 'Alex Tudor'), (7, 'FastLift', 'Roxana Dinu'), (8, 'ProLift', 'Cristi Ilie'),
                                                (9, 'QuickTrans', 'Oana Marinescu'), (10, 'CargoFlex', 'Mihai Pavel'), (11, 'HeavyWorks', 'Laura Bota'), (12, 'EcoLift', 'Razvan Ursu'),
                                                (13, 'LiftUp', 'Paula Dragomir'), (14, 'TeraLog', 'Radu Popescu'), (15, 'SmartTrans', 'Irina Olteanu')
    ) AS src(OldID, cn, contact) ON 1=0 WHEN NOT MATCHED THEN INSERT ("company_name", "contact_name", "phone", "email", "address") VALUES (src.cn, src.contact, '0700000000', 'email@test.com', 'Romania')
        OUTPUT src.OldID, INSERTED.customer_id INTO @Map_Customers(OldID, NewID);

    DECLARE @Map_Rentals TABLE (OldID INT, NewID UNIQUEIDENTIFIER);
    MERGE INTO "Rentals" AS target USING (
        SELECT src.OldID, c.NewID as cid, t.NewID as tid, src.sd, src.ed, src.dr, src.tc, src.rs FROM (VALUES
                                                                                                           (1, 1, 1, '2023-01-10', '2023-01-20', 120.00, 1200.00, 'Returned'), (2, 2, 2, '2023-02-01', '2023-02-10', 150.00, 1350.00, 'Returned'),
                                                                                                           (3, 3, 3, '2023-03-05', '2023-03-15', 100.00, 1000.00, 'Returned'), (4, 4, 4, '2023-04-01', NULL, 200.00, NULL, 'Active'),
                                                                                                           (5, 5, 5, '2023-05-01', '2023-05-08', 90.00, 630.00, 'Returned'), (6, 6, 6, '2023-06-01', NULL, 160.00, NULL, 'Active'),
                                                                                                           (7, 7, 7, '2023-07-15', '2023-07-25', 110.00, 1100.00, 'Returned'), (8, 8, 8, '2023-08-10', '2023-08-20', 95.00, 950.00, 'Returned'),
                                                                                                           (9, 9, 9, '2023-09-01', NULL, 175.00, NULL, 'Active'), (10, 10, 10, '2023-10-05', '2023-10-15', 125.00, 1250.00, 'Returned'),
                                                                                                           (11, 11, 11, '2023-11-01', '2023-11-10', 130.00, 1170.00, 'Returned'), (12, 12, 12, '2023-11-20', NULL, 145.00, NULL, 'Active'),
                                                                                                           (13, 13, 13, '2023-12-01', '2023-12-05', 100.00, 500.00, 'Returned'), (14, 14, 14, '2024-01-02', NULL, 180.00, NULL, 'Active'),
                                                                                                           (15, 15, 15, '2024-01-10', NULL, 160.00, NULL, 'Active')
                                                                                                      ) AS src(OldID, cust, truck, sd, ed, dr, tc, rs) JOIN @Map_Customers c ON src.cust = c.OldID JOIN @Map_Trucks t ON src.truck = t.OldID
    ) AS src ON 1=0 WHEN NOT MATCHED THEN INSERT ("customer_id", "truck_id", "start_date", "end_date", "daily_rate", "total_cost", "return_status") VALUES (src.cid, src.tid, src.sd, src.ed, src.dr, src.tc, src.rs)
        OUTPUT src.OldID, INSERTED.rental_id INTO @Map_Rentals(OldID, NewID);

    INSERT INTO "Invoices" ("rental_id", "issue_date", "status") SELECT r.NewID, src.idate, src.status FROM (VALUES
                                                                                                                 (1, '2023-01-21', 'Paid'), (2, '2023-02-11', 'Paid'), (3, '2023-03-16', 'Paid'), (4, '2023-04-12', 'Pending'), (5, '2023-05-09', 'Paid'),
                                                                                                                 (6, '2023-06-05', 'Pending'), (7, '2023-07-26', 'Paid'), (8, '2023-08-21', 'Paid'), (9, '2023-09-05', 'Pending'), (10, '2023-10-16', 'Paid'),
                                                                                                                 (11, '2023-11-11', 'Paid'), (12, '2023-11-25', 'Pending'), (13, '2023-12-06', 'Paid'), (14, '2024-01-05', 'Pending'), (15, '2024-01-11', 'Pending')
                                                                                                            ) AS src(OldID, idate, status) JOIN @Map_Rentals r ON src.OldID = r.OldID;

    -- ==========================================
    -- 6. Employees, Maintenance & Parts
    -- ==========================================
    DECLARE @Map_Employees TABLE (OldID INT, NewID UNIQUEIDENTIFIER);
    MERGE INTO "Employees" AS target USING (VALUES
                                                (1, 'Andrei', 'Popescu', 'Technician', '2020-01-10'), (2, 'Maria', 'Ionescu', 'Driver', '2021-03-12'), (3, 'George', 'Stan', 'Sales', '2019-07-15'),
                                                (4, 'Ioana', 'Radu', 'Manager', '2018-09-20'), (5, 'Alexandru', 'Tudor', 'Admin', '2022-02-05'), (6, 'Roxana', 'Ene', 'Technician', '2020-11-11'),
                                                (7, 'Paul', 'Ilie', 'Driver', '2021-08-18'), (8, 'Cristian', 'Marin', 'Sales', '2019-05-23'), (9, 'Diana', 'Pop', 'Manager', '2018-01-30'),
                                                (10, 'Mihai', 'Dobre', 'Admin', '2022-03-17'), (11, 'Laura', 'Badea', 'Technician', '2020-06-19'), (12, 'Razvan', 'Ursu', 'Driver', '2021-02-25'),
                                                (13, 'Paula', 'Dragan', 'Sales', '2019-10-05'), (14, 'Radu', 'Popa', 'Manager', '2018-12-09'), (15, 'Irina', 'Olteanu', 'Admin', '2022-04-15')
    ) AS src(OldID, fn, ln, role, hd) ON 1=0 WHEN NOT MATCHED THEN INSERT ("first_name", "last_name", "role", "hire_date", "phone", "email") VALUES (src.fn, src.ln, src.role, src.hd, '0700000000', 'email@test.com')
        OUTPUT src.OldID, INSERTED.employee_id INTO @Map_Employees(OldID, NewID);

    DECLARE @Map_Records TABLE (OldID INT, NewID UNIQUEIDENTIFIER);
    MERGE INTO "MaintenanceRecords" AS target USING (
        SELECT src.OldID, t.NewID as tid, src.sd, src.descrip, src.cost, e.NewID as eid FROM (VALUES
                                                                                                  (1, 1, '2023-01-15', 'Oil change', 100.00, 1), (2, 2, '2023-02-20', 'Battery replacement', 350.00, 6), (3, 3, '2023-03-22', 'Brake inspection', 200.00, 11),
                                                                                                  (4, 4, '2023-04-25', 'Hydraulic fix', 500.00, 1), (5, 5, '2023-05-12', 'Wheel alignment', 150.00, 6), (6, 6, '2023-06-18', 'Motor repair', 600.00, 1),
                                                                                                  (7, 7, '2023-07-21', 'Hydraulic seal change', 180.00, 11), (8, 8, '2023-08-09', 'Software update', 250.00, 1), (9, 9, '2023-09-14', 'Sensor calibration', 300.00, 11),
                                                                                                  (10, 10, '2023-10-05', 'Battery service', 400.00, 6), (11, 11, '2023-10-30', 'Fork adjustment', 120.00, 1), (12, 12, '2023-11-12', 'Tire replacement', 220.00, 6),
                                                                                                  (13, 13, '2023-12-01', 'Brake pads change', 180.00, 1), (14, 14, '2024-01-08', 'Lubrication', 90.00, 11), (15, 15, '2024-01-20', 'Full inspection', 600.00, 6)
                                                                                             ) AS src(OldID, truck, sd, descrip, cost, emp) JOIN @Map_Trucks t ON src.truck = t.OldID JOIN @Map_Employees e ON src.emp = e.OldID
    ) AS src ON 1=0 WHEN NOT MATCHED THEN INSERT ("truck_id", "service_date", "description", "cost", "employee_id") VALUES (src.tid, src.sd, src.descrip, src.cost, src.eid)
        OUTPUT src.OldID, INSERTED.record_id INTO @Map_Records(OldID, NewID);

    DECLARE @Map_Suppliers TABLE (OldID INT, NewID UNIQUEIDENTIFIER);
    MERGE INTO "Suppliers" AS target USING (VALUES
                                                (1, 'PartsCo'), (2, 'WheelWorks'), (3, 'LiftTech'), (4, 'BearMaster'), (5, 'SteelMax'), (6, 'PolyParts'), (7, 'TransSupplies'),
                                                (8, 'FastParts'), (9, 'MegaLift'), (10, 'BearingPro'), (11, 'AllParts'), (12, 'EcoSupply'), (13, 'TechLift'), (14, 'SteelParts'), (15, 'HeavySupply')
    ) AS src(OldID, n) ON 1=0 WHEN NOT MATCHED THEN INSERT ("name", "contact_name", "phone", "email", "address") VALUES (src.n, 'Contact', '0700', 'email@test.com', 'Address')
        OUTPUT src.OldID, INSERTED.supplier_id INTO @Map_Suppliers(OldID, NewID);

    DECLARE @Map_Parts TABLE (OldID INT, NewID UNIQUEIDENTIFIER);
    MERGE INTO "PartsInventory" AS target USING (
        SELECT src.OldID, src.n, s.NewID as sid, src.q, src.uc FROM (VALUES
                                                                         (1, 'Bearing Type A', 1, 100, 15.50), (2, 'Bearing Type B', 2, 50, 18.00), (3, 'Rubber Wheel', 3, 80, 25.00), (4, 'Polyurethane Wheel', 4, 60, 30.00), (5, 'Steel Frame', 5, 40, 75.00),
                                                                         (6, 'Hydraulic Pump', 6, 20, 120.00), (7, 'Motor Unit', 7, 15, 250.00), (8, 'Battery Pack', 8, 25, 200.00), (9, 'Fork Assembly', 9, 10, 300.00), (10, 'Brake Kit', 10, 50, 90.00),
                                                                         (11, 'Control Panel', 11, 30, 150.00), (12, 'Sensor Unit', 12, 40, 110.00), (13, 'Hydraulic Seal', 13, 60, 40.00), (14, 'Lubricant', 14, 200, 10.00), (15, 'Wiring Harness', 15, 35, 80.00)
                                                                    ) AS src(OldID, n, supp, q, uc) JOIN @Map_Suppliers s ON src.supp = s.OldID
    ) AS src ON 1=0 WHEN NOT MATCHED THEN INSERT ("name", "supplier_id", "quantity", "unit_cost") VALUES (src.n, src.sid, src.q, src.uc)
        OUTPUT src.OldID, INSERTED.part_id INTO @Map_Parts(OldID, NewID);

    INSERT INTO "MaintenanceParts" ("record_id", "part_id", "quantity_used")
    SELECT r.NewID, p.NewID, src.qu FROM (VALUES
                                              (1, 1, 2), (2, 2, 1), (3, 3, 2), (4, 4, 1), (5, 5, 4), (6, 6, 2), (7, 7, 3), (8, 8, 2), (9, 9, 1), (10, 10, 2), (11, 11, 3), (12, 12, 2), (13, 13, 4), (14, 14, 5), (15, 15, 3)
                                         ) AS src(rec, part, qu) JOIN @Map_Records r ON src.rec = r.OldID JOIN @Map_Parts p ON src.part = p.OldID;
END;
GO