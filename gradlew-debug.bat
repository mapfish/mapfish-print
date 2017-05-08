@echo off

set GRADLE_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=23111

set CMD_LINE_ARGS=

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
./gradlew %CMD_LINE_ARGS%
