@echo off
rem Archive the objects of BGI.BAT as BUILD\BGI.LIB.
if not exist BUILD\NUL md BUILD
if exist BUILD\BGI.LIB del BUILD\BGI.LIB
if exist TLIB.LOG del TLIB.LOG
for %%o in (BGIMODES BGIPOINT BGIFB BGIINPUT) do tlib BUILD\BGI.LIB /C /E /P64 +BUILDTC\%%o.OBJ >> TLIB.LOG
find "Error:" TLIB.LOG > NUL
if not errorlevel 1 goto error
del TLIB.LOG
echo BGI library created successfully.
goto end

:error
type TLIB.LOG
del TLIB.LOG
echo Library build failed. See the output above.

:end
