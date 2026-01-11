create table atracti(
    cod_a BIGINT IDENTITY PRIMARY KEY,
    nume VARCHAR(100) NOT NULL,
    descriere VARCHAR(max) DEFAULT 'Default description',
    varsta_min BIGINT,
    cod_s BIGINT NOT NULL FOREIGN KEY references sectiuni(cod_s)
)

create table sectiuni(
    cod_s BIGINT IDENTITY PRIMARY KEY,
    nume VARCHAR(100) NOT NULL,
    descriere VARCHAR(max) DEFAULT 'Default description',
)

create table vizitatori(
    cod_v BIGINT IDENTITY PRIMARY KEY,
    nume varchar(100) NOT NULL,
    email varchar(50) NOT NULL,
    cod_c BIGINT NOT NULL FOREIGN KEY references categorii_vizitatori(cod_c)
)

create table categorii_vizitatori(
    cod_c BIGINT IDENTITY PRIMARY KEY,
    nume varchar(100) not null
)

create table note(
    cod_a BIGINT NOT NULL,
    cod_v BIGINT NOT NULL,
    nota BIGINT CHECK (nota BETWEEN 1 and 10),
    CONSTRAINT  pk_atractie_vizitator primary key (cod_a,cod_v),
    CONSTRAINT fk_atractie foreign key (cod_a) references atracti(cod_a),
    CONSTRAINT fk_vizitator foreign key (cod_v) references vizitatori(cod_v),
)

INSERT INTO categorii_vizitatori(nume)
VALUES ('tiner'),
        ('studenti'),
       ('pensionari'),('profesori'),('elevi'),('copii'),('asociati'),('angajati')

INSERT INTO sectiuni(nume)
VALUES ('Sectiunea 1'),('Sectiunea 2'),('Sectiunea 3'),('Sectiunea 4'),
       ('Sectiunea 5'),('Sectiunea 6'),('Sectiunea 7')

INSERT INTO atracti(nume,varsta_min,cod_s)
VALUES ('atractie 1', 5, 1),('atractie 2', 6, 2),('atractie 3', 7, 3),
       ('atractie 4', 8, 4),('atractie 5', 9, 5),('atractie 6', 10, 6),
       ('atractie 7', 12, 7)

insert into vizitatori(nume,email,cod_c)
VALUES ('vizitator 1', 'viz1@gmail.com', 1), ('vizitator 2', 'viz2@gmail.com', 2),
       ('vizitator 3', 'viz3@gmail.com', 3), ('vizitator 4', 'viz4@gmail.com', 4),
       ('vizitator 5', 'viz5@gmail.com', 5), ('vizitator 6', 'viz6@gmail.com', 6),
       ('vizitator 7', 'viz7@gmail.com', 7)

insert into note(cod_a, cod_v, nota)
VALUES (1,1,1),(2,3,5),(5,3,8),
       (2,5,4),(4,3,10),(1,2,9),
       (1,7,7)

UPDATE categorii_vizitatori
set nume = 'tineret'
where cod_c = 1

update sectiuni
set nume = 'Sectiune updatata'
where cod_s = 1

update atracti
set nume = 'Atractie updatata', descriere = 'Descriere Noua'
where cod_s = 3

update vizitatori
set nume = 'Marius', email = 'marius@gmail.com', cod_c =3
where cod_v = 4

update note
set nota = 3
where cod_v = 3 and cod_a = 4

DELETE FROM note where cod_v = 1 and cod_a = 1

-- 1. Remove the old constraint
ALTER TABLE atracti
    DROP CONSTRAINT FK_OldConstraintName;

-- 2. Add the new one with Cascade
ALTER TABLE atracti
    ADD CONSTRAINT FK_atracti_sectiuni
        FOREIGN KEY (cod_s) REFERENCES Sectiuni(cod_s)
            ON DELETE CASCADE;

DELETE FROM atracti where cod_a = 1
