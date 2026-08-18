

-- Password for user1 was 'user1'
insert into users(id,email,password,first_name,last_name,role) values (0, 'user1@example.com','$2a$04$qr7RWyqOnWWC1nwotUW1nOe1RD5.mKJVHK16WZy6v49pymu1WDHmi','User','1','USER');
-- Password for admin was 'admin'
insert into users(id,email,password,first_name,last_name,role) values (1,'admin1@example.com','$2a$04$0CF/Gsquxlel3fWq5Ic/ZOGDCaXbMfXYiXsviTNMQofWRXhvJH3IK','Admin','1','ADMIN');
-- Password for user2 was 'user2'
insert into users(id,email,password,first_name,last_name,role) values (2,'user2@example.com','$2a$04$PiVhNPAxunf0Q4IMbVeNIuH4M4ecySWHihyrclxW..PLArjLbg8CC','User2','2','USER');

-- Work logs --
insert into work_logs (id,explanation,created_date,created_by) values (100,'Reviewed the security configuration','2023-07-03 20:30:00',0);
insert into work_logs (id,explanation,created_date,created_by) values (101,'Prepared the client meeting notes','2023-12-23 13:00:00',2);
insert into work_logs (id,explanation,created_date,created_by) values (102,'Audited the login flow','2023-09-14 11:30:00',1);


-- the end --
