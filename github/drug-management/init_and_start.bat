@echo off

REM 重新初始化数据库
echo 正在重新初始化数据库...
mysql -u root -p < src\main\resources\db\init.sql

REM 启动后端服务
echo 正在启动后端服务...
mvn spring-boot:run

pause