-- ----------------------------
-- Table customer
-- ----------------------------
INSERT INTO customer (user_id, name, sharding) VALUES ('db1kxufiiwjd238hf832dff1', 'db1_张三', '1');
INSERT INTO customer (user_id, name, sharding) VALUES ('db1kxufiiwjd238hf832dff2', 'db1_李四', '1');
INSERT INTO customer (user_id, name, sharding) VALUES ('db1kxufiiwjd238hf832dff3', 'db1_王五', '1');

INSERT INTO customer (user_id, name, sharding) VALUES ('db2kxufiiwjd238hf832df1', 'db2_吴京', '2');
INSERT INTO customer (user_id, name, sharding) VALUES ('db2kxufiiwjd238hf832dff2', 'db2_刘烨', '2');
INSERT INTO customer (user_id, name, sharding) VALUES ('db2kxufiiwjd238hf832dff3', 'db2_六爷', '2');

-- ----------------------------
-- Table sam_test
-- ----------------------------
INSERT INTO sam_test VALUES ('1', 'hostM1_1', '123456', '1');
INSERT INTO sam_test VALUES ('2', 'hostM1_2', '123456789', '2');
INSERT INTO sam_test(name_, user_id, sharding) VALUES ('hostM1_3', '12345678910', '3');
INSERT INTO sam_test (name_, user_id, sharding) VALUES ('hostM1_17', '1000117', '2');
INSERT INTO sam_test (name_, user_id, sharding) VALUES ('hostM1_18', '1000118', '2');
INSERT INTO sam_test (name_, user_id, sharding) VALUES ('hostM1_19', '1000119', '2');
INSERT INTO sam_test (name_, user_id, sharding) VALUES ('hostM1_20', '1000120', '2');
INSERT INTO sam_test (name_, user_id, sharding) VALUES ('hostM1_21', '1000121', '2');
INSERT INTO sam_test (name_, user_id, sharding) VALUES ('hostM1_22', '1000122', '2');
INSERT INTO sam_test (name_, user_id, sharding) VALUES ('hostM1_22', '1000123', '2');

