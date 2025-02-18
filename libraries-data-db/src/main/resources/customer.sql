drop table if exists customer;

CREATE TABLE customer
(
    id integer NOT NULL,
    fullname character varying(255),
    email character varying(255),
    CONSTRAINT customer_pkey PRIMARY KEY (id)
);

--这样访问
docker exec -it dbz-mysql-master mysql -uroot -proot
docker exec -it dbz-mysql-slave mysql -uroot -proot
docker exec -it dbz-mysql-target mysql -uroot -proot

--添加列
ALTER TABLE customer ADD sex integer;