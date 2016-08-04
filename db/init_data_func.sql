
DROP PROCEDURE  IF EXISTS myFunction;
CREATE PROCEDURE myFunction()
begin
declare idx int default 1;
while (idx<4000000) do
	set idx=idx +1;
	INSERT INTO sam_test(name_, user_id, sharding) VALUES (CONCAT('hostM1_',idx), CONCAT('10001',idx), '2');
end while;
end
//


call myFunction();