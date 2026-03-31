USE proiect_sem2;
GO

ALTER TABLE "PalletTrucks" DROP CONSTRAINT "FK_PalletTrucks_Wheels";
GO

ALTER TABLE "PalletTrucks"
    ADD CONSTRAINT "FK_PalletTrucks_Wheels"
        FOREIGN KEY ("wheels_id")
        REFERENCES "Wheels"("wheels_id")
        ON DELETE CASCADE;
GO

ALTER TABLE "Rentals" DROP CONSTRAINT "FK_Rentals_Trucks";
GO
Truncate Table "Rentals"
ALTER TABLE "Rentals"
    ADD CONSTRAINT "FK_Rentals_Trucks"
        FOREIGN KEY ("truck_id")
        REFERENCES "PalletTrucks"("truck_id")
        ON DELETE SET NULL;
GO

SELECT
    fk.name                AS FK_Name,
    OBJECT_NAME(fk.parent_object_id)  AS TableName,
    delete_referential_action_desc   AS OnDeleteAction
FROM sys.foreign_keys fk
WHERE fk.name = 'FK_PalletTrucks_Wheels';
