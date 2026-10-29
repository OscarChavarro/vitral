@echo off
rem Hosted real-mode ABI probes only; not the application link.
echo -ml > TEST.RSP
echo -IC:\TURBOC\INCLUDE;.. >> TEST.RSP
echo -LC:\TURBOC\LIB >> TEST.RSP
echo -eBUILD\VECTEST.EXE >> TEST.RSP
echo VECTEST.CPP >> TEST.RSP
echo BUILD\VECTOR.OBJ >> TEST.RSP
tcc @TEST.RSP
if errorlevel 1 goto error
BUILD\VECTEST.EXE
if errorlevel 1 goto error
echo Vector ABI checks passed.
echo -ml > TEST.RSP
echo -IC:\TURBOC\INCLUDE;.. >> TEST.RSP
echo -LC:\TURBOC\LIB >> TEST.RSP
echo -eBUILD\TIMETEST.EXE >> TEST.RSP
echo TIMETEST.CPP >> TEST.RSP
echo BUILD\TIME.OBJ >> TEST.RSP
tcc @TEST.RSP
if errorlevel 1 goto error
BUILD\TIMETEST.EXE
if errorlevel 1 goto error
echo RTC conversion checks passed.
goto end
:error
echo Runtime ABI checks FAILED.
:end
