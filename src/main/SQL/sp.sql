CREATE DEFINER=`root`@`127.0.0.1` PROCEDURE `update_password`(IN `username` VARCHAR(50), IN `newpassword` VARCHAR(50), OUT `rowsAffected` INT)
	LANGUAGE SQL
	NOT DETERMINISTIC
	CONTAINS SQL
	SQL SECURITY DEFINER
	COMMENT ''
BEGIN
	update accounts set password1=newpassword where name1=username;
	set rowsAffected = row_count();
END