@ECHO OFF
SETLOCAL

SET APP_HOME=%~dp0
IF EXIST "%USERPROFILE%\.gradle\wrapper\dists\gradle-8.14-bin" (
  FOR /D %%G IN ("%USERPROFILE%\.gradle\wrapper\dists\gradle-8.14-bin\*") DO (
    IF EXIST "%%G\gradle-8.14\bin\gradle.bat" (
      CALL "%%G\gradle-8.14\bin\gradle.bat" -p "%APP_HOME%" %*
      EXIT /B %ERRORLEVEL%
    )
  )
)
IF EXIST "%USERPROFILE%\.gradle\wrapper\dists\gradle-8.13-bin" (
  FOR /D %%G IN ("%USERPROFILE%\.gradle\wrapper\dists\gradle-8.13-bin\*") DO (
    IF EXIST "%%G\gradle-8.13\bin\gradle.bat" (
      CALL "%%G\gradle-8.13\bin\gradle.bat" -p "%APP_HOME%" %*
      EXIT /B %ERRORLEVEL%
    )
  )
)

gradle -p "%APP_HOME%" %*

