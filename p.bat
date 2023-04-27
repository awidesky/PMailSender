@ECHO OFF

:start

if "%~1"=="" (
  java -jar PMailSender.jar
) else (
  java -jar PMailSender.jar -files %*
)

if %errorlevel% == 1 (
     pause
     goto start
)
