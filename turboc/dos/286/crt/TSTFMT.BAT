@echo off
rem Hosted real-mode ABI probe, not the protected application link.
echo -ml > TEST.RSP
echo -f87 >> TEST.RSP
echo -IC:\TURBOC\INCLUDE >> TEST.RSP
echo -LC:\TURBOC\LIB >> TEST.RSP
echo -eBUILD\FMTTEST.EXE >> TEST.RSP
echo FMTTEST.CPP >> TEST.RSP
echo BUILD\FORMAT.OBJ >> TEST.RSP
tcc @TEST.RSP
if errorlevel 1 goto error
BUILD\FMTTEST.EXE
if errorlevel 1 goto error
echo Formatter ABI checks passed.
goto end
:error
echo Formatter ABI checks FAILED.
:end
