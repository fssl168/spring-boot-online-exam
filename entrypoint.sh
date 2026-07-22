#!/bin/sh
# 等待 MySQL 就绪
echo "等待 MySQL 启动..."
while ! nc -z mysql 3306 2>/dev/null; do
  sleep 1
done
echo "MySQL 已就绪，启动后端..."
# 启动 Spring Boot
exec java -jar app.jar --spring.profiles.active=prod
