drop table if exists customer;

/**用户表*/
create table customer (
    id int not null AUTO_INCREMENT,  -- 客户id
		user_id varchar(32) null, 
    name varchar(32) null,   -- 客户姓名
		sharding int default 1,
    constraint PK_CUSTOMER primary key (id)
);


drop table if exists order_info;

create table order_info (
    id int not null AUTO_INCREMENT,  -- 业务ID
		order_id varchar(32) null,
		user_id varchar(32) null,
    money BIGINT not null,
		sharding int default 1,
    constraint PK_ORDER primary key (id)
)
