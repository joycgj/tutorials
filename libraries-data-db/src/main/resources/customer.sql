drop table if exists customer;

CREATE TABLE customer
(
    id integer NOT NULL,
    fullname character varying(255),
    email character varying(255),
    CONSTRAINT customer_pkey PRIMARY KEY (id)
);

--这样访问
mysql -h127.0.0.1 -P13306 -uroot -p
mysql -h127.0.0.1 -P13305 -uroot -p

--添加列
ALTER TABLE customer ADD sex integer;