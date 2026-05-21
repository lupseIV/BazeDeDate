# Raport Laborator 5: Migrarea Schemei BD si Metode Avansate

**Student:** Lupse Ioan Victor  
**Data:** Mai 2026  
**Proiect:** TranspaletApp — aplicatie de gestiune a transpaletelor si inchirierilor

---

## 1. Strategia de Migrare Aleasa si Ratiunea

### Instrumentul ales: Liquibase

Am ales **Liquibase** in locul Flyway din mai multe motive practice:

- **Format XML/YAML nativ**: Liquibase suporta descrierea schimbarilor in XML, YAML sau JSON, nu doar SQL raw. Acest lucru permite o portabilitate mai mare intre dialecte de baze de date (SQL Server, PostgreSQL, MySQL) fara a rescrie scripturile.
- **Rollback built-in**: Liquibase ofera suport nativ pentru rollback declarativ prin blocul `<rollback>` in interiorul fiecarui `changeSet`. Flyway necesita scrierea manuala a scripturilor `U__` (undo), iar functionalitatea de rollback este disponibila numai in editia platita Teams.
- **Preconditions**: Mecanismul `<preConditions onFail="MARK_RAN">` permite executarea conditionala a unui changeset — daca tabela sau coloana exista deja, migrarea e marcata ca rulata fara a arunca eroare. Ideal pentru medii de echipa unde schema poate fi deja partial aplicata.
- **Tracking robust**: Liquibase stocheaza istoricul migrarilor in tabela `DATABASECHANGELOG` cu hash MD5 al fiecarui changeset. Daca un changeset este modificat dupa ce a fost aplicat, Liquibase detecteaza inconsistenta si blocheaza rularea — prevenind coruptia accidentala a schemei.

### Structura changelog-ului

```
db/changelog/
├── db.changelog-master.xml          <- punctul de intrare, include toate fisierele
└── changes/
    ├── v2-wheelmaterial-add-density.xml
    ├── v3-new-table-rentals.xml
    ├── v4-seed-rentals.xml
    ├── v5-bearings-diameter-type-decimal.xml
    ├── v6-add-indexes.xml
    ├── v7-optimistic-locking.xml
    ├── v8-soft-delete-rentals.yaml
    ├── v9-migrate-capacity-to-tons.xml
    └── v10-audit-logging.xml
```

Fiecare fisier contine unul sau mai multe `changeSet`-uri, identificate unic prin combinatia `id + author + filename`. Master changelogul include fisierele in ordine, garantand aplicarea secventiala.

---

## 2. Documentatie Pas cu Pas a Executiei Migrarilor

### Migrarea Initiala (v1 — baseline)

Migrarea initiala a fost generata automat din schema existenta a bazei de date folosind plugin-ul Liquibase pentru IntelliJ / `liquibase generateChangeLog`. Aceasta creeaza tabelele de baza ale aplicatiei:

| Tabela               | Descriere                                                              |
|----------------------|------------------------------------------------------------------------|
| `Employees`          | Angajatii firmei (nu sunt folositi in cod Java dar exista in schema)   |
| `WheelMaterials`     | Materialele din care sunt fabricate rotile                             |
| `Bearings`           | Rulmentii asociati rotilor                                             |
| `Wheels`             | Rotile transpaletelor (FK -> Bearings, WheelMaterials)                 |
| `PalletTrucks`       | Transpaletele (FK -> Wheels)                                           |
| `PalletTruckDetails` | Detalii suplimentare ale transpaletului (FK CASCADE -> PalletTrucks)   |

**Cum se aplica:**
```bash
# Cu Gradle (configurat in build.gradle):
./gradlew update
```

### v2 — Adaugare coloana noua (Task B)

**Fisier:** `v2-wheelmaterial-add-density.xml`

```xml
<sql>
    ALTER TABLE WheelMaterials ADD density DECIMAL(10,4) NOT NULL DEFAULT 0.0000;
</sql>
<rollback>
    <sql>ALTER TABLE WheelMaterials DROP COLUMN density;</sql>
</rollback>
```

- Coloana `density` este adaugata cu `NOT NULL DEFAULT 0.0000`, astfel randurile existente primesc o valoare valida imediat.
- Entitatea `WheelMaterial.java` a fost actualizata cu campul `@Column(name = "density") private BigDecimal density`.
- **Rollback:** `DROP COLUMN density` — reversibil complet.

### v3 — Adaugare tabel nou (Task C)

**Fisier:** `v3-new-table-rentals.xml`

Creeaza tabela `Rentals` cu:
- Cheie primara `UNIQUEIDENTIFIER` cu `NEWSEQUENTIALID()` (performanta mai buna decat `NEWID()` aleator pentru index clustered)
- `FK_Rentals_Trucks` referind `PalletTrucks(truck_id)`
- Constrangere `CHECK` pe `return_status IN ('Active', 'Returned', 'Overdue')`
- Entitatea `Rental.java` mapata complet cu `@ManyToOne` spre `PalletTruck`

### v4 — Seeding date (Task C + Bonus)

**Fisier:** `v4-seed-rentals.xml`

Populeaza tabela `Rentals` cu 15 inregistrari de test folosind un `MERGE` care previne inserarea duplicatelor (`WHEN NOT MATCHED THEN INSERT`). Datele sunt legate dinamic de `PalletTrucks` reale prin `ROW_NUMBER()`, independent de UUID-urile generate.

### v5 — Modificare coloana existenta (Task D)

**Fisier:** `v5-bearings-diameter-type-decimal.xml`

```sql
ALTER TABLE Bearings ALTER COLUMN diameter DECIMAL(10,2) NOT NULL;
-- Rollback:
ALTER TABLE Bearings ALTER COLUMN diameter BIGINT NOT NULL;
```

Coloana `diameter` a fost schimbata din `BIGINT` in `DECIMAL(10,2)` pentru a permite valori fractionate ale diametrului rulmentilor. Entitatea `Bearing.java` folosea deja `Double` in Java, deci mapping-ul era consistent.

### v6 — Adaugare indexuri (Task E)

**Fisier:** `v6-add-indexes.xml`

| Index                            | Tabela       | Coloana       | Justificare                                      |
|----------------------------------|--------------|---------------|--------------------------------------------------|
| `idx_pallettrucks_serial_number` | PalletTrucks | serial_number | Cautare frecventa dupa numar de serie            |
| `idx_rentals_truck_id`           | Rentals      | truck_id      | FK-ul din JOIN-uri Rentals -> PalletTrucks       |
| `idx_employees_email`            | Employees    | email         | Lookupuri de autentificare/identificare angajati |

**Masurare performanta** (clasa `IndexPerformanceDemo.java`):

Metoda `benchmark()` ruleaza fiecare query de `WARMUP_RUNS=5` ori (pentru a incalzi cache-ul de query plan), apoi masoara `MEASURED_RUNS=50` executii si afiseaza media in milisecunde. Pe tabele cu volum mic (date de test), overhead-ul indexului poate fi vizibil; pe productie cu mii de randuri, speedup-ul tipic este de 10-100x pentru coloane cu cardinalitate ridicata.

### v7 — Optimistic Locking (Cerinta 3)

**Fisier:** `v7-optimistic-locking.xml`

```sql
ALTER TABLE Bearings ADD version int NOT NULL DEFAULT 1;
```

### v8 — Soft Delete (Cerinta 4)

**Fisier:** `v8-soft-delete-rentals.yaml`

Adauga 3 coloane la `Rentals`: `is_deleted BIT`, `deleted_at DATETIME2`, `deleted_by VARCHAR(255)`.

### v9 — Migrare date intre coloane (Bonus)

**Fisier:** `v9-migrate-capacity-to-tons.xml`

Demonstreaza pattern-ul **expand-migrate-contract** in 3 changeseturi separate:
1. `v9-add-capacity-tons-column` — adauga `capacity_tons DECIMAL(10,3) NULL`
2. `v9-migrate-capacity-kg-to-tons` — populeaza: `capacity_tons = capacity_kg / 1000.0`
3. `v9-set-capacity-tons-not-null` — aplica constrangerea `NOT NULL` dupa backfill

Separarea in 3 changeseturi este o best practice: daca pasul de backfill esueaza pe un subset de date, Liquibase poate rula doar acel changeset la reluare, fara a re-executa adaugarea coloanei.

### v10 — Audit Logging (Bonus)

**Fisier:** `v10-audit-logging.xml`

Adauga `created_at` si `updated_at DATETIME2 NOT NULL DEFAULT GETDATE()` la `PalletTrucks` si `Rentals`. Valorile sunt gestionate si la nivel Java prin clasa `AuditableEntity` (`@MappedSuperclass`) cu callback-uri `@PrePersist` si `@PreUpdate`.

---

## 3. Locking Optimist — Explicatie si Rezultatele Demonstratiei

### Ce este Locking-ul Optimist?

Locking-ul optimist este o strategie de control al concurentei care **nu blocheaza randul in baza de date** atunci cand o tranzactie citeste date. In schimb, fiecare rand are o coloana `version` (numar intreg). Cand o tranzactie doreste sa salveze modificarile, verifica daca versiunea din baza de date este aceeasi cu cea citita initial:

- **Daca versiunile coincid** -> nicio alta tranzactie nu a modificat randul -> salvarea reuseste -> versiunea se incrementeaza automat.
- **Daca versiunile difera** -> altcineva a modificat randul intre timp -> `OptimisticLockException` -> aplicatia gestioneaza conflictul.

**Comparatie cu Locking Pesimist:**

| Caracteristica       | Locking Optimist               | Locking Pesimist                   |
|----------------------|--------------------------------|------------------------------------|
| Mecanism             | Coloana `version` + verificare | `SELECT FOR UPDATE` / lock pe rand |
| Conflict             | Detectat la commit             | Prevenit prin blocare              |
| Performanta (citire) | Excelenta — fara blocaje       | Degradata — asteptare lock         |
| Potrivit pentru      | Rate scazuta de conflicte      | Rate ridicata de conflicte         |
| Risc                 | Retry necesar la conflict      | Deadlock posibil                   |

### Implementare

```java
// Bearing.java
@Version
@Column(name = "version", nullable = false)
private int version = 1;
```

JPA/Hibernate gestioneaza automat incrementarea versiunii si arunca `OptimisticLockException` (infasurata in `RollbackException` la commit) daca versiunea din UPDATE WHERE nu mai corespunde.

### Demonstratia — Scenariul din `OptimisticLockingDemo.java`

```
--- SIMULARE CONCURENTA: LOCKING OPTIMIST ---
Utilizatorul A a incarcat Bearing cu versiunea: 1
Utilizatorul B a incarcat Bearing cu versiunea: 1

Utilizatorul A modifica diametrul la 100...
Utilizatorul A a salvat cu succes! Noua versiune in BD este: 2

Utilizatorul B incearca sa modifice diametrul la 200...

[EROARE] Datele au fost modificate de un alt utilizator !
Alege o optiune pentru a rezolva conflictul:
  1 - Reincarcarea datelor (pastreaza modificarile celuilalt utilizator)
  2 - Actualizare fortata (suprascrie cu modificarile tale)
  3 - Anulare (renunta la tot)
Optiunea ta: _
```

**Cum functioneaza intern:** Hibernate genereaza:
```sql
UPDATE Bearings SET diameter=200, version=2 WHERE bid=? AND version=1
```
Deoarece `version=1` nu mai exista (a devenit 2 dupa salvarea lui A), `UPDATE` afecteaza 0 randuri -> Hibernate arunca `OptimisticLockException`.

**Gestionarea conflictului** ofera 3 optiuni:
1. **Reload** — se re-citesc datele proaspete, utilizatorul vede ce a salvat A.
2. **Force update** — se citeste entitatea cu versiunea curenta (2) si se suprascrie cu valoarea dorita de B.
3. **Cancel** — operatiunea se abandoneaza.

Conflictul este inregistrat in log cu `logger.warn()` pentru debugging:
```
CONFLICT DE VERSIUNE detectat pentru Bearing ID: <uuid>.
Utilizatorul B a incercat sa salveze versiunea 1, dar in BD versiunea este mai noua.
```

---

## 4. Stergere Soft vs. Hard — Comparatie si Compromisuri

### Implementare Stergere Soft pe `Rentals`

**Migrare (v8):** Adauga `is_deleted`, `deleted_at`, `deleted_by` la tabela `Rentals`.

**Entitate (`Rental.java`):**
```java
@SQLDelete(sql = "UPDATE Rentals SET is_deleted=1, deleted_at=GETDATE(), deleted_by='system' WHERE rental_id=?")
@FilterDef(name = "deletedFilter", parameters = @ParamDef(name = "isDeleted", type = Boolean.class))
@Filter(name = "deletedFilter", condition = "is_deleted = :isDeleted")
public class Rental extends AuditableEntity implements Identifiable<UUID> {
    private boolean isDeleted = false;
    private LocalDateTime deletedAt;
    private String deletedBy;

    public void softDelete(String username) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = username;
    }
}
```

**Repository (`RentalJpaRepository.java`):**

| Metoda                      | Comportament                                                     |
|-----------------------------|------------------------------------------------------------------|
| `findAll()`                 | Activeaza filtrul `deletedFilter=false` — exclude stergerile     |
| `findAllIncludingDeleted()` | Fara filtru — returneaza toate randurile (vizualizare admin)     |
| `deleteById(UUID)`          | Apeleaza `em.remove()` -> declanseaza `@SQLDelete` (soft)        |
| `hardDeleteById(UUID)`      | `DELETE FROM Rentals WHERE id=?` — stergere fizica               |
| `restoreById(UUID)`         | Seteaza `is_deleted=false`, `deleted_at=null`, `deleted_by=null` |

### Comparatie: Soft Delete vs. Hard Delete

| Criteriu                     | Soft Delete                                         | Hard Delete                               |
|------------------------------|-----------------------------------------------------|-------------------------------------------|
| **Recuperabilitate**         | Da — `restoreById()` restaureaza imediat            | Nu — datele sunt pierdute permanent       |
| **Audit trail**              | Complet — cine si cand a sters                      | Absent — nicio urma dupa stergere         |
| **Integritate referentiala** | Simpla — FK-urile raman intacte                     | Complexa — CASCADE sau orfani             |
| **Spatiu in baza de date**   | Creste — randurile raman fizic                      | Optim — spatiul este eliberat             |
| **Complexitate query**       | Crescuta — filtru `WHERE is_deleted=0` necesar      | Simpla — nu exista randuri "ascunse"      |
| **GDPR / Dreptul la uitare** | Problematic — datele nu sunt cu adevarat sterse     | Conformant — stergere completa            |
| **Caz de utilizare ideal**   | Tranzactii business, comenzi, contracte             | Date temporare, log-uri, cache            |

**Concluzie:** Pentru `Rentals` (contracte de inchiriere), stergerea soft este alegerea corecta — nu vrei sa pierzi istoricul unui contract activ printr-un click gresit. Hard delete se pastreaza ca optiune administrativa explicita (`delete(id, true)`).

---

## 5. Lectii Invatate, Best Practices si Provocari

### Best Practices Respectate

**1. Un changeset = o schimbare atomica**  
Fiecare schimbare logica (adaugare coloana, creare tabel, seeding) este un changeset separat. Daca ceva esueaza, Liquibase reia exact de la changeset-ul care a dat gres.

**2. Rollback explicit pentru toate changeseturile**  
Fiecare `<changeSet>` contine un bloc `<rollback>` — esential pentru a putea reveni la o versiune anterioara in cazul unui deployment esuat:
```bash
./gradlew rollback -PliquibaseCommandValue=1   # revine cu 1 changeset
```

**3. Preconditions pentru idempotenta**  
`<preConditions onFail="MARK_RAN">` face ca migrarea sa fie sigura de re-rulat — daca schema exista deja (de exemplu, aplicata manual pe un mediu de staging), changeset-ul e marcat ca executat fara eroare.

**4. Separarea datelor de schema**  
Seed-ul (`v4`) este un changeset separat de crearea tabelei (`v3`). Astfel, rollback-ul tabelei nu trebuie sa stie nimic despre date, si datele pot fi actualizate independent.

**5. Pattern expand-migrate-contract (v9)**  
Adaugarea unei coloane derivate in 3 pasi (add nullable -> backfill -> enforce NOT NULL) este sigura in productie: nu blocheaza tabelul, permite rollback granular, si poate fi pausata intre pasi.

### Provocari Intampinate

**1. `RollbackException` vs. `OptimisticLockException`**  
Initial, blocul `catch (OptimisticLockException e)` nu era niciodata atins deoarece Hibernate infasoara exceptia in `RollbackException` la momentul commit-ului tranzactiei. Solutia: detectarea cauzei in `catch (RollbackException e)` cu `e.getCause() instanceof OptimisticLockException`.

**2. `@SQLDelete` si username-ul utilizatorului**  
Adnotarea `@SQLDelete` intercepteaza `em.remove()` dar nu primeste parametri din aplicatie (doar ID-ul entitatii). Din acest motiv, `deleted_by` este setat la `'system'` in path-ul JPA `remove()`. Pentru capturarea username-ului real, se apeleaza direct `softDelete(username)` urmat de `em.merge()` — fara `em.remove()`.

**3. Duplicate IDs in changeseturi**  
Changeseturile `v4` si `v7` aveau ambele `id="0"` cu `author="admin"`. Desi Liquibase identifica unic un changeset prin `id + author + filename` (nu doar `id`), este o practica proasta care poate cauza confuzie. Best practice: folositi ID-uri descriptive (ex: `v7-add-version-bearings`).

**4. Seeding cu UUID-uri generate dinamic**  
Datele de test din `v4` trebuiau sa fie legate de `PalletTrucks` existente cu UUID-uri generate de baza de date (nu cunoscute in prealabil). Solutia: `ROW_NUMBER() OVER (ORDER BY truck_id)` pentru a atribui un index secvential truck-urilor existente, folosit in JOIN pentru inserare.

---

*Raport generat pentru Laboratorul 5 — Baze de Date, Semestrul 2*
