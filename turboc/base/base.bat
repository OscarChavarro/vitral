@echo off
if not exist BUILD md BUILD
if not exist BUILDTC md BUILDTC

echo [1/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\VSDK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VSDK.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\VSDK.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [2/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\CPP98SUP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CPP98SUP.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\CPP98SUP.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [3/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\ENTITY.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ENTITY.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\ENTITY.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [4/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\ENTCONAC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ENTCONAC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\ENTCONAC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [5/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\EDITOR\CONTSPEC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CONTSPEC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\EDITOR\CONTSPEC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [6/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\EDITOR\GENEEDIT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GENEEDIT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\EDITOR\GENEEDIT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [7/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\LOGGING\LOGGER.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LOGGER.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\LOGGING\LOGGER.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [8/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\STATISTI\RAYTSTAT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYTSTAT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\STATISTI\RAYTSTAT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [9/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\STATISTI\SOLTEXST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SOLTEXST.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\STATISTI\SOLTEXST.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [10/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\STATISTI\GEOMSTAT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GEOMSTAT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\STATISTI\GEOMSTAT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [11/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\COLOR\COLORRGB.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COLORRGB.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\COLOR\COLORRGB.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [12/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\COMPLEX.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COMPLEX.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\COMPLEX.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [13/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\MATRINXM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MATRINXM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\MATRINXM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [14/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\MATR4X4D.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MATR4X4D.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\MATR4X4D.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [15/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\MATR4X4F.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MATR4X4F.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\MATR4X4F.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [16/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\ALGEPRIM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ALGEPRIM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\ALGEPRIM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [17/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\PROCESSI\LINALGEN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LINALGEN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\LINEALGE\PROCESSI\LINALGEN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [18/283] Compiling SRC\MAIN\VSDK\TOOL\PROCESSI\COMPGEOM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COMPGEOM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\PROCESSI\COMPGEOM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [19/283] Compiling SRC\MAIN\VSDK\TOOL\NUMEANAL\LOOUPTAB\LOUPTAC1.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LOUPTAC1.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\NUMEANAL\LOOUPTAB\LOUPTAC1.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [20/283] Compiling SRC\MAIN\VSDK\TOOL\NUMEANAL\LOOUPTAB\LOUPTASI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LOUPTASI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\NUMEANAL\LOOUPTAB\LOUPTASI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [21/283] Compiling SRC\MAIN\VSDK\TOOL\NUMEANAL\POLYNOMI\POLYSOLV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POLYSOLV.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\NUMEANAL\POLYNOMI\POLYSOLV.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [22/283] Compiling SRC\MAIN\VSDK\TOOL\NUMEANAL\POLYNOMI\CUBISOLV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CUBISOLV.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\NUMEANAL\POLYNOMI\CUBISOLV.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [23/283] Compiling SRC\MAIN\VSDK\TOOL\NUMEANAL\POLYNOMI\QUADSOLV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\QUADSOLV.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\NUMEANAL\POLYNOMI\QUADSOLV.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [24/283] Compiling SRC\MAIN\VSDK\TOOL\NUMEANAL\POLYNOMI\QUARSOLV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\QUARSOLV.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\NUMEANAL\POLYNOMI\QUARSOLV.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [25/283] Compiling SRC\MAIN\JAVA\LANG\OBJECT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\OBJECT.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\OBJECT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [26/283] Compiling SRC\MAIN\JAVA\LANG\MATH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MATH.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\MATH.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [27/283] Compiling SRC\MAIN\JAVA\LANG\INTECONS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INTECONS.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\INTECONS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [28/283] Compiling SRC\MAIN\JAVA\LANG\STRING.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\STRING.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\STRING.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [29/283] Compiling SRC\MAIN\JAVA\LANG\SYSTEM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SYSTEM.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\SYSTEM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [30/283] Compiling SRC\MAIN\JAVA\LANG\CHARACTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CHARACTE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\CHARACTE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [31/283] Compiling SRC\MAIN\JAVA\LANG\FLOAT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FLOAT.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\FLOAT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [32/283] Compiling SRC\MAIN\JAVA\LANG\DOUBLE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DOUBLE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\DOUBLE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [33/283] Compiling SRC\MAIN\JAVA\LANG\STRIBUIL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\STRIBUIL.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\STRIBUIL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [34/283] Compiling SRC\MAIN\JAVA\LANG\PROCBUIL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PROCBUIL.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\PROCBUIL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [35/283] Compiling SRC\MAIN\JAVA\LANG\THREAD.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\THREAD.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\THREAD.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [36/283] Compiling SRC\MAIN\JAVA\IO\INPUSTRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INPUSTRE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\INPUSTRE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [37/283] Compiling SRC\MAIN\JAVA\IO\FILE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\FILE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [38/283] Compiling SRC\MAIN\JAVA\IO\FILINPST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILINPST.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\FILINPST.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [39/283] Compiling SRC\MAIN\JAVA\IO\FILOUTST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILOUTST.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\FILOUTST.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [40/283] Compiling SRC\MAIN\JAVA\IO\BUFINPST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BUFINPST.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\BUFINPST.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [41/283] Compiling SRC\MAIN\JAVA\IO\BUFOUTST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BUFOUTST.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\BUFOUTST.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [42/283] Compiling SRC\MAIN\JAVA\IO\PRINSTRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRINSTRE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\PRINSTRE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [43/283] Compiling SRC\MAIN\JAVA\IO\BUFFREAD.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BUFFREAD.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\BUFFREAD.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [44/283] Compiling SRC\MAIN\JAVA\IO\PRINWRIT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRINWRIT.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\PRINWRIT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [45/283] Compiling SRC\MAIN\JAVA\IO\READER.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\READER.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\READER.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [46/283] Compiling SRC\MAIN\JAVA\IO\INPSTRRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INPSTRRE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\INPSTRRE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [47/283] Compiling SRC\MAIN\JAVA\IO\FILEREAD.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILEREAD.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\FILEREAD.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [48/283] Compiling SRC\MAIN\JAVA\IO\STRETOKE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\STRETOKE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\STRETOKE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [49/283] Compiling SRC\MAIN\JAVA\NIO\FILE\FILES.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILES.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\NIO\FILE\FILES.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [50/283] Compiling SRC\MAIN\JAVA\UTIL\FORMATTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FORMATTE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\UTIL\FORMATTE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [51/283] Compiling SRC\MAIN\JAVA\UTIL\STRITOKE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\STRITOKE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\UTIL\STRITOKE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [52/283] Compiling SRC\MAIN\JAVA\UTIL\CONCURRE\EXECUTOR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\EXECUTOR.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\UTIL\CONCURRE\EXECUTOR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [53/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\KEYEVENT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\KEYEVENT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\KEYEVENT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [54/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\MOUSEVEN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MOUSEVEN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\MOUSEVEN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [55/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\LIGIOMBI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LIGIOMBI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\LIGIOMBI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [56/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\INFPLAGI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INFPLAGI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\INFPLAGI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [57/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\RAYGIZMO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYGIZMO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\RAYGIZMO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [58/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\RAGIINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAGIINTE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\RAGIINTE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [59/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\TANGINTE\TANINTEV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TANINTEV.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\TANGINTE\TANINTEV.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [60/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\TANGINTE\TIE2IPGM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TIE2IPGM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\TANGINTE\TIE2IPGM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [61/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\TANGINTE\TAIE2RGM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TAIE2RGM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\TANGINTE\TAIE2RGM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [62/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\RENCONCO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RENCONCO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\RENCONCO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [63/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PROMONCO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PROMONCO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PROMONCO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [64/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PRMOCOLF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRMOCOLF.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PRMOCOLF.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [65/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PRMOINRA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRMOINRA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PRMOINRA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [66/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PARALLEL\PAPRMOPR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PAPRMOPR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PARALLEL\PAPRMOPR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [67/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PARALLEL\PAPRMOC1.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PAPRMOC1.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PARALLEL\PAPRMOC1.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [68/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\CAMERA\CAMERA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAMERA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\CAMERA\CAMERA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [69/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\BACKGROU\SIMPBACK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMPBACK.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\BACKGROU\SIMPBACK.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [70/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\LIGHT\LIGHT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LIGHT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\LIGHT\LIGHT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [71/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\LIGHT\AMBILIGH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AMBILIGH.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\LIGHT\AMBILIGH.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [72/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\LIGHT\DIRELIGH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DIRELIGH.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\LIGHT\DIRELIGH.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [73/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\LIGHT\POINLIGH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POINLIGH.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\LIGHT\POINLIGH.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [74/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\LIGHT\SPOTLIGH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SPOTLIGH.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\LIGHT\SPOTLIGH.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [75/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\SCENE\SIMPSCEN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMPSCEN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\SCENE\SIMPSCEN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [76/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\SCENE\SIMSCESN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMSCESN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\SCENE\SIMSCESN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [77/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\SCENE\SIMBODGR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMBODGR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\SCENE\SIMBODGR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [78/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\SCENE\SIMPBODY.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMPBODY.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\SCENE\SIMPBODY.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [79/283] Compiling SRC\MAIN\VSDK\TOOL\PROCESSI\STOPWATC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\STOPWATC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\PROCESSI\STOPWATC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [80/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\SYMBALGE\ALGEEXPR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ALGEEXPR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\SYMBALGE\ALGEEXPR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [81/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\ELEMENT\RAY.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAY.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\ELEMENT\RAY.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [82/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\ELEMENT\RAYHIT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYHIT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\ELEMENT\RAYHIT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [83/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\ELEMENT\TRIANGLE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRIANGLE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\ELEMENT\TRIANGLE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [84/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\ELEMENT\VERTEX.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VERTEX.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\ELEMENT\VERTEX.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [85/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\ELEMENT\VERTEX2D.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VERTEX2D.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\ELEMENT\VERTEX2D.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [86/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\CURVE\CURVE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CURVE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\CURVE\CURVE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [87/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\CURVE\PARACURV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PARACURV.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\CURVE\PARACURV.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [88/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEOMETRY.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GEOMETRY.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEOMETRY.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [89/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\SURFACE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SURFACE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\SURFACE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [90/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\HALFSPAC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\HALFSPAC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\HALFSPAC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [91/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\INFIPLAN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INFIPLAN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\INFIPLAN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [92/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\FUNEXPSU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FUNEXPSU.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\FUNEXPSU.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [93/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\MD2MESH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MD2MESH.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\MD2MESH.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [94/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\PABICUPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PABICUPA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\PABICUPA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [95/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\_PABICPI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PABICPI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\_PABICPI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [96/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\TRISTRME.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRISTRME.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\TRISTRME.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [97/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\TRIAMESH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRIAMESH.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\TRIAMESH.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [98/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\QUADMESH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\QUADMESH.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\QUADMESH.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [99/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\TRIMESGR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRIMESGR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\TRIMESGR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [100/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\VOXELIZA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VOXELIZA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\VOXELIZA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [101/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\TRIMESVO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRIMESVO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\TRIMESVO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [102/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\TRMEGRVO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRMEGRVO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\TRMEGRVO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [103/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\FUEXSUVO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FUEXSUVO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\FUEXSUVO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [104/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\SURRAYIN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SURRAYIN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\SURRAYIN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [105/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\GEOMTRIA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GEOMTRIA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\GEOMTRIA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [106/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDECTR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MONDECTR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDECTR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [107/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_COAWPOT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_COAWPOT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_COAWPOT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [108/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_CONSTRU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_CONSTRU.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_CONSTRU.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [109/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_INCSEIN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_INCSEIN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_INCSEIN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [110/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_INSBASC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_INSBASC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_INSBASC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [111/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_MONOTON.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_MONOTON.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_MONOTON.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [112/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_RANSEOR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_RANSEOR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_RANSEOR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [113/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_SEGTABU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_SEGTABU.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_SEGTABU.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [114/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_TRIATRA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_TRIATRA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_TRIATRA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [115/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_TRTRQUN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_TRTRQUN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLTR\MONDE\_TRTRQUN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [116/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLYCLIP\_POL2DWA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POL2DWA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLYCLIP\_POL2DWA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [117/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLYCLIP\POLYPROC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POLYPROC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLYCLIP\POLYPROC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [118/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLYCLIP\POLTOPME.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POLTOPME.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLYCLIP\POLTOPME.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [119/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLYCLIP\WEATPOCL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WEATPOCL.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\POLYCLIP\WEATPOCL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [120/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\POLYGON\_POL2DCO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POL2DCO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\POLYGON\_POL2DCO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [121/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\POLYGON\POLYGO2D.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POLYGO2D.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\SURFACE\POLYGON\POLYGO2D.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [122/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\VOLUME.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VOLUME.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\VOLUME.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [123/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\SOLID.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SOLID.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\SOLID.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [124/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\SPHERE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SPHERE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\SPHERE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [125/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\BOX.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BOX.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\BOX.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [126/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\CONE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CONE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\CONE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [127/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\ARROW.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ARROW.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\ARROW.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [128/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\TORUS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TORUS.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\TORUS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [129/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\VOXEVOLU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VOXEVOLU.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\VOXEVOLU.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [130/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POLBOUSO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POLBOUSO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POLBOUSO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [131/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOEO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOEO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOEO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [132/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSONP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSONP.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSONP.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [133/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOPR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOPR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOPR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [134/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOGV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOGV.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOGV.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [135/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOVE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOVE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOVE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [136/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOTE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOTE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [137/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\_POBOSTV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSTV.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\_POBOSTV.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [138/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\_POBSBTP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBSBTP.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\_POBSBTP.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [139/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\_POBOSVS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSVS.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\_POBOSVS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [140/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOTS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOTS.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\POBOSOTS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [141/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSOE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [142/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSHE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSHE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSHE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [143/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSOL.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [144/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSOF.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOF.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [145/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\NODE\_POBSNTS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBSNTS.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\VOLUM\POBS\NODE\_POBSNTS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [146/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\CAMCONAQ.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAMCONAQ.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\CAMCONAQ.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [147/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\CAMCONOR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAMCONOR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\CAMCONOR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [148/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\CAMCONBL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAMCONBL.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\CAMCONBL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [149/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\CACOGOEA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CACOGOEA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\CACOGOEA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [150/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\TRACWORK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRACWORK.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\TRACWORK.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [152/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\RAYTRACI\DEPBUFEN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DEPBUFEN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\RAYTRACI\DEPBUFEN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [153/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\RAYTRACI\PARARAYT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PARARAYT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\RAYTRACI\PARARAYT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [154/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\RAYTRACI\RASTILAR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RASTILAR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\RAYTRACI\RASTILAR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [155/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\RAYTRACI\RASTILGE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RASTILGE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\RAYTRACI\RASTILGE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [156/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\RAYTRACI\SIMPRAYT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMPRAYT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\RAYTRACI\SIMPRAYT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [157/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\AUTSTEGE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AUTSTEGE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\AUTSTEGE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [158/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\HIDDLINE\HIDLINRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\HIDLINRE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\HIDDLINE\HIDLINRE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [159/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\RASTER\RASTER2D.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RASTER2D.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\RASTER\RASTER2D.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [160/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\HIDDLINE\WIREREND.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIREREND.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\HIDDLINE\WIREREND.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [161/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\POBOSODE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSODE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\POBOSODE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [162/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\CPTESACO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CPTESACO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\CPTESACO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [163/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\LIGHSHAD.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LIGHSHAD.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\LIGHSHAD.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [164/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\CONSSHAD.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CONSSHAD.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\CONSSHAD.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [165/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\CONTEXSH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CONTEXSH.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\CONTEXSH.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [166/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\COOTORSH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COOTORSH.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\COOTORSH.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [167/283] Compiling SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\SHADSELE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SHADSELE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\RENDER\SHADERS\SHADSELE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [168/283] Compiling SRC\MAIN\VSDK\TOOL\IO\PERSELEM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PERSELEM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\PERSELEM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [169/283] Compiling SRC\MAIN\VSDK\TOOL\IO\FILELOCA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILELOCA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\FILELOCA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [170/283] Compiling SRC\MAIN\VSDK\TOOL\IO\GUI\DIALPERS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DIALPERS.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\GUI\DIALPERS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [171/283] Compiling SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\ENVIPERS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ENVIPERS.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\ENVIPERS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [172/283] Compiling SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\WRITEOBJ.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WRITEOBJ.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\WRITEOBJ.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [173/283] Compiling SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\WRITEGTS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WRITEGTS.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\WRITEGTS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [174/283] Compiling SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\WRITEVTK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WRITEVTK.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\WRITEVTK.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [175/283] Compiling SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\READEOBJ.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\READEOBJ.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\READEOBJ.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [176/283] Compiling SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\READEPLY.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\READEPLY.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\READEPLY.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [177/283] Compiling SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\REAPLYRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\REAPLYRE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\REAPLYRE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [178/283] Compiling SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\REAMITSC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\REAMITSC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\REAMITSC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [179/283] Compiling SRC\MAIN\VSDK\TOOL\IO\IMAGE\IMAGPERS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\IMAGPERS.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\IMAGE\IMAGPERS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [180/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\TEXTUTIL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TEXTUTIL.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\TEXTUTIL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [181/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\FROM2D\CORGIMHU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CORGIMHU.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\FROM2D\CORGIMHU.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [182/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\FROM2D\IMAGTEXT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\IMAGTEXT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\FROM2D\IMAGTEXT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [183/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\FROM2D\SOTECOMA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SOTECOMA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\FROM2D\SOTECOMA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [184/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\PROCEDUR\BUMTEXFI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BUMTEXFI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\PROCEDUR\BUMTEXFI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [185/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\PROCEDUR\COLTEXFI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COLTEXFI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\PROCEDUR\COLTEXFI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [186/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\PROCEDUR\PROCNOIS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PROCNOIS.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\SOLITEXT\PROCEDUR\PROCNOIS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [187/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\RGBPIXEL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBPIXEL.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\RGBPIXEL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [188/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\RGBAPIXE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBAPIXE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\RGBAPIXE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [189/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\SHAPDESC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SHAPDESC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\SHAPDESC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [190/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\RGBCOLP1.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBCOLP1.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\RGBCOLP1.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [191/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\RGBCOLPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBCOLPA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\RGBCOLPA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [192/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\RGPRCOPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGPRCOPA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\RGPRCOPA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [193/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\GRASCAPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GRASCAPA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\GRASCAPA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [194/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\IMAGE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\IMAGE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\IMAGE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [195/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\RGBIMAU1.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBIMAU1.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\RGBIMAU1.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [196/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\RGBIMAUN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBIMAUN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\RGBIMAUN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [197/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\RGIMHDUN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGIMHDUN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\RGIMHDUN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [198/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\INCOIMUN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INCOIMUN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\INCOIMUN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [199/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\INCOIMHU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INCOIMHU.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\INCOIMHU.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [200/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\RGBIMACO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBIMACO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\RGBIMACO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [201/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\ZBUFFER.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ZBUFFER.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\ZBUFFER.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [202/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\NORMAMAP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\NORMAMAP.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\NORMAMAP.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [203/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\CAL2DBUF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAL2DBUF.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\CAL2DBUF.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [204/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\PRCOSHDE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRCOSHDE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\PRCOSHDE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [205/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\FOUSHADE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FOUSHADE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\FOUSHADE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [206/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\GEOMMETA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GEOMMETA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\GEOMMETA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [207/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\MATERIAL\SIMPMATE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMPMATE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\MATERIAL\SIMPMATE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [208/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\MATERIAL\MICFACMA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MICFACMA.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\MATERIAL\MICFACMA.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [209/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\MATERIAL\SHADTYPE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SHADTYPE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\MATERIAL\SHADTYPE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [210/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\MATERIAL\RENDCONF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RENDCONF.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\MATERIAL\RENDCONF.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [211/283] Compiling SRC\MAIN\VSDK\TOOL\PROCESSI\CURVMODE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CURVMODE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\PROCESSI\CURVMODE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [212/283] Compiling SRC\MAIN\VSDK\TOOL\PROCESSI\IMAGPROC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\IMAGPROC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\PROCESSI\IMAGPROC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [213/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\BACKGROU\CUBEBACK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CUBEBACK.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\BACKGROU\CUBEBACK.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [214/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\BACKGROU\FIXEBACK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FIXEBACK.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\BACKGROU\FIXEBACK.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [215/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\POBOSOMO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOMO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\POBOSOMO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [216/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\_POBOSOO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSOO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\_POBOSOO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [217/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FALLBA\_POBOSIN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSIN.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FALLBA\_POBOSIN.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [218/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FALLBA\_POBOSFG.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSFG.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FALLBA\_POBOSFG.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [219/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FALLBA\_PBSAACF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSAACF.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FALLBA\_PBSAACF.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [220/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FALLBA\_POBSOCF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBSOCF.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FALLBA\_POBSOCF.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [221/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\CONSTR\_POBSPDF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBSPDF.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\CONSTR\_POBSPDF.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [222/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\CONSTR\_POBSOPF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBSOPF.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\CONSTR\_POBSOPF.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [223/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\SLICIN\_POBOSOS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSOS.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\SLICIN\_POBOSOS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [224/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\_SETOPTR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_SETOPTR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\_SETOPTR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [225/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\TOP\_PBSSONE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSONE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\TOP\_PBSSONE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [226/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\TOP\_PBSSNEC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSNEC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\TOP\_PBSSNEC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [227/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\TOP\_POBOSSF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSSF.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\TOP\_POBOSSF.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [228/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\INT\_PBSSICB.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSICB.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\INT\_PBSSICB.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [229/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\INT\_POBOSSI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSSI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\INT\_POBOSSI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [230/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\_POBOSSO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSSO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\_POBOSSO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [231/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\INT\_PBSSGPP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSGPP.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\INT\_PBSSGPP.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [232/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSOSC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSOSC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSOSC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [233/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSVFC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSVFC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSVFC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [234/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSVVC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSVVC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSVVC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [235/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_POBOSSC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSSC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_POBOSSC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [236/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSNIC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSNIC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSNIC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [237/283] Compiling SRC\MAIN\VSDK\TOOL\COMMON\STATISTI\POBOSOST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOST.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\COMMON\STATISTI\POBOSOST.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [238/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FIXTUR\SITEGELI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SITEGELI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FIXTUR\SITEGELI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [239/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FIXTUR\CSKUBOFI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSKUBOFI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\FIXTUR\CSKUBOFI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [240/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\VALIDA\_POBOSFV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSFV.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\GEOM\GEP\PSO\VALIDA\_POBOSFV.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [241/283] Compiling SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\PARCURPE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PARCURPE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\PARCURPE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [242/283] Compiling SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\PABICUPP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PABICUPP.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\GEOMETRY\PABICUPP.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [243/283] Compiling SRC\MAIN\VSDK\TOOL\IO\IMAGE\RGCOPAPE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGCOPAPE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\IO\IMAGE\RGCOPAPE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [244/283] Compiling SRC\MAIN\ORG\W3C\DOM\NODE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\NODE.OBJ >> TC.RSP
echo SRC\MAIN\ORG\W3C\DOM\NODE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [245/283] Compiling SRC\MAIN\JAVAX\XML\PARSERS\DOCUBUIL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DOCUBUIL.OBJ >> TC.RSP
echo SRC\MAIN\JAVAX\XML\PARSERS\DOCUBUIL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [246/283] Compiling SRC\MAIN\JAVAX\XML\TRANSFOR\TRANSFOR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRANSFOR.OBJ >> TC.RSP
echo SRC\MAIN\JAVAX\XML\TRANSFOR\TRANSFOR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [247/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDGET.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGET.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDGET.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [248/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDBUTGR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDBUTGR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDBUTGR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [249/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDGCOMM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGCOMM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDGCOMM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [250/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDGDIAL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGDIAL.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDGDIAL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [251/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDDIAEL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDDIAEL.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDDIAEL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [252/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\DIALOG\COMPONEN\DIAFILFI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DIAFILFI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\DIALOG\COMPONEN\DIAFILFI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [253/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\DIALOG\COMPONEN\DIATRENO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DIATRENO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\DIALOG\COMPONEN\DIATRENO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [254/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\DIALOG\RUNTIME\DIALSESS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DIALSESS.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\DIALOG\RUNTIME\DIALSESS.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [255/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDGMENU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGMENU.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDGMENU.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [256/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDMENEL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDMENEL.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDMENEL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [257/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDMENIT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDMENIT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDMENIT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [258/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\VIEWPORT\VIEWPORT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VIEWPORT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\VIEWPORT\VIEWPORT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [259/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\VIEWPORT\VIEELESC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VIEELESC.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\VIEWPORT\VIEELESC.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [260/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\VIEWPORT\VIEWPSET.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VIEWPSET.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\VIEWPORT\VIEWPSET.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [261/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\VIEWPORT\VISEINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VISEINTE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\VIEWPORT\VISEINTE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [262/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\VIEWPORT\VIEINTTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VIEINTTE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\VIEWPORT\VIEINTTE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [263/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\GIZSOLTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GIZSOLTE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\GIZSOLTE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [264/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\GIVEARBU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GIVEARBU.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\GIVEARBU.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [265/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\INPUGIZM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INPUGIZM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\INPUGIZM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [266/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\INGIVACR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INGIVACR.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\INGIVACR.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [267/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\REFFRAGI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\REFFRAGI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\REFFRAGI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [268/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\TRANGIZM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRANGIZM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\TRANGIZM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [269/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\TRGIINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRGIINTE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\TRGIINTE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [270/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\ROTAGIZM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ROTAGIZM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\ROTAGIZM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [271/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\ROGIINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ROGIINTE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\ROGIINTE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [272/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\SCALGIZM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SCALGIZM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\SCALGIZM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [273/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\SCGIINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SCGIINTE.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\SCGIINTE.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [274/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\PRESELEM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRESELEM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\PRESELEM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [275/283] Compiling SRC\MAIN\VSDK\TOOL\ENVI\BACKGROU\BACKGROU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BACKGROU.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\ENVI\BACKGROU\BACKGROU.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [276/283] Compiling SRC\MAIN\VSDK\TOOL\MEDIA\MEDIENTI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MEDIENTI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\MEDIA\MEDIENTI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [277/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\CAMECONT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAMECONT.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\CAMECONT.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [278/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\COMMLIST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COMMLIST.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\COMMLIST.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [279/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\CONTROLL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CONTROLL.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\CONTROLL.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [280/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\GIZMO\GIZMO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GIZMO.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\GIZMO\GIZMO.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [281/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PROGMONI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PROGMONI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\FEEDBACK\PROGMONI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [282/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDGELEM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGELEM.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\WIDGET\WIDGELEM.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo [283/283] Compiling SRC\MAIN\VSDK\TOOL\GUI\WIDGET\VARIABLE\WIDGVARI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGVARI.OBJ >> TC.RSP
echo SRC\MAIN\VSDK\TOOL\GUI\WIDGET\VARIABLE\WIDGVARI.CPP >> TC.RSP
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error

echo Base objects compiled successfully.
goto end
:error
type TC.LOG
echo Build failed. See the compiler output above.
:end

