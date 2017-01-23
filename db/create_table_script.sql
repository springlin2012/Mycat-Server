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




/*
Navicat MySQL Data Transfer

Source Server         : env_develop_221
Source Server Version : 50623
Source Host           : 10.10.16.221:3306
Source Database       : test_mycat

Target Server Type    : MYSQL
Target Server Version : 50623
File Encoding         : 65001

Date: 2016-07-01 18:56:17
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for sam_test
-- ----------------------------
DROP TABLE IF EXISTS sam_test;
CREATE TABLE sam_test (
  id bigint(11) NOT NULL AUTO_INCREMENT,
  name_ varchar(255) DEFAULT NULL,
  user_id bigint(20) DEFAULT NULL,
  sharding bigint(255) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT;

-- ----------------------------
-- Table 全局表
-- ----------------------------
CREATE TABLE global_test (
  id bigint(11) NOT NULL AUTO_INCREMENT,
  name_ varchar(255) DEFAULT NULL,
  code bigint(20) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT;

