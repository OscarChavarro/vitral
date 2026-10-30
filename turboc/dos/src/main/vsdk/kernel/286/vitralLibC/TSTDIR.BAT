@echo off
rem Hosted ABI probe with a fake directory backend; no guest disk writes.
echo -ml > TEST.RSP
echo -IC:\TURBOC\INCLUDE;.. >> TEST.RSP
echo -LC:\TURBOC\LIB >> TEST.RSP
echo -eBUILD\DIRTEST.EXE >> TEST.RSP
echo DIRTEST.CPP >> TEST.RSP
echo BUILD\DIRS.OBJ >> TEST.RSP
tcc @TEST.RSP
if errorlevel 1 goto error
BUILD\DIRTEST.EXE
if errorlevel 1 goto error
echo Directory ABI checks passed.
goto end
:error
echo Directory ABI checks FAILED.
:end
