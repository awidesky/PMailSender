@ECHO OFF

set /p t=title : 
set /p c=content : 

:start

java -jar P.jar "%t%" "%c%"


if %errorlevel% == 1 (
     pause
     goto start
)