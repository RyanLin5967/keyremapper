@echo off
call mvn clean package
call java -jar target/keyremapper-1.0-SNAPSHOT.jar