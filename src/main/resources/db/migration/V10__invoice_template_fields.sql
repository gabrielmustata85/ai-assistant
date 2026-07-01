-- Date complete pentru factura în stil factureaza.ro.
ALTER TABLE company ADD COLUMN reg_com VARCHAR(64);
ALTER TABLE company ADD COLUMN address VARCHAR(512);
ALTER TABLE company ADD COLUMN iban    VARCHAR(34);
ALTER TABLE company ADD COLUMN bank    VARCHAR(64);
ALTER TABLE company ADD COLUMN phone   VARCHAR(32);
ALTER TABLE company ADD COLUMN email   VARCHAR(128);

ALTER TABLE partner ADD COLUMN reg_com VARCHAR(64);

ALTER TABLE invoice ADD COLUMN unit       VARCHAR(16);
ALTER TABLE invoice ADD COLUMN quantity   NUMERIC(14,3);
ALTER TABLE invoice ADD COLUMN unit_price NUMERIC(14,2);
