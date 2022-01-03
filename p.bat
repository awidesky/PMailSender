@ECHO OFF

:start

if "%~1"=="" (
  java -jar P.jar
) else (
  java -jar P.jar -files %*
)

if %errorlevel% == 1 (
     pause
     goto start
)
