create table inghetate(
    cod_i BIGINT IDENTITY PRIMARY KEY,
    denumire varchar(50) not null,
    descriere varchar(max) not null,
    pret decimal(10,2),
    cod_t Bigint not null,
    CONSTRAINT fk_tip FOREIGN KEY (cod_t) references tip_inghetata(cod_t)
)

create table tip_inghetata(
    cod_t BIGINT IDENTITY PRIMARY KEY,
    denumire varchar(50) not null,
    descriere varchar(max) not null,
)

create table copii (
    cod_c BIGINT IDENTITY PRIMARY KEY,
    nume varchar(50) not null,
    prenume varchar(50) not null,
    gen varchar(20) check (gen in ('Masculin','Feminin')) not null,
    varsta int check (varsta > 0) not null,
    cod_i bigint,
    constraint fk_inghetat foreign key (cod_i) references inghetate(cod_i)
)

create table comenzi(
    cod_cm BIGINT IDENTITY PRIMARY KEY,
    denumire varchar(50) not null,
    cantitate int check (cantitate > 0) not null,
    pret decimal(10,2)
)

create table comenzi_inghetate(
    cod_cm bigint not null,
    cod_i bigint not null,
    data_livrarii date not null,
    discount int check (discount between 0 and 100),
    constraint pk_comenzi_inghetate primary key (cod_cm,cod_i),
    constraint fk_inghetate foreign key (cod_i) references inghetate(cod_i),
    constraint fk_comenzi foreign key (cod_cm) references comenzi(cod_cm)
)

create or alter procedure AdaugaInghetataComenzi
    @cod_cm BIGINT,
    @cod_i BIGINT,
    @data_livrare Date,
    @discount int
AS
BEGIN
    print 'date check'
    if @data_livrare < CAST(getdate() as date)
    begin
        print 'Data de livrare nu poate fi in trecut'
        return
    end

    print 'discount check'
    if @discount not between 0 and 100
    begin
        print 'Discount aplicat trebuie sa fie o valoare intre 0 si 100'
    end

    print 'comanda check'
    if not exists(select * from comenzi where @cod_cm = cod_cm)
    begin
        print 'Comanda data nu exista'
        return
    end

    print 'inghetata check'
    if not exists(select * from inghetate where @cod_i = cod_i)
    begin
        print 'Inghetata data nu exista'
        return
    end

    if not exists(select * from comenzi_inghetate where cod_cm = @cod_cm and cod_i = @cod_i)
    begin
        insert into comenzi_inghetate(cod_cm, cod_i, data_livrarii, discount)
        values (@cod_cm,@cod_i,@data_livrare,@discount)
        print 'Inghetata '+CAST(@cod_i as varchar(10))+' adaugata comenzii '+CAST(@cod_cm as varchar(10))
    end else
    begin
        update comenzi_inghetate
        set data_livrarii=@data_livrare, discount=@discount
        where cod_cm = @cod_cm and @cod_i = cod_i
        print 'Inghetata '+CAST(@cod_i as varchar(10)) +' din comanda '+CAST(@cod_cm as varchar(10)) + ' a fost actualizata cu succes'
    end

end

insert into comenzi (denumire, cantitate, pret) values ('comanda 1', 5,10.2)
insert into tip_inghetata (denumire, descriere) values ('denumire 1', 'descriere')
insert into inghetate (denumire, descriere, pret, cod_t) values ('denumire ing', 'dseds', 13.2, 1)

exec AdaugaInghetataComenzi @cod_cm = 1, @cod_i = 1, @data_livrare = '10-10-2027',  @discount = 0
select *
from comenzi_inghetate;

CREATE OR ALTER FUNCTION InghetataStats(@n INT)
    RETURNS @ResultTable TABLE (denumire VARCHAR(255))
AS
BEGIN
    IF @n < 1
        BEGIN
            INSERT INTO @ResultTable (denumire) VALUES ('Error: N must be >= 1');
            RETURN;
        END

    INSERT INTO @ResultTable (denumire)
    SELECT i.denumire
    FROM inghetate i
             INNER JOIN comenzi_inghetate ci ON i.cod_i = ci.cod_i
    GROUP BY i.denumire
    HAVING COUNT(*) >= @n;
    IF NOT EXISTS (SELECT 1 FROM @ResultTable)
        BEGIN
            INSERT INTO @ResultTable (denumire) VALUES ('No ice cream found for this criteria');
        END
    RETURN;
END;
    CREATE OR ALTER FUNCTION dbo.InghetataStats_Inline (@n INT)
        RETURNS TABLE
            AS
            RETURN
            (
            SELECT i.denumire
            FROM inghetate i
                     INNER JOIN comenzi_inghetate ci ON i.cod_i = ci.cod_i
            GROUP BY i.denumire
            HAVING COUNT(*) >= @n AND @n >= 1 
            );
select * from InghetataStats(1)