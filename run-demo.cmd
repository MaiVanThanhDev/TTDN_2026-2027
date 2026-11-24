@echo off
REM Chay demo voi CSDL H2 (khong can MySQL). Mo http://localhost:8080
REM Tai khoan demo: admin / librarian / reader - mat khau = DEMO_PASSWORD
if "%DEMO_PASSWORD%"=="" set DEMO_PASSWORD=Demo-password-123
echo Mat khau demo: %DEMO_PASSWORD%
call "%~dp0mvnw.cmd" -B spring-boot:run -Dspring-boot.run.profiles=demo,h2
