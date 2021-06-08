@ECHO OFF

:start

java -jar P.jar


if %errorlevel% == 1 (
     pause
     goto start
)
