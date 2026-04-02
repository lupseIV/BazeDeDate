# Raport Laborator 2: Tranzacții și Niveluri de Izolare

## Obiectiv
Înțelegerea conceptelor de tranzacție a bazei de date, implementarea și demonstrarea diferitelor niveluri de izolare, a problemelor de concurență și a optimizărilor de procesare în lot (batch).

---

## 1. Demonstrații ale Problemelor de Concurență

### A. Demonstrație Dirty Read (Citire Murdară)

**Explicație:**  
Un "dirty read" are loc atunci când o tranzacție citește date care au fost modificate de o altă tranzacție concurentă, dar care nu au fost încă confirmate (comise). Dacă tranzacția care a făcut modificarea face rollback, datele citite de prima tranzacție devin invalide.

**Log-uri Consola:**
```text
Properties loaded successfully!
--- Datele au fost resetate la starea initiala ---

=== A. Dirty Read ===

--- Starea curenta a bazei de date ---
ID: B24940C6-9D2E-F111-A0B5-10FFE002A93A | Name: Ion Popescu | Role: Technician | Email: emp1@test.com
ID: B34940C6-9D2E-F111-A0B5-10FFE002A93A | Name: Maria Ionescu | Role: Sales | Email: emp2@test.com
---------------------------------------

Tranzactia B: Sleep 1 secunda pentru a se asigura ca A a facut update-ul necomis...
Tranzactia A: BEGIN TRANSACTION
Tranzactia A: Rol actualizat la 'Manager' (necomis)
Tranzactia A: Sleep 3 secunde pentru a permite tranzactiei B sa citeasca valoarea necomisa...
Tranzactia B: BEGIN TRANSACTION (READ UNCOMMITTED)
Tranzactia B: Valoarea rolului citita este: 'Manager'
Tranzactia A: ROLLBACK efectuat!

--- Starea curenta a bazei de date ---
ID: B24940C6-9D2E-F111-A0B5-10FFE002A93A | Name: Ion Popescu | Role: Technician | Email: emp1@test.com
ID: B34940C6-9D2E-F111-A0B5-10FFE002A93A | Name: Maria Ionescu | Role: Sales | Email: emp2@test.com
```

**Analiză:**  
Tranzacția B a citit valoarea "Manager", deși această modificare a fost anulată ulterior de Tranzacția A. Nivelul de izolare care permite această problemă este `READ UNCOMMITTED`. Problema se previne crescând nivelul de izolare la `READ COMMITTED`.

---

### B. Demonstrație Non-Repeatable Read (Citire Nerepetabilă)

**Explicație:**  
Apare atunci când o tranzacție citește același rând de două ori și obține date diferite, deoarece o altă tranzacție a modificat (și comis) acel rând între cele două citiri.

**Log-uri Consola:**
```text
=== B. Non-Repeatable Read ===

--- Starea curenta a bazei de date ---
ID: 224786F0-9D2E-F111-A0B5-10FFE002A93A | Name: Ion Popescu | Role: Technician | Email: emp1@test.com
ID: 234786F0-9D2E-F111-A0B5-10FFE002A93A | Name: Maria Ionescu | Role: Sales | Email: emp2@test.com
---------------------------------------

Tranzactia B: Sleep 1 secunda pentru a se asigura ca A a facut prima citire...
Tranzactia A: BEGIN TRANSACTION (READ COMMITTED)
Tranzactia A - Prima citire: 'Technician'
Tranzactia A: Sleep 3 secunde pentru a permite tranzactiei B sa faca update-ul si commit-ul...
Tranzactia B: BEGIN TRANSACTION
Tranzactia B: Rol actualizat la 'Manager' si comis.
Tranzactia A - A doua citire: 'Manager'

--- Starea curenta a bazei de date ---
ID: 224786F0-9D2E-F111-A0B5-10FFE002A93A | Name: Ion Popescu | Role: Manager | Email: emp1@test.com
ID: 234786F0-9D2E-F111-A0B5-10FFE002A93A | Name: Maria Ionescu | Role: Sales | Email: emp2@test.com
---------------------------------------
```

**Analiză:**  
Cele două citiri din Tranzacția A au returnat valori diferite ("Technician", apoi "Manager"). Nivelul de izolare `READ COMMITTED` permite această anomalie. Se previne utilizând nivelul `REPEATABLE READ`.

---

### C. Demonstrație Phantom Read (Citire Fantomă)

**Explicație:**  
O tranzacție citește un set de rânduri care satisfac o condiție. O a doua tranzacție inserează un rând nou care satisface aceeași condiție. Când prima tranzacție repetă citirea, obține un rând în plus (fantoma).

**Log-uri Consola:**
```text
=== C. Phantom Read ===

--- Starea curenta a bazei de date ---
ID: 651842FD-9D2E-F111-A0B5-10FFE002A93A | Name: Ion Popescu | Role: Technician | Email: emp1@test.com
ID: 661842FD-9D2E-F111-A0B5-10FFE002A93A | Name: Maria Ionescu | Role: Sales | Email: emp2@test.com
---------------------------------------

Tranzactia B: Sleep 1 secunda pentru a se asigura ca A a facut prima numaratoare...
Tranzactia A: BEGIN TRANSACTION (REPEATABLE READ)
Tranzactia A - Prima numaratoare (Sales): 1
Tranzactia A: Sleep 3 secunde pentru a permite tranzactiei B sa insereze un nou angajat cu rol 'Sales' si sa comita...
Tranzactia B: BEGIN TRANSACTION
Tranzactia B: Angajat nou inserat cu rolul 'Sales' si comis.
Tranzactia A - A doua numaratoare (Sales): 2

--- Starea curenta a bazei de date ---
ID: 651842FD-9D2E-F111-A0B5-10FFE002A93A | Name: Ion Popescu | Role: Technician | Email: emp1@test.com
ID: 661842FD-9D2E-F111-A0B5-10FFE002A93A | Name: Maria Ionescu | Role: Sales | Email: emp2@test.com
ID: 671842FD-9D2E-F111-A0B5-10FFE002A93A | Name: Angajat Nou Test | Role: Sales | Email: emp3@test.com
---------------------------------------
```

**Analiză:**  
Nivelul `REPEATABLE READ` nu previne inserările noi. Problema se previne doar cu `SERIALIZABLE`.

---

### D. Demonstrație Lost Update (Actualizare Pierdută)

**Explicație:**  
Două tranzacții citesc aceeași valoare, o modifică și fac update. Una dintre modificări se pierde.

**Log-uri Consola:**
```text
=== D. Lost Update ===

--- Starea curenta a bazei de date ---
ID: DDF5D619-9E2E-F111-A0B5-10FFE002A93A | Name: Ion Popescu | Role: Technician | Email: emp1@test.com
ID: DEF5D619-9E2E-F111-A0B5-10FFE002A93A | Name: Maria Ionescu | Role: Sales | Email: emp2@test.com
---------------------------------------

Tranzactia A: BEGIN TRANSACTION
Tranzactia B: BEGIN TRANSACTION
Tranzactia A: A citit prenumele 'Ion'
Tranzactia B: A citit prenumele 'Ion'
Tranzactia A: A calculat noul prenume 'Ion-A'
Tranzactia B: A calculat noul prenume 'Ion-B'
Tranzactia A: COMMIT cu succes (Prenume setat la 'Ion-A')
Tranzactia B: COMMIT cu succes (Prenume setat la 'Ion-B')

--- Starea curenta a bazei de date ---
ID: DDF5D619-9E2E-F111-A0B5-10FFE002A93A | Name: Ion-B Popescu | Role: Technician | Email: emp1@test.com
ID: DEF5D619-9E2E-F111-A0B5-10FFE002A93A | Name: Maria Ionescu | Role: Sales | Email: emp2@test.com
---------------------------------------
```


**Analiză:**  
Modificarea lui A este pierdută. Soluții:
- Blocare pesimistă (`SELECT ... FOR UPDATE`)
- Blocare optimistă (coloană de versiune)

## Scenarii de Utilizare pentru Niveluri de Izolare

### READ UNCOMMITTED
**Descriere:** Permite citirea datelor necomise (dirty reads).

**Scenarii de utilizare:**
- Dashboard-uri de analytics în timp real (ex: număr de utilizatori activi)
- Sisteme de monitorizare unde aproximația este acceptabilă
- Raportări rapide pe volume foarte mari de date
- Situații în care performanța este mai importantă decât acuratețea

---

### READ COMMITTED (Default)
**Descriere:** Permite citirea doar a datelor comise.

**Scenarii de utilizare:**
- Aplicații web standard (CRUD)
- Sisteme de gestiune a utilizatorilor
- Aplicații enterprise obișnuite
- API-uri backend unde consistența de bază este suficientă

---

### REPEATABLE READ
**Descriere:** Asigură că datele citite nu se modifică în timpul tranzacției.

**Scenarii de utilizare:**
- Generare facturi
- Calcul salarii (state de plată)
- Rapoarte financiare pe termen scurt
- Procese unde aceleași date trebuie recitite consistent

---

### SERIALIZABLE
**Descriere:** Cel mai strict nivel – tranzacțiile sunt complet izolate.

**Scenarii de utilizare:**
- Transferuri bancare
- Sisteme de plăți
- Sisteme de rezervări (bilete avion, hoteluri)
- E-commerce cu stoc limitat
- Orice sistem critic unde consistența absolută este obligatorie

---

## 2. Demonstrație Deadlock (Blocaj reciproc)

**Explicație:**  
Două tranzacții se blochează reciproc așteptând resursele celeilalte.

**Log-uri Consola:**
```text
=== 2. Demonstratie Deadlock ===

--- Starea curenta a bazei de date ---
ID: 0859CF56-9E2E-F111-A0B5-10FFE002A93A | Name: Ion Popescu | Role: Technician | Email: emp1@test.com
ID: 0959CF56-9E2E-F111-A0B5-10FFE002A93A | Name: Maria Ionescu | Role: Sales | Email: emp2@test.com
---------------------------------------

Tranzactia B: BEGIN TRANSACTION
Tranzactia A: BEGIN TRANSACTION
Tranzactia B: A blocat Angajat 2 (emp2)
Tranzactia B: Incearca sa blocheze Angajat 1 (emp1)...
Tranzactia A ERROR (Deadlock): Transaction (Process ID 79) was deadlocked on lock resources with another process and has been chosen as the deadlock victim. Rerun the transaction.

--- Starea curenta a bazei de date ---
ID: 0859CF56-9E2E-F111-A0B5-10FFE002A93A | Name: Ion Popescu | Role: Manager | Email: emp1@test.com
ID: 0959CF56-9E2E-F111-A0B5-10FFE002A93A | Name: Maria Ionescu | Role: Manager | Email: emp2@test.com
---------------------------------------
```

**Rezolvare:**  
SGBD-ul detectează deadlock-ul și oprește una dintre tranzacții.  
Prevenire: accesarea resurselor în aceeași ordine.

---

## 3. Analiza Nivelurilor de Izolare și Compromisuri

| Nivel de Izolare | Dirty Read | Non-Repeatable | Phantom Read | Performanță | Scenariu |
|-----------------|-----------|----------------|--------------|-------------|----------|
| READ UNCOMMITTED | Permis | Permis | Permis | Foarte ridicată | Analytics în timp real |
| READ COMMITTED | Prevenit | Permis | Permis | Ridicată | Aplicații web |
| REPEATABLE READ | Prevenit | Prevenit | Permis | Medie | Facturare |
| SERIALIZABLE | Prevenit | Prevenit | Prevenit | Scăzută | Tranzacții financiare |

---

## 4. Analiza Performanței Inserărilor în Lot (Batch Processing)

### [Csv rezultate](performance_results.csv)
---

### Concluzii

**Auto-commit:**  
Cea mai lentă metodă — fiecare insert = tranzacție separată.

**Commit în loturi:**  
Mult mai rapid — reduce numărul de tranzacții și I/O.

**Tranzacție unică + executeBatch():**  
Cea mai eficientă:
- minim de round-trips
- atomicitate completă
- performanță maximă

