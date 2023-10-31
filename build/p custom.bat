@ECHO OFF

set /p t=title : 
set /p c=content : 

:start

if "%~1"=="" (
  java -jar PMailSender.jar "-title=%t%" "-content=%c%"
) else (
  java -jar PMailSender.jar "-title=%t%" "-content=%c%" -files %*
)

if %errorlevel% == 1 (
     pause
     goto start
)