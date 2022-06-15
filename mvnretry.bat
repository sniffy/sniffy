@echo off
setlocal enabledelayedexpansion
set max_retries=3
set retry_count=0
set output_file=%date:/=%%time::=%
set output_file=%output_file: =0%
set output_file=%temp%\mvn%output_file:.=%.out
set mvn_command=call mvn %*
set tee_found=true
where /q tee
if not errorlevel 1 goto retry
  set tee_found=false
  echo tee.exe not found in system path^^! Build will continue but output will be delayed...
:retry
  echo %mvn_command%
  if %tee_found%==true (
    %mvn_command% | tee %output_file%
  ) else (
    %mvn_command% > %output_file%
    type %output_file%
  )
  echo Parsing output...
  set "resume_from="
  for /f "tokens=2 delims=:" %%i in ('type %output_file% ^| find "mvn <goals> -rf"') do (
    set resume_from=%%i
  )
  if !retry_count! LSS %max_retries% if not [%resume_from%] == [] (
    echo Resuming from %resume_from%...
    set /a retry_count=retry_count+1
    set /a retries_remaining=max_retries-retry_count
    echo Retrying... [retries used: !retry_count!, retries remaining: !retries_remaining!]
    set mvn_command=call mvn -rf :%resume_from% %*
    goto retry
  )
del /q %output_file%
endlocal