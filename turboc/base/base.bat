@echo off
if not exist BUILD\NUL md BUILD
if not exist BUILDTC\NUL md BUILDTC

echo [1/416] Compiling SRC\MAIN\V\T\COMMON\VSDK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VSDK.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\VSDK.CPP >> TC.RSP
if exist BUILDTC\VSDK.OBJ del BUILDTC\VSDK.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VSDK.OBJ goto error

echo [2/416] Compiling SRC\MAIN\V\T\COMMON\CPP98SUP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CPP98SUP.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\CPP98SUP.CPP >> TC.RSP
if exist BUILDTC\CPP98SUP.OBJ del BUILDTC\CPP98SUP.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CPP98SUP.OBJ goto error

echo [3/416] Compiling SRC\MAIN\V\T\COMMON\ENTITY.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ENTITY.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\ENTITY.CPP >> TC.RSP
if exist BUILDTC\ENTITY.OBJ del BUILDTC\ENTITY.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\ENTITY.OBJ goto error

echo [4/416] Compiling SRC\MAIN\V\T\COMMON\ENTCONAC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ENTCONAC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\ENTCONAC.CPP >> TC.RSP
if exist BUILDTC\ENTCONAC.OBJ del BUILDTC\ENTCONAC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\ENTCONAC.OBJ goto error

echo [5/416] Compiling SRC\MAIN\V\T\GUI\EDITOR\CONTSPEC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CONTSPEC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\EDITOR\CONTSPEC.CPP >> TC.RSP
if exist BUILDTC\CONTSPEC.OBJ del BUILDTC\CONTSPEC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CONTSPEC.OBJ goto error

echo [6/416] Compiling SRC\MAIN\V\T\GUI\EDITOR\GENEEDIT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GENEEDIT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\EDITOR\GENEEDIT.CPP >> TC.RSP
if exist BUILDTC\GENEEDIT.OBJ del BUILDTC\GENEEDIT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GENEEDIT.OBJ goto error

echo [7/416] Compiling SRC\MAIN\V\T\COMMON\LOGGING\LOGGER.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LOGGER.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\LOGGING\LOGGER.CPP >> TC.RSP
if exist BUILDTC\LOGGER.OBJ del BUILDTC\LOGGER.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\LOGGER.OBJ goto error

echo [8/416] Compiling SRC\MAIN\V\T\COMMON\STATISTI\RAYTSTAT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYTSTAT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\STATISTI\RAYTSTAT.CPP >> TC.RSP
if exist BUILDTC\RAYTSTAT.OBJ del BUILDTC\RAYTSTAT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYTSTAT.OBJ goto error

echo [9/416] Compiling SRC\MAIN\V\T\COMMON\STATISTI\SOLTEXST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SOLTEXST.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\STATISTI\SOLTEXST.CPP >> TC.RSP
if exist BUILDTC\SOLTEXST.OBJ del BUILDTC\SOLTEXST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SOLTEXST.OBJ goto error

echo [10/416] Compiling SRC\MAIN\V\T\COMMON\STATISTI\GEOMSTAT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GEOMSTAT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\STATISTI\GEOMSTAT.CPP >> TC.RSP
if exist BUILDTC\GEOMSTAT.OBJ del BUILDTC\GEOMSTAT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GEOMSTAT.OBJ goto error

echo [11/416] Compiling SRC\MAIN\V\T\COMMON\COLOR\COLORRGB.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COLORRGB.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\COLOR\COLORRGB.CPP >> TC.RSP
if exist BUILDTC\COLORRGB.OBJ del BUILDTC\COLORRGB.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\COLORRGB.OBJ goto error

echo [12/416] Compiling SRC\MAIN\V\T\COMMON\COLOR\CIE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CIE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\COLOR\CIE.CPP >> TC.RSP
if exist BUILDTC\CIE.OBJ del BUILDTC\CIE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CIE.OBJ goto error

echo [13/416] Compiling SRC\MAIN\V\T\COMMON\LINEALGE\COMPLEX.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COMPLEX.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\LINEALGE\COMPLEX.CPP >> TC.RSP
if exist BUILDTC\COMPLEX.OBJ del BUILDTC\COMPLEX.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\COMPLEX.OBJ goto error

echo [14/416] Compiling SRC\MAIN\V\T\COMMON\LINEALGE\MATRINXM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MATRINXM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\LINEALGE\MATRINXM.CPP >> TC.RSP
if exist BUILDTC\MATRINXM.OBJ del BUILDTC\MATRINXM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\MATRINXM.OBJ goto error

echo [15/416] Compiling SRC\MAIN\V\T\COMMON\LINEALGE\MATR4X4D.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MATR4X4D.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\LINEALGE\MATR4X4D.CPP >> TC.RSP
if exist BUILDTC\MATR4X4D.OBJ del BUILDTC\MATR4X4D.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\MATR4X4D.OBJ goto error

echo [16/416] Compiling SRC\MAIN\V\T\COMMON\LINEALGE\MATR4X4F.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MATR4X4F.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\LINEALGE\MATR4X4F.CPP >> TC.RSP
if exist BUILDTC\MATR4X4F.OBJ del BUILDTC\MATR4X4F.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\MATR4X4F.OBJ goto error

echo [17/416] Compiling SRC\MAIN\V\T\COMMON\LINEALGE\ALGEPRIM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ALGEPRIM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\LINEALGE\ALGEPRIM.CPP >> TC.RSP
if exist BUILDTC\ALGEPRIM.OBJ del BUILDTC\ALGEPRIM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\ALGEPRIM.OBJ goto error

echo [18/416] Compiling SRC\MAIN\V\T\COMMON\LINEALGE\PROCESSI\LINALGEN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LINALGEN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\LINEALGE\PROCESSI\LINALGEN.CPP >> TC.RSP
if exist BUILDTC\LINALGEN.OBJ del BUILDTC\LINALGEN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\LINALGEN.OBJ goto error

echo [19/416] Compiling SRC\MAIN\V\T\PROCESSI\COMPGEOM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COMPGEOM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\PROCESSI\COMPGEOM.CPP >> TC.RSP
if exist BUILDTC\COMPGEOM.OBJ del BUILDTC\COMPGEOM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\COMPGEOM.OBJ goto error

echo [20/416] Compiling SRC\MAIN\V\T\NUMEANAL\LOOUPTAB\LOUPTAC1.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LOUPTAC1.OBJ >> TC.RSP
echo SRC\MAIN\V\T\NUMEANAL\LOOUPTAB\LOUPTAC1.CPP >> TC.RSP
if exist BUILDTC\LOUPTAC1.OBJ del BUILDTC\LOUPTAC1.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\LOUPTAC1.OBJ goto error

echo [21/416] Compiling SRC\MAIN\V\T\NUMEANAL\LOOUPTAB\LOUPTASI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LOUPTASI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\NUMEANAL\LOOUPTAB\LOUPTASI.CPP >> TC.RSP
if exist BUILDTC\LOUPTASI.OBJ del BUILDTC\LOUPTASI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\LOUPTASI.OBJ goto error

echo [22/416] Compiling SRC\MAIN\V\T\NUMEANAL\POLYNOMI\POLYSOLV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POLYSOLV.OBJ >> TC.RSP
echo SRC\MAIN\V\T\NUMEANAL\POLYNOMI\POLYSOLV.CPP >> TC.RSP
if exist BUILDTC\POLYSOLV.OBJ del BUILDTC\POLYSOLV.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POLYSOLV.OBJ goto error

echo [23/416] Compiling SRC\MAIN\V\T\NUMEANAL\POLYNOMI\CUBISOLV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CUBISOLV.OBJ >> TC.RSP
echo SRC\MAIN\V\T\NUMEANAL\POLYNOMI\CUBISOLV.CPP >> TC.RSP
if exist BUILDTC\CUBISOLV.OBJ del BUILDTC\CUBISOLV.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CUBISOLV.OBJ goto error

echo [24/416] Compiling SRC\MAIN\V\T\NUMEANAL\POLYNOMI\QUADSOLV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\QUADSOLV.OBJ >> TC.RSP
echo SRC\MAIN\V\T\NUMEANAL\POLYNOMI\QUADSOLV.CPP >> TC.RSP
if exist BUILDTC\QUADSOLV.OBJ del BUILDTC\QUADSOLV.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\QUADSOLV.OBJ goto error

echo [25/416] Compiling SRC\MAIN\V\T\NUMEANAL\POLYNOMI\QUARSOLV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\QUARSOLV.OBJ >> TC.RSP
echo SRC\MAIN\V\T\NUMEANAL\POLYNOMI\QUARSOLV.CPP >> TC.RSP
if exist BUILDTC\QUARSOLV.OBJ del BUILDTC\QUARSOLV.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\QUARSOLV.OBJ goto error

echo [26/416] Compiling SRC\MAIN\JACKSON\DATABIND\JSONNODE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\JSONNODE.OBJ >> TC.RSP
echo SRC\MAIN\JACKSON\DATABIND\JSONNODE.CPP >> TC.RSP
if exist BUILDTC\JSONNODE.OBJ del BUILDTC\JSONNODE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\JSONNODE.OBJ goto error

echo [27/416] Compiling SRC\MAIN\JACKSON\DATABIND\OBJEMAPP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\OBJEMAPP.OBJ >> TC.RSP
echo SRC\MAIN\JACKSON\DATABIND\OBJEMAPP.CPP >> TC.RSP
if exist BUILDTC\OBJEMAPP.OBJ del BUILDTC\OBJEMAPP.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\OBJEMAPP.OBJ goto error

echo [28/416] Compiling SRC\MAIN\JAVA\LANG\OBJECT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\OBJECT.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\OBJECT.CPP >> TC.RSP
if exist BUILDTC\OBJECT.OBJ del BUILDTC\OBJECT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\OBJECT.OBJ goto error

echo [29/416] Compiling SRC\MAIN\JAVA\LANG\MATH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MATH.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\MATH.CPP >> TC.RSP
if exist BUILDTC\MATH.OBJ del BUILDTC\MATH.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\MATH.OBJ goto error

echo [30/416] Compiling SRC\MAIN\JAVA\LANG\INTECONS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INTECONS.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\INTECONS.CPP >> TC.RSP
if exist BUILDTC\INTECONS.OBJ del BUILDTC\INTECONS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\INTECONS.OBJ goto error

echo [31/416] Compiling SRC\MAIN\JAVA\LANG\STRING.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\STRING.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\STRING.CPP >> TC.RSP
if exist BUILDTC\STRING.OBJ del BUILDTC\STRING.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\STRING.OBJ goto error

echo [32/416] Compiling SRC\MAIN\JAVA\LANG\SYSTEM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SYSTEM.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\SYSTEM.CPP >> TC.RSP
if exist BUILDTC\SYSTEM.OBJ del BUILDTC\SYSTEM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SYSTEM.OBJ goto error

echo [33/416] Compiling SRC\MAIN\JAVA\LANG\CHARACTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CHARACTE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\CHARACTE.CPP >> TC.RSP
if exist BUILDTC\CHARACTE.OBJ del BUILDTC\CHARACTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CHARACTE.OBJ goto error

echo [34/416] Compiling SRC\MAIN\JAVA\LANG\FLOAT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FLOAT.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\FLOAT.CPP >> TC.RSP
if exist BUILDTC\FLOAT.OBJ del BUILDTC\FLOAT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FLOAT.OBJ goto error

echo [35/416] Compiling SRC\MAIN\JAVA\LANG\DOUBLE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DOUBLE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\DOUBLE.CPP >> TC.RSP
if exist BUILDTC\DOUBLE.OBJ del BUILDTC\DOUBLE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\DOUBLE.OBJ goto error

echo [36/416] Compiling SRC\MAIN\JAVA\LANG\STRIBUIL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\STRIBUIL.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\STRIBUIL.CPP >> TC.RSP
if exist BUILDTC\STRIBUIL.OBJ del BUILDTC\STRIBUIL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\STRIBUIL.OBJ goto error

echo [37/416] Compiling SRC\MAIN\JAVA\LANG\PROCBUIL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PROCBUIL.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\PROCBUIL.CPP >> TC.RSP
if exist BUILDTC\PROCBUIL.OBJ del BUILDTC\PROCBUIL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PROCBUIL.OBJ goto error

echo [38/416] Compiling SRC\MAIN\JAVA\LANG\THREAD.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\THREAD.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\LANG\THREAD.CPP >> TC.RSP
if exist BUILDTC\THREAD.OBJ del BUILDTC\THREAD.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\THREAD.OBJ goto error

echo [39/416] Compiling SRC\MAIN\JAVA\IO\INPUSTRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INPUSTRE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\INPUSTRE.CPP >> TC.RSP
if exist BUILDTC\INPUSTRE.OBJ del BUILDTC\INPUSTRE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\INPUSTRE.OBJ goto error

echo [40/416] Compiling SRC\MAIN\JAVA\IO\FILE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\FILE.CPP >> TC.RSP
if exist BUILDTC\FILE.OBJ del BUILDTC\FILE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FILE.OBJ goto error

echo [41/416] Compiling SRC\MAIN\JAVA\IO\FILINPST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILINPST.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\FILINPST.CPP >> TC.RSP
if exist BUILDTC\FILINPST.OBJ del BUILDTC\FILINPST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FILINPST.OBJ goto error

echo [42/416] Compiling SRC\MAIN\JAVA\IO\FILOUTST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILOUTST.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\FILOUTST.CPP >> TC.RSP
if exist BUILDTC\FILOUTST.OBJ del BUILDTC\FILOUTST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FILOUTST.OBJ goto error

echo [43/416] Compiling SRC\MAIN\JAVA\IO\BUFINPST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BUFINPST.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\BUFINPST.CPP >> TC.RSP
if exist BUILDTC\BUFINPST.OBJ del BUILDTC\BUFINPST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BUFINPST.OBJ goto error

echo [44/416] Compiling SRC\MAIN\JAVA\IO\BUFOUTST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BUFOUTST.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\BUFOUTST.CPP >> TC.RSP
if exist BUILDTC\BUFOUTST.OBJ del BUILDTC\BUFOUTST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BUFOUTST.OBJ goto error

echo [45/416] Compiling SRC\MAIN\JAVA\IO\PRINSTRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRINSTRE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\PRINSTRE.CPP >> TC.RSP
if exist BUILDTC\PRINSTRE.OBJ del BUILDTC\PRINSTRE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PRINSTRE.OBJ goto error

echo [46/416] Compiling SRC\MAIN\JAVA\IO\BUFFREAD.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BUFFREAD.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\BUFFREAD.CPP >> TC.RSP
if exist BUILDTC\BUFFREAD.OBJ del BUILDTC\BUFFREAD.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BUFFREAD.OBJ goto error

echo [47/416] Compiling SRC\MAIN\JAVA\IO\PRINWRIT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRINWRIT.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\PRINWRIT.CPP >> TC.RSP
if exist BUILDTC\PRINWRIT.OBJ del BUILDTC\PRINWRIT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PRINWRIT.OBJ goto error

echo [48/416] Compiling SRC\MAIN\JAVA\IO\READER.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\READER.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\READER.CPP >> TC.RSP
if exist BUILDTC\READER.OBJ del BUILDTC\READER.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\READER.OBJ goto error

echo [49/416] Compiling SRC\MAIN\JAVA\IO\INPSTRRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INPSTRRE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\INPSTRRE.CPP >> TC.RSP
if exist BUILDTC\INPSTRRE.OBJ del BUILDTC\INPSTRRE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\INPSTRRE.OBJ goto error

echo [50/416] Compiling SRC\MAIN\JAVA\IO\FILEREAD.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILEREAD.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\FILEREAD.CPP >> TC.RSP
if exist BUILDTC\FILEREAD.OBJ del BUILDTC\FILEREAD.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FILEREAD.OBJ goto error

echo [51/416] Compiling SRC\MAIN\JAVA\IO\STRETOKE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\STRETOKE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\IO\STRETOKE.CPP >> TC.RSP
if exist BUILDTC\STRETOKE.OBJ del BUILDTC\STRETOKE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\STRETOKE.OBJ goto error

echo [52/416] Compiling SRC\MAIN\JAVA\NIO\FILE\FILES.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILES.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\NIO\FILE\FILES.CPP >> TC.RSP
if exist BUILDTC\FILES.OBJ del BUILDTC\FILES.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FILES.OBJ goto error

echo [53/416] Compiling SRC\MAIN\JAVA\UTIL\FORMATTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FORMATTE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\UTIL\FORMATTE.CPP >> TC.RSP
if exist BUILDTC\FORMATTE.OBJ del BUILDTC\FORMATTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FORMATTE.OBJ goto error

echo [54/416] Compiling SRC\MAIN\JAVA\UTIL\STRITOKE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\STRITOKE.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\UTIL\STRITOKE.CPP >> TC.RSP
if exist BUILDTC\STRITOKE.OBJ del BUILDTC\STRITOKE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\STRITOKE.OBJ goto error

echo [55/416] Compiling SRC\MAIN\JAVA\UTIL\CONCURRE\EXECUTOR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\EXECUTOR.OBJ >> TC.RSP
echo SRC\MAIN\JAVA\UTIL\CONCURRE\EXECUTOR.CPP >> TC.RSP
if exist BUILDTC\EXECUTOR.OBJ del BUILDTC\EXECUTOR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\EXECUTOR.OBJ goto error

echo [56/416] Compiling SRC\MAIN\V\T\GUI\KEYEVENT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\KEYEVENT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\KEYEVENT.CPP >> TC.RSP
if exist BUILDTC\KEYEVENT.OBJ del BUILDTC\KEYEVENT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\KEYEVENT.OBJ goto error

echo [57/416] Compiling SRC\MAIN\V\T\GUI\MOUSEVEN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MOUSEVEN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\MOUSEVEN.CPP >> TC.RSP
if exist BUILDTC\MOUSEVEN.OBJ del BUILDTC\MOUSEVEN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\MOUSEVEN.OBJ goto error

echo [58/416] Compiling SRC\MAIN\V\T\GUI\FRAMEBUF\FRAMEBUF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FRAMEBUF.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FRAMEBUF\FRAMEBUF.CPP >> TC.RSP
if exist BUILDTC\FRAMEBUF.OBJ del BUILDTC\FRAMEBUF.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FRAMEBUF.OBJ goto error

echo [59/416] Compiling SRC\MAIN\V\T\GUI\FRAMEBUF\FBTHEME.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FBTHEME.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FRAMEBUF\FBTHEME.CPP >> TC.RSP
if exist BUILDTC\FBTHEME.OBJ del BUILDTC\FBTHEME.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FBTHEME.OBJ goto error

echo [60/416] Compiling SRC\MAIN\V\T\GUI\FRAMEBUF\FBPAINT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FBPAINT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FRAMEBUF\FBPAINT.CPP >> TC.RSP
if exist BUILDTC\FBPAINT.OBJ del BUILDTC\FBPAINT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FBPAINT.OBJ goto error

echo [61/416] Compiling SRC\MAIN\V\T\GUI\FRAMEBUF\FBSNAP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FBSNAP.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FRAMEBUF\FBSNAP.CPP >> TC.RSP
if exist BUILDTC\FBSNAP.OBJ del BUILDTC\FBSNAP.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FBSNAP.OBJ goto error

echo [62/416] Compiling SRC\MAIN\V\T\GUI\FRAMEBUF\FBMENBAR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FBMENBAR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FRAMEBUF\FBMENBAR.CPP >> TC.RSP
if exist BUILDTC\FBMENBAR.OBJ del BUILDTC\FBMENBAR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FBMENBAR.OBJ goto error

echo [63/416] Compiling SRC\MAIN\V\T\GUI\FRAMEBUF\FBMENRD.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FBMENRD.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FRAMEBUF\FBMENRD.CPP >> TC.RSP
if exist BUILDTC\FBMENRD.OBJ del BUILDTC\FBMENRD.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FBMENRD.OBJ goto error

echo [64/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\LIGIOMBI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LIGIOMBI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\LIGIOMBI.CPP >> TC.RSP
if exist BUILDTC\LIGIOMBI.OBJ del BUILDTC\LIGIOMBI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\LIGIOMBI.OBJ goto error

echo [65/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\INFPLAGI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INFPLAGI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\INFPLAGI.CPP >> TC.RSP
if exist BUILDTC\INFPLAGI.OBJ del BUILDTC\INFPLAGI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\INFPLAGI.OBJ goto error

echo [66/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\RAYGIZMO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYGIZMO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\RAYGIZMO.CPP >> TC.RSP
if exist BUILDTC\RAYGIZMO.OBJ del BUILDTC\RAYGIZMO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYGIZMO.OBJ goto error

echo [67/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\RAGIINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAGIINTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\RAGIINTE.CPP >> TC.RSP
if exist BUILDTC\RAGIINTE.OBJ del BUILDTC\RAGIINTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAGIINTE.OBJ goto error

echo [68/416] Compiling SRC\MAIN\V\T\GUI\TANGINTE\TANINTEV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TANINTEV.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\TANGINTE\TANINTEV.CPP >> TC.RSP
if exist BUILDTC\TANINTEV.OBJ del BUILDTC\TANINTEV.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TANINTEV.OBJ goto error

echo [69/416] Compiling SRC\MAIN\V\T\GUI\TANGINTE\TIE2IPGM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TIE2IPGM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\TANGINTE\TIE2IPGM.CPP >> TC.RSP
if exist BUILDTC\TIE2IPGM.OBJ del BUILDTC\TIE2IPGM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TIE2IPGM.OBJ goto error

echo [70/416] Compiling SRC\MAIN\V\T\GUI\TANGINTE\TAIE2RGM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TAIE2RGM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\TANGINTE\TAIE2RGM.CPP >> TC.RSP
if exist BUILDTC\TAIE2RGM.OBJ del BUILDTC\TAIE2RGM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TAIE2RGM.OBJ goto error

echo [71/416] Compiling SRC\MAIN\V\T\GUI\RENCONCO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RENCONCO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\RENCONCO.CPP >> TC.RSP
if exist BUILDTC\RENCONCO.OBJ del BUILDTC\RENCONCO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RENCONCO.OBJ goto error

echo [72/416] Compiling SRC\MAIN\V\T\GUI\FEEDBACK\PROMONCO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PROMONCO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FEEDBACK\PROMONCO.CPP >> TC.RSP
if exist BUILDTC\PROMONCO.OBJ del BUILDTC\PROMONCO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PROMONCO.OBJ goto error

echo [73/416] Compiling SRC\MAIN\V\T\GUI\FEEDBACK\PRMOCOLF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRMOCOLF.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FEEDBACK\PRMOCOLF.CPP >> TC.RSP
if exist BUILDTC\PRMOCOLF.OBJ del BUILDTC\PRMOCOLF.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PRMOCOLF.OBJ goto error

echo [74/416] Compiling SRC\MAIN\V\T\GUI\FEEDBACK\PRMOINRA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRMOINRA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FEEDBACK\PRMOINRA.CPP >> TC.RSP
if exist BUILDTC\PRMOINRA.OBJ del BUILDTC\PRMOINRA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PRMOINRA.OBJ goto error

echo [75/416] Compiling SRC\MAIN\V\T\GUI\FEEDBACK\PARALLEL\PAPRMOPR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PAPRMOPR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FEEDBACK\PARALLEL\PAPRMOPR.CPP >> TC.RSP
if exist BUILDTC\PAPRMOPR.OBJ del BUILDTC\PAPRMOPR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PAPRMOPR.OBJ goto error

echo [76/416] Compiling SRC\MAIN\V\T\GUI\FEEDBACK\PARALLEL\PAPRMOC1.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PAPRMOC1.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FEEDBACK\PARALLEL\PAPRMOC1.CPP >> TC.RSP
if exist BUILDTC\PAPRMOC1.OBJ del BUILDTC\PAPRMOC1.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PAPRMOC1.OBJ goto error

echo [77/416] Compiling SRC\MAIN\V\T\ENVI\CAMERA\CAMERA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAMERA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\CAMERA\CAMERA.CPP >> TC.RSP
if exist BUILDTC\CAMERA.OBJ del BUILDTC\CAMERA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CAMERA.OBJ goto error

echo [78/416] Compiling SRC\MAIN\V\T\ENVI\BACKGROU\SIMPBACK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMPBACK.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\BACKGROU\SIMPBACK.CPP >> TC.RSP
if exist BUILDTC\SIMPBACK.OBJ del BUILDTC\SIMPBACK.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SIMPBACK.OBJ goto error

echo [79/416] Compiling SRC\MAIN\V\T\ENVI\LIGHT\LIGHT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LIGHT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\LIGHT\LIGHT.CPP >> TC.RSP
if exist BUILDTC\LIGHT.OBJ del BUILDTC\LIGHT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\LIGHT.OBJ goto error

echo [80/416] Compiling SRC\MAIN\V\T\ENVI\LIGHT\AMBILIGH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AMBILIGH.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\LIGHT\AMBILIGH.CPP >> TC.RSP
if exist BUILDTC\AMBILIGH.OBJ del BUILDTC\AMBILIGH.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\AMBILIGH.OBJ goto error

echo [81/416] Compiling SRC\MAIN\V\T\ENVI\LIGHT\DIRELIGH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DIRELIGH.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\LIGHT\DIRELIGH.CPP >> TC.RSP
if exist BUILDTC\DIRELIGH.OBJ del BUILDTC\DIRELIGH.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\DIRELIGH.OBJ goto error

echo [82/416] Compiling SRC\MAIN\V\T\ENVI\LIGHT\POINLIGH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POINLIGH.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\LIGHT\POINLIGH.CPP >> TC.RSP
if exist BUILDTC\POINLIGH.OBJ del BUILDTC\POINLIGH.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POINLIGH.OBJ goto error

echo [83/416] Compiling SRC\MAIN\V\T\ENVI\LIGHT\SPOTLIGH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SPOTLIGH.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\LIGHT\SPOTLIGH.CPP >> TC.RSP
if exist BUILDTC\SPOTLIGH.OBJ del BUILDTC\SPOTLIGH.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SPOTLIGH.OBJ goto error

echo [84/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\SIMPSCEN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMPSCEN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\SIMPSCEN.CPP >> TC.RSP
if exist BUILDTC\SIMPSCEN.OBJ del BUILDTC\SIMPSCEN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SIMPSCEN.OBJ goto error

echo [85/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\SIMSCESN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMSCESN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\SIMSCESN.CPP >> TC.RSP
if exist BUILDTC\SIMSCESN.OBJ del BUILDTC\SIMSCESN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SIMSCESN.OBJ goto error

echo [86/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\SIMBODGR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMBODGR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\SIMBODGR.CPP >> TC.RSP
if exist BUILDTC\SIMBODGR.OBJ del BUILDTC\SIMBODGR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SIMBODGR.OBJ goto error

echo [87/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\SIMPBODY.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMPBODY.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\SIMPBODY.CPP >> TC.RSP
if exist BUILDTC\SIMPBODY.OBJ del BUILDTC\SIMPBODY.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SIMPBODY.OBJ goto error

echo [88/416] Compiling SRC\MAIN\V\T\PROCESSI\STOPWATC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\STOPWATC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\PROCESSI\STOPWATC.CPP >> TC.RSP
if exist BUILDTC\STOPWATC.OBJ del BUILDTC\STOPWATC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\STOPWATC.OBJ goto error

echo [89/416] Compiling SRC\MAIN\V\T\COMMON\SYMBALGE\ALGEEXPR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ALGEEXPR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\SYMBALGE\ALGEEXPR.CPP >> TC.RSP
if exist BUILDTC\ALGEEXPR.OBJ del BUILDTC\ALGEEXPR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\ALGEEXPR.OBJ goto error

echo [90/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAY.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAY.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAY.CPP >> TC.RSP
if exist BUILDTC\RAY.OBJ del BUILDTC\RAY.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAY.OBJ goto error

echo [91/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYHIT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYHIT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYHIT.CPP >> TC.RSP
if exist BUILDTC\RAYHIT.OBJ del BUILDTC\RAYHIT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYHIT.OBJ goto error

echo [92/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\PLANFACE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PLANFACE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\PLANFACE.CPP >> TC.RSP
if exist BUILDTC\PLANFACE.OBJ del BUILDTC\PLANFACE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PLANFACE.OBJ goto error

echo [93/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\PRIRAYTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRIRAYTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\PRIRAYTE.CPP >> TC.RSP
if exist BUILDTC\PRIRAYTE.OBJ del BUILDTC\PRIRAYTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PRIRAYTE.OBJ goto error

echo [94/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\SPATINDE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SPATINDE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\SPATINDE.CPP >> TC.RSP
if exist BUILDTC\SPATINDE.OBJ del BUILDTC\SPATINDE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SPATINDE.OBJ goto error

echo [95/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\SPINWORK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SPINWORK.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\SPINWORK.CPP >> TC.RSP
if exist BUILDTC\SPINWORK.OBJ del BUILDTC\SPINWORK.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SPINWORK.OBJ goto error

echo [96/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\LINSPAIN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LINSPAIN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\LINSPAIN.CPP >> TC.RSP
if exist BUILDTC\LINSPAIN.OBJ del BUILDTC\LINSPAIN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\LINSPAIN.OBJ goto error

echo [97/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\BISASPIN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BISASPIN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\BISASPIN.CPP >> TC.RSP
if exist BUILDTC\BISASPIN.OBJ del BUILDTC\BISASPIN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BISASPIN.OBJ goto error

echo [98/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\SPAGRTRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SPAGRTRE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\SPAGRTRE.CPP >> TC.RSP
if exist BUILDTC\SPAGRTRE.OBJ del BUILDTC\SPAGRTRE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SPAGRTRE.OBJ goto error

echo [99/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\GRSPAIND.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GRSPAIND.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SPATINDX\GRSPAIND.CPP >> TC.RSP
if exist BUILDTC\GRSPAIND.OBJ del BUILDTC\GRSPAIND.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GRSPAIND.OBJ goto error

echo [100/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\TRIANGLE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRIANGLE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\TRIANGLE.CPP >> TC.RSP
if exist BUILDTC\TRIANGLE.OBJ del BUILDTC\TRIANGLE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRIANGLE.OBJ goto error

echo [101/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\VERTEX.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VERTEX.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\VERTEX.CPP >> TC.RSP
if exist BUILDTC\VERTEX.OBJ del BUILDTC\VERTEX.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VERTEX.OBJ goto error

echo [102/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\VERTEX2D.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VERTEX2D.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\VERTEX2D.CPP >> TC.RSP
if exist BUILDTC\VERTEX2D.OBJ del BUILDTC\VERTEX2D.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VERTEX2D.OBJ goto error

echo [103/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\CURVE\CURVE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CURVE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\CURVE\CURVE.CPP >> TC.RSP
if exist BUILDTC\CURVE.OBJ del BUILDTC\CURVE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CURVE.OBJ goto error

echo [104/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\CURVE\PARACURV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PARACURV.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\CURVE\PARACURV.CPP >> TC.RSP
if exist BUILDTC\PARACURV.OBJ del BUILDTC\PARACURV.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PARACURV.OBJ goto error

echo [105/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEOMETRY.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GEOMETRY.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEOMETRY.CPP >> TC.RSP
if exist BUILDTC\GEOMETRY.OBJ del BUILDTC\GEOMETRY.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GEOMETRY.OBJ goto error

echo [106/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\SURFACE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SURFACE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\SURFACE.CPP >> TC.RSP
if exist BUILDTC\SURFACE.OBJ del BUILDTC\SURFACE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SURFACE.OBJ goto error

echo [107/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\HALFSPAC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\HALFSPAC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\HALFSPAC.CPP >> TC.RSP
if exist BUILDTC\HALFSPAC.OBJ del BUILDTC\HALFSPAC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\HALFSPAC.OBJ goto error

echo [108/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\INFIPLAN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INFIPLAN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\INFIPLAN.CPP >> TC.RSP
if exist BUILDTC\INFIPLAN.OBJ del BUILDTC\INFIPLAN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\INFIPLAN.OBJ goto error

echo [109/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\FUNEXPSU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FUNEXPSU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\FUNEXPSU.CPP >> TC.RSP
if exist BUILDTC\FUNEXPSU.OBJ del BUILDTC\FUNEXPSU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FUNEXPSU.OBJ goto error

echo [110/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\MD2MESH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MD2MESH.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\MD2MESH.CPP >> TC.RSP
if exist BUILDTC\MD2MESH.OBJ del BUILDTC\MD2MESH.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\MD2MESH.OBJ goto error

echo [111/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\PABICUPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PABICUPA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\PABICUPA.CPP >> TC.RSP
if exist BUILDTC\PABICUPA.OBJ del BUILDTC\PABICUPA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PABICUPA.OBJ goto error

echo [112/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\_PABICPI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PABICPI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\_PABICPI.CPP >> TC.RSP
if exist BUILDTC\_PABICPI.OBJ del BUILDTC\_PABICPI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_PABICPI.OBJ goto error

echo [113/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\TRISTRME.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRISTRME.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\TRISTRME.CPP >> TC.RSP
if exist BUILDTC\TRISTRME.OBJ del BUILDTC\TRISTRME.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRISTRME.OBJ goto error

echo [114/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\TRIAMESH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRIAMESH.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\TRIAMESH.CPP >> TC.RSP
if exist BUILDTC\TRIAMESH.OBJ del BUILDTC\TRIAMESH.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRIAMESH.OBJ goto error

echo [115/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\QUADMESH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\QUADMESH.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\QUADMESH.CPP >> TC.RSP
if exist BUILDTC\QUADMESH.OBJ del BUILDTC\QUADMESH.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\QUADMESH.OBJ goto error

echo [116/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\TRIMESGR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRIMESGR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\TRIMESGR.CPP >> TC.RSP
if exist BUILDTC\TRIMESGR.OBJ del BUILDTC\TRIMESGR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRIMESGR.OBJ goto error

echo [117/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\VOXELIZA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VOXELIZA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\VOXELIZA.CPP >> TC.RSP
if exist BUILDTC\VOXELIZA.OBJ del BUILDTC\VOXELIZA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VOXELIZA.OBJ goto error

echo [118/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\TRIMESVO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRIMESVO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\TRIMESVO.CPP >> TC.RSP
if exist BUILDTC\TRIMESVO.OBJ del BUILDTC\TRIMESVO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRIMESVO.OBJ goto error

echo [119/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\TRMEGRVO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRMEGRVO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\TRMEGRVO.CPP >> TC.RSP
if exist BUILDTC\TRMEGRVO.OBJ del BUILDTC\TRMEGRVO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRMEGRVO.OBJ goto error

echo [120/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\FUEXSUVO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FUEXSUVO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\FUEXSUVO.CPP >> TC.RSP
if exist BUILDTC\FUEXSUVO.OBJ del BUILDTC\FUEXSUVO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FUEXSUVO.OBJ goto error

echo [121/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\SURRAYIN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SURRAYIN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\SURRAYIN.CPP >> TC.RSP
if exist BUILDTC\SURRAYIN.OBJ del BUILDTC\SURRAYIN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SURRAYIN.OBJ goto error

echo [122/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\GEOMTRIA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GEOMTRIA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\GEOMTRIA.CPP >> TC.RSP
if exist BUILDTC\GEOMTRIA.OBJ del BUILDTC\GEOMTRIA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GEOMTRIA.OBJ goto error

echo [123/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\GEOCALEX.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GEOCALEX.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\GEOCALEX.CPP >> TC.RSP
if exist BUILDTC\GEOCALEX.OBJ del BUILDTC\GEOCALEX.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GEOCALEX.OBJ goto error

echo [124/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDECTR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MONDECTR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDECTR.CPP >> TC.RSP
if exist BUILDTC\MONDECTR.OBJ del BUILDTC\MONDECTR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\MONDECTR.OBJ goto error

echo [125/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_COAWPOT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_COAWPOT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_COAWPOT.CPP >> TC.RSP
if exist BUILDTC\_COAWPOT.OBJ del BUILDTC\_COAWPOT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_COAWPOT.OBJ goto error

echo [126/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_CONSTRU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_CONSTRU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_CONSTRU.CPP >> TC.RSP
if exist BUILDTC\_CONSTRU.OBJ del BUILDTC\_CONSTRU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_CONSTRU.OBJ goto error

echo [127/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_INCSEIN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_INCSEIN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_INCSEIN.CPP >> TC.RSP
if exist BUILDTC\_INCSEIN.OBJ del BUILDTC\_INCSEIN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_INCSEIN.OBJ goto error

echo [128/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_INSBASC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_INSBASC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_INSBASC.CPP >> TC.RSP
if exist BUILDTC\_INSBASC.OBJ del BUILDTC\_INSBASC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_INSBASC.OBJ goto error

echo [129/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_MONOTON.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_MONOTON.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_MONOTON.CPP >> TC.RSP
if exist BUILDTC\_MONOTON.OBJ del BUILDTC\_MONOTON.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_MONOTON.OBJ goto error

echo [130/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_RANSEOR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_RANSEOR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_RANSEOR.CPP >> TC.RSP
if exist BUILDTC\_RANSEOR.OBJ del BUILDTC\_RANSEOR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_RANSEOR.OBJ goto error

echo [131/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_SEGTABU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_SEGTABU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_SEGTABU.CPP >> TC.RSP
if exist BUILDTC\_SEGTABU.OBJ del BUILDTC\_SEGTABU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_SEGTABU.OBJ goto error

echo [132/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_TRIATRA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_TRIATRA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_TRIATRA.CPP >> TC.RSP
if exist BUILDTC\_TRIATRA.OBJ del BUILDTC\_TRIATRA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_TRIATRA.OBJ goto error

echo [133/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_TRTRQUN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_TRTRQUN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLTR\MONDE\_TRTRQUN.CPP >> TC.RSP
if exist BUILDTC\_TRTRQUN.OBJ del BUILDTC\_TRTRQUN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_TRTRQUN.OBJ goto error

echo [134/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLYCLIP\_POL2DWA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POL2DWA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLYCLIP\_POL2DWA.CPP >> TC.RSP
if exist BUILDTC\_POL2DWA.OBJ del BUILDTC\_POL2DWA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POL2DWA.OBJ goto error

echo [135/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLYCLIP\POLTOPME.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POLTOPME.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLYCLIP\POLTOPME.CPP >> TC.RSP
if exist BUILDTC\POLTOPME.OBJ del BUILDTC\POLTOPME.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POLTOPME.OBJ goto error

echo [136/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\POLYCLIP\WEATPOCL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WEATPOCL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\POLYCLIP\WEATPOCL.CPP >> TC.RSP
if exist BUILDTC\WEATPOCL.OBJ del BUILDTC\WEATPOCL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WEATPOCL.OBJ goto error

echo [137/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\POLYGON\_POL2DCO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POL2DCO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\POLYGON\_POL2DCO.CPP >> TC.RSP
if exist BUILDTC\_POL2DCO.OBJ del BUILDTC\_POL2DCO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POL2DCO.OBJ goto error

echo [138/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\POLYGON\POLYGO2D.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POLYGO2D.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\POLYGON\POLYGO2D.CPP >> TC.RSP
if exist BUILDTC\POLYGO2D.OBJ del BUILDTC\POLYGO2D.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POLYGO2D.OBJ goto error

echo [139/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\VOLUME.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VOLUME.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\VOLUME.CPP >> TC.RSP
if exist BUILDTC\VOLUME.OBJ del BUILDTC\VOLUME.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VOLUME.OBJ goto error

echo [140/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\SOLID.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SOLID.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\SOLID.CPP >> TC.RSP
if exist BUILDTC\SOLID.OBJ del BUILDTC\SOLID.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SOLID.OBJ goto error

echo [141/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\SPHERE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SPHERE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\SPHERE.CPP >> TC.RSP
if exist BUILDTC\SPHERE.OBJ del BUILDTC\SPHERE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SPHERE.OBJ goto error

echo [142/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\BOX.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BOX.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\BOX.CPP >> TC.RSP
if exist BUILDTC\BOX.OBJ del BUILDTC\BOX.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BOX.OBJ goto error

echo [143/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\CONE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CONE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\CONE.CPP >> TC.RSP
if exist BUILDTC\CONE.OBJ del BUILDTC\CONE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CONE.OBJ goto error

echo [144/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\ARROW.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ARROW.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\ARROW.CPP >> TC.RSP
if exist BUILDTC\ARROW.OBJ del BUILDTC\ARROW.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\ARROW.OBJ goto error

echo [145/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\TORUS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TORUS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\TORUS.CPP >> TC.RSP
if exist BUILDTC\TORUS.OBJ del BUILDTC\TORUS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TORUS.OBJ goto error

echo [146/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\TUBE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TUBE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\TUBE.CPP >> TC.RSP
if exist BUILDTC\TUBE.OBJ del BUILDTC\TUBE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TUBE.OBJ goto error

echo [147/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\PRISM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRISM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\PRISM.CPP >> TC.RSP
if exist BUILDTC\PRISM.OBJ del BUILDTC\PRISM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PRISM.OBJ goto error

echo [148/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\VOXEVOLU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VOXEVOLU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\VOXEVOLU.CPP >> TC.RSP
if exist BUILDTC\VOXEVOLU.OBJ del BUILDTC\VOXEVOLU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VOXEVOLU.OBJ goto error

echo [149/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POLBOUSO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POLBOUSO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POLBOUSO.CPP >> TC.RSP
if exist BUILDTC\POLBOUSO.OBJ del BUILDTC\POLBOUSO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POLBOUSO.OBJ goto error

echo [150/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOEO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOEO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOEO.CPP >> TC.RSP
if exist BUILDTC\POBOSOEO.OBJ del BUILDTC\POBOSOEO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOEO.OBJ goto error

echo [151/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSONP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSONP.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSONP.CPP >> TC.RSP
if exist BUILDTC\POBOSONP.OBJ del BUILDTC\POBOSONP.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSONP.OBJ goto error

echo [152/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOPR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOPR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOPR.CPP >> TC.RSP
if exist BUILDTC\POBOSOPR.OBJ del BUILDTC\POBOSOPR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOPR.OBJ goto error

echo [153/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOGV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOGV.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOGV.CPP >> TC.RSP
if exist BUILDTC\POBOSOGV.OBJ del BUILDTC\POBOSOGV.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOGV.OBJ goto error

echo [154/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOVE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOVE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOVE.CPP >> TC.RSP
if exist BUILDTC\POBOSOVE.OBJ del BUILDTC\POBOSOVE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOVE.OBJ goto error

echo [155/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOTE.CPP >> TC.RSP
if exist BUILDTC\POBOSOTE.OBJ del BUILDTC\POBOSOTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOTE.OBJ goto error

echo [156/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOFG.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOFG.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOFG.CPP >> TC.RSP
if exist BUILDTC\POBOSOFG.OBJ del BUILDTC\POBOSOFG.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOFG.OBJ goto error

echo [157/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSORE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSORE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSORE.CPP >> TC.RSP
if exist BUILDTC\POBOSORE.OBJ del BUILDTC\POBOSORE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSORE.OBJ goto error

echo [158/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\_POBOSTV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSTV.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\_POBOSTV.CPP >> TC.RSP
if exist BUILDTC\_POBOSTV.OBJ del BUILDTC\_POBOSTV.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSTV.OBJ goto error

echo [159/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\_POBSBTP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBSBTP.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\_POBSBTP.CPP >> TC.RSP
if exist BUILDTC\_POBSBTP.OBJ del BUILDTC\_POBSBTP.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBSBTP.OBJ goto error

echo [160/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\_POBOSVS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSVS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\_POBOSVS.CPP >> TC.RSP
if exist BUILDTC\_POBOSVS.OBJ del BUILDTC\_POBOSVS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSVS.OBJ goto error

echo [161/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOTS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOTS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOTS.CPP >> TC.RSP
if exist BUILDTC\POBOSOTS.OBJ del BUILDTC\POBOSOTS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOTS.OBJ goto error

echo [162/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSOE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOE.CPP >> TC.RSP
if exist BUILDTC\_POBOSOE.OBJ del BUILDTC\_POBOSOE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSOE.OBJ goto error

echo [163/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSHE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSHE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSHE.CPP >> TC.RSP
if exist BUILDTC\_POBOSHE.OBJ del BUILDTC\_POBOSHE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSHE.OBJ goto error

echo [164/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSOL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOL.CPP >> TC.RSP
if exist BUILDTC\_POBOSOL.OBJ del BUILDTC\_POBOSOL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSOL.OBJ goto error

echo [165/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSOF.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\NODE\_POBOSOF.CPP >> TC.RSP
if exist BUILDTC\_POBOSOF.OBJ del BUILDTC\_POBOSOF.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSOF.OBJ goto error

echo [166/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\NODE\_POBSNTS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBSNTS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\NODE\_POBSNTS.CPP >> TC.RSP
if exist BUILDTC\_POBSNTS.OBJ del BUILDTC\_POBSNTS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBSNTS.OBJ goto error

echo [167/416] Compiling SRC\MAIN\V\T\GUI\CAMCONAQ.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAMCONAQ.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\CAMCONAQ.CPP >> TC.RSP
if exist BUILDTC\CAMCONAQ.OBJ del BUILDTC\CAMCONAQ.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CAMCONAQ.OBJ goto error

echo [168/416] Compiling SRC\MAIN\V\T\GUI\CAMCONOR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAMCONOR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\CAMCONOR.CPP >> TC.RSP
if exist BUILDTC\CAMCONOR.OBJ del BUILDTC\CAMCONOR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CAMCONOR.OBJ goto error

echo [169/416] Compiling SRC\MAIN\V\T\GUI\CAMCONBL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAMCONBL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\CAMCONBL.CPP >> TC.RSP
if exist BUILDTC\CAMCONBL.OBJ del BUILDTC\CAMCONBL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CAMCONBL.OBJ goto error

echo [170/416] Compiling SRC\MAIN\V\T\GUI\CACOGOEA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CACOGOEA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\CACOGOEA.CPP >> TC.RSP
if exist BUILDTC\CACOGOEA.OBJ del BUILDTC\CACOGOEA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CACOGOEA.OBJ goto error

echo [171/416] Compiling SRC\MAIN\V\T\RENDER\TRACWORK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRACWORK.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\TRACWORK.CPP >> TC.RSP
if exist BUILDTC\TRACWORK.OBJ del BUILDTC\TRACWORK.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRACWORK.OBJ goto error

echo [172/416] Compiling SRC\MAIN\V\T\RENDER\SPHPOLCA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SPHPOLCA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\SPHPOLCA.CPP >> TC.RSP
if exist BUILDTC\SPHPOLCA.OBJ del BUILDTC\SPHPOLCA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SPHPOLCA.OBJ goto error

echo [173/416] Compiling SRC\MAIN\V\T\RENDER\FRAMEBUF\FRGI2DDR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FRGI2DDR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\FRAMEBUF\FRGI2DDR.CPP >> TC.RSP
if exist BUILDTC\FRGI2DDR.OBJ del BUILDTC\FRGI2DDR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FRGI2DDR.OBJ goto error

echo [174/416] Compiling SRC\MAIN\V\T\RENDER\GIZMO\GIZ2DDRL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GIZ2DDRL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\GIZMO\GIZ2DDRL.CPP >> TC.RSP
if exist BUILDTC\GIZ2DDRL.OBJ del BUILDTC\GIZ2DDRL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GIZ2DDRL.OBJ goto error

echo [175/416] Compiling SRC\MAIN\V\T\RENDER\GIZMO\GIZ2DPRO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GIZ2DPRO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\GIZMO\GIZ2DPRO.CPP >> TC.RSP
if exist BUILDTC\GIZ2DPRO.OBJ del BUILDTC\GIZ2DPRO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GIZ2DPRO.OBJ goto error

echo [176/416] Compiling SRC\MAIN\V\T\RENDER\GIZMO\INGI2DPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INGI2DPA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\GIZMO\INGI2DPA.CPP >> TC.RSP
if exist BUILDTC\INGI2DPA.OBJ del BUILDTC\INGI2DPA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\INGI2DPA.OBJ goto error

echo [177/416] Compiling SRC\MAIN\V\T\RENDER\GIZMO\ROGI2DPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ROGI2DPA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\GIZMO\ROGI2DPA.CPP >> TC.RSP
if exist BUILDTC\ROGI2DPA.OBJ del BUILDTC\ROGI2DPA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\ROGI2DPA.OBJ goto error

echo [178/416] Compiling SRC\MAIN\V\T\RENDER\GIZMO\CCGI2DPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CCGI2DPA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\GIZMO\CCGI2DPA.CPP >> TC.RSP
if exist BUILDTC\CCGI2DPA.OBJ del BUILDTC\CCGI2DPA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CCGI2DPA.OBJ goto error

echo [179/416] Compiling SRC\MAIN\V\T\RENDER\GIZMO\SCGI2DPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SCGI2DPA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\GIZMO\SCGI2DPA.CPP >> TC.RSP
if exist BUILDTC\SCGI2DPA.OBJ del BUILDTC\SCGI2DPA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SCGI2DPA.OBJ goto error

echo [180/416] Compiling SRC\MAIN\V\T\RENDER\GIZMO\TRGI2DPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRGI2DPA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\GIZMO\TRGI2DPA.CPP >> TC.RSP
if exist BUILDTC\TRGI2DPA.OBJ del BUILDTC\TRGI2DPA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRGI2DPA.OBJ goto error

echo [181/416] Compiling SRC\MAIN\V\T\RENDER\RAYTRACI\DEPBUFEN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DEPBUFEN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\RAYTRACI\DEPBUFEN.CPP >> TC.RSP
if exist BUILDTC\DEPBUFEN.OBJ del BUILDTC\DEPBUFEN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\DEPBUFEN.OBJ goto error

echo [182/416] Compiling SRC\MAIN\V\T\RENDER\RAYTRACI\PARARAYT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PARARAYT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\RAYTRACI\PARARAYT.CPP >> TC.RSP
if exist BUILDTC\PARARAYT.OBJ del BUILDTC\PARARAYT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PARARAYT.OBJ goto error

echo [183/416] Compiling SRC\MAIN\V\T\RENDER\RAYTRACI\RASTILAR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RASTILAR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\RAYTRACI\RASTILAR.CPP >> TC.RSP
if exist BUILDTC\RASTILAR.OBJ del BUILDTC\RASTILAR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RASTILAR.OBJ goto error

echo [184/416] Compiling SRC\MAIN\V\T\RENDER\RAYTRACI\RASTILGE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RASTILGE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\RAYTRACI\RASTILGE.CPP >> TC.RSP
if exist BUILDTC\RASTILGE.OBJ del BUILDTC\RASTILGE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RASTILGE.OBJ goto error

echo [185/416] Compiling SRC\MAIN\V\T\RENDER\RAYTRACI\SIMPRAYT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMPRAYT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\RAYTRACI\SIMPRAYT.CPP >> TC.RSP
if exist BUILDTC\SIMPRAYT.OBJ del BUILDTC\SIMPRAYT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SIMPRAYT.OBJ goto error

echo [186/416] Compiling SRC\MAIN\V\T\RENDER\AUTSTEGE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AUTSTEGE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\AUTSTEGE.CPP >> TC.RSP
if exist BUILDTC\AUTSTEGE.OBJ del BUILDTC\AUTSTEGE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\AUTSTEGE.OBJ goto error

echo [187/416] Compiling SRC\MAIN\V\T\RENDER\HIDDLINE\HIDLINRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\HIDLINRE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\HIDDLINE\HIDLINRE.CPP >> TC.RSP
if exist BUILDTC\HIDLINRE.OBJ del BUILDTC\HIDLINRE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\HIDLINRE.OBJ goto error

echo [188/416] Compiling SRC\MAIN\V\T\RENDER\HIDDSURF\HIDSURRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\HIDSURRE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\HIDDSURF\HIDSURRE.CPP >> TC.RSP
if exist BUILDTC\HIDSURRE.OBJ del BUILDTC\HIDSURRE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\HIDSURRE.OBJ goto error

echo [189/416] Compiling SRC\MAIN\V\T\RENDER\DITHERIN\DITHERIN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DITHERIN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\DITHERIN\DITHERIN.CPP >> TC.RSP
if exist BUILDTC\DITHERIN.OBJ del BUILDTC\DITHERIN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\DITHERIN.OBJ goto error

echo [190/416] Compiling SRC\MAIN\V\T\RENDER\RASTER\RASTER2D.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RASTER2D.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\RASTER\RASTER2D.CPP >> TC.RSP
if exist BUILDTC\RASTER2D.OBJ del BUILDTC\RASTER2D.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RASTER2D.OBJ goto error

echo [191/416] Compiling SRC\MAIN\V\T\RENDER\HIDDLINE\WIREREND.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIREREND.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\HIDDLINE\WIREREND.CPP >> TC.RSP
if exist BUILDTC\WIREREND.OBJ del BUILDTC\WIREREND.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WIREREND.OBJ goto error

echo [192/416] Compiling SRC\MAIN\V\T\RENDER\POBOSODE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSODE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\POBOSODE.CPP >> TC.RSP
if exist BUILDTC\POBOSODE.OBJ del BUILDTC\POBOSODE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSODE.OBJ goto error

echo [193/416] Compiling SRC\MAIN\V\T\RENDER\SHADERS\CPTESACO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CPTESACO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\SHADERS\CPTESACO.CPP >> TC.RSP
if exist BUILDTC\CPTESACO.OBJ del BUILDTC\CPTESACO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CPTESACO.OBJ goto error

echo [194/416] Compiling SRC\MAIN\V\T\RENDER\SHADERS\LIGHSHAD.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\LIGHSHAD.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\SHADERS\LIGHSHAD.CPP >> TC.RSP
if exist BUILDTC\LIGHSHAD.OBJ del BUILDTC\LIGHSHAD.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\LIGHSHAD.OBJ goto error

echo [195/416] Compiling SRC\MAIN\V\T\RENDER\SHADERS\CONSSHAD.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CONSSHAD.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\SHADERS\CONSSHAD.CPP >> TC.RSP
if exist BUILDTC\CONSSHAD.OBJ del BUILDTC\CONSSHAD.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CONSSHAD.OBJ goto error

echo [196/416] Compiling SRC\MAIN\V\T\RENDER\SHADERS\CONTEXSH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CONTEXSH.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\SHADERS\CONTEXSH.CPP >> TC.RSP
if exist BUILDTC\CONTEXSH.OBJ del BUILDTC\CONTEXSH.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CONTEXSH.OBJ goto error

echo [197/416] Compiling SRC\MAIN\V\T\RENDER\SHADERS\COOTORSH.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COOTORSH.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\SHADERS\COOTORSH.CPP >> TC.RSP
if exist BUILDTC\COOTORSH.OBJ del BUILDTC\COOTORSH.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\COOTORSH.OBJ goto error

echo [198/416] Compiling SRC\MAIN\V\T\RENDER\SHADERS\SHADSELE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SHADSELE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\SHADERS\SHADSELE.CPP >> TC.RSP
if exist BUILDTC\SHADSELE.OBJ del BUILDTC\SHADSELE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SHADSELE.OBJ goto error

echo [199/416] Compiling SRC\MAIN\V\T\IO\PERSELEM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PERSELEM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\PERSELEM.CPP >> TC.RSP
if exist BUILDTC\PERSELEM.OBJ del BUILDTC\PERSELEM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PERSELEM.OBJ goto error

echo [200/416] Compiling SRC\MAIN\V\T\IO\FILELOCA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILELOCA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\FILELOCA.CPP >> TC.RSP
if exist BUILDTC\FILELOCA.OBJ del BUILDTC\FILELOCA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FILELOCA.OBJ goto error

echo [201/416] Compiling SRC\MAIN\V\T\IO\WRAPPER\FILUNCWR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FILUNCWR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\WRAPPER\FILUNCWR.CPP >> TC.RSP
if exist BUILDTC\FILUNCWR.OBJ del BUILDTC\FILUNCWR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FILUNCWR.OBJ goto error

echo [202/416] Compiling SRC\MAIN\V\T\IO\WRAPPER\PIPINPST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PIPINPST.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\WRAPPER\PIPINPST.CPP >> TC.RSP
if exist BUILDTC\PIPINPST.OBJ del BUILDTC\PIPINPST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PIPINPST.OBJ goto error

echo [203/416] Compiling SRC\MAIN\V\T\IO\WRAPPER\PIPOUTST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PIPOUTST.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\WRAPPER\PIPOUTST.CPP >> TC.RSP
if exist BUILDTC\PIPOUTST.OBJ del BUILDTC\PIPOUTST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PIPOUTST.OBJ goto error

echo [204/416] Compiling SRC\MAIN\V\T\IO\WRAPPER\MAT2X2PR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MAT2X2PR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\WRAPPER\MAT2X2PR.CPP >> TC.RSP
if exist BUILDTC\MAT2X2PR.OBJ del BUILDTC\MAT2X2PR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\MAT2X2PR.OBJ goto error

echo [205/416] Compiling SRC\MAIN\V\T\IO\WRAPPER\VEC3DPRI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VEC3DPRI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\WRAPPER\VEC3DPRI.CPP >> TC.RSP
if exist BUILDTC\VEC3DPRI.OBJ del BUILDTC\VEC3DPRI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VEC3DPRI.OBJ goto error

echo [206/416] Compiling SRC\MAIN\V\T\IO\GUI\DIALPERS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DIALPERS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GUI\DIALPERS.CPP >> TC.RSP
if exist BUILDTC\DIALPERS.OBJ del BUILDTC\DIALPERS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\DIALPERS.OBJ goto error

echo [207/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\ENVIPERS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ENVIPERS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\ENVIPERS.CPP >> TC.RSP
if exist BUILDTC\ENVIPERS.OBJ del BUILDTC\ENVIPERS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\ENVIPERS.OBJ goto error

echo [208/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\WRITEOBJ.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WRITEOBJ.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\WRITEOBJ.CPP >> TC.RSP
if exist BUILDTC\WRITEOBJ.OBJ del BUILDTC\WRITEOBJ.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WRITEOBJ.OBJ goto error

echo [209/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\WRITEGTS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WRITEGTS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\WRITEGTS.CPP >> TC.RSP
if exist BUILDTC\WRITEGTS.OBJ del BUILDTC\WRITEGTS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WRITEGTS.OBJ goto error

echo [210/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\WRITEVTK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WRITEVTK.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\WRITEVTK.CPP >> TC.RSP
if exist BUILDTC\WRITEVTK.OBJ del BUILDTC\WRITEVTK.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WRITEVTK.OBJ goto error

echo [211/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\READEOBJ.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\READEOBJ.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\READEOBJ.CPP >> TC.RSP
if exist BUILDTC\READEOBJ.OBJ del BUILDTC\READEOBJ.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\READEOBJ.OBJ goto error

echo [212/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\READEPLY.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\READEPLY.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\READEPLY.CPP >> TC.RSP
if exist BUILDTC\READEPLY.OBJ del BUILDTC\READEPLY.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\READEPLY.OBJ goto error

echo [213/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\REAPLYRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\REAPLYRE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\REAPLYRE.CPP >> TC.RSP
if exist BUILDTC\REAPLYRE.OBJ del BUILDTC\REAPLYRE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\REAPLYRE.OBJ goto error

echo [214/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\REAMITSC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\REAMITSC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\REAMITSC.CPP >> TC.RSP
if exist BUILDTC\REAMITSC.OBJ del BUILDTC\REAMITSC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\REAMITSC.OBJ goto error

echo [215/416] Compiling SRC\MAIN\V\T\IO\IMAGE\IMAGPERS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\IMAGPERS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\IMAGE\IMAGPERS.CPP >> TC.RSP
if exist BUILDTC\IMAGPERS.OBJ del BUILDTC\IMAGPERS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\IMAGPERS.OBJ goto error

echo [216/416] Compiling SRC\MAIN\V\T\MEDIA\SOLITEXT\TEXTUTIL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TEXTUTIL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\SOLITEXT\TEXTUTIL.CPP >> TC.RSP
if exist BUILDTC\TEXTUTIL.OBJ del BUILDTC\TEXTUTIL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TEXTUTIL.OBJ goto error

echo [217/416] Compiling SRC\MAIN\V\T\MEDIA\SOLITEXT\FROM2D\CORGIMHU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CORGIMHU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\SOLITEXT\FROM2D\CORGIMHU.CPP >> TC.RSP
if exist BUILDTC\CORGIMHU.OBJ del BUILDTC\CORGIMHU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CORGIMHU.OBJ goto error

echo [218/416] Compiling SRC\MAIN\V\T\MEDIA\SOLITEXT\FROM2D\IMAGTEXT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\IMAGTEXT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\SOLITEXT\FROM2D\IMAGTEXT.CPP >> TC.RSP
if exist BUILDTC\IMAGTEXT.OBJ del BUILDTC\IMAGTEXT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\IMAGTEXT.OBJ goto error

echo [219/416] Compiling SRC\MAIN\V\T\MEDIA\SOLITEXT\FROM2D\SOTECOMA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SOTECOMA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\SOLITEXT\FROM2D\SOTECOMA.CPP >> TC.RSP
if exist BUILDTC\SOTECOMA.OBJ del BUILDTC\SOTECOMA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SOTECOMA.OBJ goto error

echo [220/416] Compiling SRC\MAIN\V\T\MEDIA\SOLITEXT\PROCEDUR\BUMTEXFI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BUMTEXFI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\SOLITEXT\PROCEDUR\BUMTEXFI.CPP >> TC.RSP
if exist BUILDTC\BUMTEXFI.OBJ del BUILDTC\BUMTEXFI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BUMTEXFI.OBJ goto error

echo [221/416] Compiling SRC\MAIN\V\T\MEDIA\SOLITEXT\PROCEDUR\COLTEXFI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COLTEXFI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\SOLITEXT\PROCEDUR\COLTEXFI.CPP >> TC.RSP
if exist BUILDTC\COLTEXFI.OBJ del BUILDTC\COLTEXFI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\COLTEXFI.OBJ goto error

echo [222/416] Compiling SRC\MAIN\V\T\MEDIA\SOLITEXT\PROCEDUR\PROCNOIS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PROCNOIS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\SOLITEXT\PROCEDUR\PROCNOIS.CPP >> TC.RSP
if exist BUILDTC\PROCNOIS.OBJ del BUILDTC\PROCNOIS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PROCNOIS.OBJ goto error

echo [223/416] Compiling SRC\MAIN\V\T\MEDIA\RGBPIXEL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBPIXEL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\RGBPIXEL.CPP >> TC.RSP
if exist BUILDTC\RGBPIXEL.OBJ del BUILDTC\RGBPIXEL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RGBPIXEL.OBJ goto error

echo [224/416] Compiling SRC\MAIN\V\T\MEDIA\RGBAPIXE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBAPIXE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\RGBAPIXE.CPP >> TC.RSP
if exist BUILDTC\RGBAPIXE.OBJ del BUILDTC\RGBAPIXE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RGBAPIXE.OBJ goto error

echo [225/416] Compiling SRC\MAIN\V\T\MEDIA\SHAPDESC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SHAPDESC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\SHAPDESC.CPP >> TC.RSP
if exist BUILDTC\SHAPDESC.OBJ del BUILDTC\SHAPDESC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SHAPDESC.OBJ goto error

echo [226/416] Compiling SRC\MAIN\V\T\MEDIA\RGBCOLP1.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBCOLP1.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\RGBCOLP1.CPP >> TC.RSP
if exist BUILDTC\RGBCOLP1.OBJ del BUILDTC\RGBCOLP1.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RGBCOLP1.OBJ goto error

echo [227/416] Compiling SRC\MAIN\V\T\MEDIA\RGBCOLPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBCOLPA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\RGBCOLPA.CPP >> TC.RSP
if exist BUILDTC\RGBCOLPA.OBJ del BUILDTC\RGBCOLPA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RGBCOLPA.OBJ goto error

echo [228/416] Compiling SRC\MAIN\V\T\MEDIA\RGPRCOPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGPRCOPA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\RGPRCOPA.CPP >> TC.RSP
if exist BUILDTC\RGPRCOPA.OBJ del BUILDTC\RGPRCOPA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RGPRCOPA.OBJ goto error

echo [229/416] Compiling SRC\MAIN\V\T\MEDIA\GRASCAPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GRASCAPA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\GRASCAPA.CPP >> TC.RSP
if exist BUILDTC\GRASCAPA.OBJ del BUILDTC\GRASCAPA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GRASCAPA.OBJ goto error

echo [230/416] Compiling SRC\MAIN\V\T\MEDIA\IMAGE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\IMAGE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\IMAGE.CPP >> TC.RSP
if exist BUILDTC\IMAGE.OBJ del BUILDTC\IMAGE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\IMAGE.OBJ goto error

echo [231/416] Compiling SRC\MAIN\V\T\MEDIA\RGBIMAU1.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBIMAU1.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\RGBIMAU1.CPP >> TC.RSP
if exist BUILDTC\RGBIMAU1.OBJ del BUILDTC\RGBIMAU1.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RGBIMAU1.OBJ goto error

echo [232/416] Compiling SRC\MAIN\V\T\MEDIA\RGBIMAUN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBIMAUN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\RGBIMAUN.CPP >> TC.RSP
if exist BUILDTC\RGBIMAUN.OBJ del BUILDTC\RGBIMAUN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RGBIMAUN.OBJ goto error

echo [233/416] Compiling SRC\MAIN\V\T\MEDIA\RGIMHDUN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGIMHDUN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\RGIMHDUN.CPP >> TC.RSP
if exist BUILDTC\RGIMHDUN.OBJ del BUILDTC\RGIMHDUN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RGIMHDUN.OBJ goto error

echo [234/416] Compiling SRC\MAIN\V\T\MEDIA\INCOIMUN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INCOIMUN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\INCOIMUN.CPP >> TC.RSP
if exist BUILDTC\INCOIMUN.OBJ del BUILDTC\INCOIMUN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\INCOIMUN.OBJ goto error

echo [235/416] Compiling SRC\MAIN\V\T\MEDIA\INCOIMHU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INCOIMHU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\INCOIMHU.CPP >> TC.RSP
if exist BUILDTC\INCOIMHU.OBJ del BUILDTC\INCOIMHU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\INCOIMHU.OBJ goto error

echo [236/416] Compiling SRC\MAIN\V\T\MEDIA\RGBIMACO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGBIMACO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\RGBIMACO.CPP >> TC.RSP
if exist BUILDTC\RGBIMACO.OBJ del BUILDTC\RGBIMACO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RGBIMACO.OBJ goto error

echo [237/416] Compiling SRC\MAIN\V\T\MEDIA\ZBUFFER.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ZBUFFER.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\ZBUFFER.CPP >> TC.RSP
if exist BUILDTC\ZBUFFER.OBJ del BUILDTC\ZBUFFER.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\ZBUFFER.OBJ goto error

echo [238/416] Compiling SRC\MAIN\V\T\MEDIA\NORMAMAP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\NORMAMAP.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\NORMAMAP.CPP >> TC.RSP
if exist BUILDTC\NORMAMAP.OBJ del BUILDTC\NORMAMAP.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\NORMAMAP.OBJ goto error

echo [239/416] Compiling SRC\MAIN\V\T\MEDIA\CAL2DBUF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAL2DBUF.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\CAL2DBUF.CPP >> TC.RSP
if exist BUILDTC\CAL2DBUF.OBJ del BUILDTC\CAL2DBUF.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CAL2DBUF.OBJ goto error

echo [240/416] Compiling SRC\MAIN\V\T\MEDIA\CAL3DBUF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAL3DBUF.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\CAL3DBUF.CPP >> TC.RSP
if exist BUILDTC\CAL3DBUF.OBJ del BUILDTC\CAL3DBUF.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CAL3DBUF.OBJ goto error

echo [241/416] Compiling SRC\MAIN\V\T\MEDIA\PRCOSHDE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRCOSHDE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\PRCOSHDE.CPP >> TC.RSP
if exist BUILDTC\PRCOSHDE.OBJ del BUILDTC\PRCOSHDE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PRCOSHDE.OBJ goto error

echo [242/416] Compiling SRC\MAIN\V\T\MEDIA\FOUSHADE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FOUSHADE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\FOUSHADE.CPP >> TC.RSP
if exist BUILDTC\FOUSHADE.OBJ del BUILDTC\FOUSHADE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FOUSHADE.OBJ goto error

echo [243/416] Compiling SRC\MAIN\V\T\MEDIA\GEOMMETA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GEOMMETA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\GEOMMETA.CPP >> TC.RSP
if exist BUILDTC\GEOMMETA.OBJ del BUILDTC\GEOMMETA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GEOMMETA.OBJ goto error

echo [244/416] Compiling SRC\MAIN\V\T\ENVI\MATERIAL\SIMPMATE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SIMPMATE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\MATERIAL\SIMPMATE.CPP >> TC.RSP
if exist BUILDTC\SIMPMATE.OBJ del BUILDTC\SIMPMATE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SIMPMATE.OBJ goto error

echo [245/416] Compiling SRC\MAIN\V\T\ENVI\MATERIAL\MICFACMA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MICFACMA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\MATERIAL\MICFACMA.CPP >> TC.RSP
if exist BUILDTC\MICFACMA.OBJ del BUILDTC\MICFACMA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\MICFACMA.OBJ goto error

echo [246/416] Compiling SRC\MAIN\V\T\ENVI\MATERIAL\SHADTYPE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SHADTYPE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\MATERIAL\SHADTYPE.CPP >> TC.RSP
if exist BUILDTC\SHADTYPE.OBJ del BUILDTC\SHADTYPE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SHADTYPE.OBJ goto error

echo [247/416] Compiling SRC\MAIN\V\T\ENVI\MATERIAL\RENDCONF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RENDCONF.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\MATERIAL\RENDCONF.CPP >> TC.RSP
if exist BUILDTC\RENDCONF.OBJ del BUILDTC\RENDCONF.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RENDCONF.OBJ goto error

echo [248/416] Compiling SRC\MAIN\V\T\ENVI\MATERIAL\PHONLOBE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PHONLOBE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\MATERIAL\PHONLOBE.CPP >> TC.RSP
if exist BUILDTC\PHONLOBE.OBJ del BUILDTC\PHONLOBE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PHONLOBE.OBJ goto error

echo [249/416] Compiling SRC\MAIN\V\T\ENVI\MATERIAL\PHONMATE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PHONMATE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\MATERIAL\PHONMATE.CPP >> TC.RSP
if exist BUILDTC\PHONMATE.OBJ del BUILDTC\PHONMATE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PHONMATE.OBJ goto error

echo [250/416] Compiling SRC\MAIN\V\T\ENVI\MATERIAL\SCATFRAM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SCATFRAM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\MATERIAL\SCATFRAM.CPP >> TC.RSP
if exist BUILDTC\SCATFRAM.OBJ del BUILDTC\SCATFRAM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SCATFRAM.OBJ goto error

echo [251/416] Compiling SRC\MAIN\V\T\ENVI\MATERIAL\TEXTURE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TEXTURE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\MATERIAL\TEXTURE.CPP >> TC.RSP
if exist BUILDTC\TEXTURE.OBJ del BUILDTC\TEXTURE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TEXTURE.OBJ goto error

echo [252/416] Compiling SRC\MAIN\V\T\PROCESSI\CURVMODE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CURVMODE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\PROCESSI\CURVMODE.CPP >> TC.RSP
if exist BUILDTC\CURVMODE.OBJ del BUILDTC\CURVMODE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CURVMODE.OBJ goto error

echo [253/416] Compiling SRC\MAIN\V\T\PROCESSI\IMAGPROC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\IMAGPROC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\PROCESSI\IMAGPROC.CPP >> TC.RSP
if exist BUILDTC\IMAGPROC.OBJ del BUILDTC\IMAGPROC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\IMAGPROC.OBJ goto error

echo [254/416] Compiling SRC\MAIN\V\T\ENVI\BACKGROU\CUBEBACK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CUBEBACK.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\BACKGROU\CUBEBACK.CPP >> TC.RSP
if exist BUILDTC\CUBEBACK.OBJ del BUILDTC\CUBEBACK.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CUBEBACK.OBJ goto error

echo [255/416] Compiling SRC\MAIN\V\T\ENVI\BACKGROU\FIXEBACK.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FIXEBACK.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\BACKGROU\FIXEBACK.CPP >> TC.RSP
if exist BUILDTC\FIXEBACK.OBJ del BUILDTC\FIXEBACK.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FIXEBACK.OBJ goto error

echo [256/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\POBOSOMO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOMO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\POBOSOMO.CPP >> TC.RSP
if exist BUILDTC\POBOSOMO.OBJ del BUILDTC\POBOSOMO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOMO.OBJ goto error

echo [257/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\_POBOSOO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSOO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\_POBOSOO.CPP >> TC.RSP
if exist BUILDTC\_POBOSOO.OBJ del BUILDTC\_POBOSOO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSOO.OBJ goto error

echo [258/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\FALLBA\_POBOSIN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSIN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\FALLBA\_POBOSIN.CPP >> TC.RSP
if exist BUILDTC\_POBOSIN.OBJ del BUILDTC\_POBOSIN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSIN.OBJ goto error

echo [259/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOSC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOSC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOSC.CPP >> TC.RSP
if exist BUILDTC\POBOSOSC.OBJ del BUILDTC\POBOSOSC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOSC.OBJ goto error

echo [260/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\_POBOSVN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSVN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\_POBOSVN.CPP >> TC.RSP
if exist BUILDTC\_POBOSVN.OBJ del BUILDTC\_POBOSVN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSVN.OBJ goto error

echo [261/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\_PBSSOA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSOA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\_PBSSOA.CPP >> TC.RSP
if exist BUILDTC\_PBSSOA.OBJ del BUILDTC\_PBSSOA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_PBSSOA.OBJ goto error

echo [262/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_POBOSIO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSIO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_POBOSIO.CPP >> TC.RSP
if exist BUILDTC\_POBOSIO.OBJ del BUILDTC\_POBOSIO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSIO.OBJ goto error

echo [263/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\SLICIN\_POBOSOS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSOS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\SLICIN\_POBOSOS.CPP >> TC.RSP
if exist BUILDTC\_POBOSOS.OBJ del BUILDTC\_POBOSOS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSOS.OBJ goto error

echo [264/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\_SETOPTR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_SETOPTR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\_SETOPTR.CPP >> TC.RSP
if exist BUILDTC\_SETOPTR.OBJ del BUILDTC\_SETOPTR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_SETOPTR.OBJ goto error

echo [265/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\TOP\_PBSSONE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSONE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\TOP\_PBSSONE.CPP >> TC.RSP
if exist BUILDTC\_PBSSONE.OBJ del BUILDTC\_PBSSONE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_PBSSONE.OBJ goto error

echo [266/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\TOP\_PBSSNEC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -v- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSNEC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\TOP\_PBSSNEC.CPP >> TC.RSP
if exist BUILDTC\_PBSSNEC.OBJ del BUILDTC\_PBSSNEC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_PBSSNEC.OBJ goto error

echo [267/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\TOP\_POBOSSF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSSF.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\TOP\_POBOSSF.CPP >> TC.RSP
if exist BUILDTC\_POBOSSF.OBJ del BUILDTC\_POBOSSF.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSSF.OBJ goto error

echo [268/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\INT\_PBSSICB.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSICB.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\INT\_PBSSICB.CPP >> TC.RSP
if exist BUILDTC\_PBSSICB.OBJ del BUILDTC\_PBSSICB.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_PBSSICB.OBJ goto error

echo [269/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\INT\_POBOSSI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSSI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\INT\_POBOSSI.CPP >> TC.RSP
if exist BUILDTC\_POBOSSI.OBJ del BUILDTC\_POBOSSI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSSI.OBJ goto error

echo [270/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\_POBOSSO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSSO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\_POBOSSO.CPP >> TC.RSP
if exist BUILDTC\_POBOSSO.OBJ del BUILDTC\_POBOSSO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSSO.OBJ goto error

echo [271/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\INT\_PBSSGPP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSGPP.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\INT\_PBSSGPP.CPP >> TC.RSP
if exist BUILDTC\_PBSSGPP.OBJ del BUILDTC\_PBSSGPP.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_PBSSGPP.OBJ goto error

echo [272/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSOSC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSOSC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSOSC.CPP >> TC.RSP
if exist BUILDTC\_PBSSOSC.OBJ del BUILDTC\_PBSSOSC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_PBSSOSC.OBJ goto error

echo [273/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSVFC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSVFC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSVFC.CPP >> TC.RSP
if exist BUILDTC\_PBSSVFC.OBJ del BUILDTC\_PBSSVFC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_PBSSVFC.OBJ goto error

echo [274/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSVVC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSVVC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSVVC.CPP >> TC.RSP
if exist BUILDTC\_PBSSVVC.OBJ del BUILDTC\_PBSSVVC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_PBSSVVC.OBJ goto error

echo [275/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_POBOSSC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSSC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_POBOSSC.CPP >> TC.RSP
if exist BUILDTC\_POBOSSC.OBJ del BUILDTC\_POBOSSC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSSC.OBJ goto error

echo [276/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSNIC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_PBSSNIC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\BOO\CLA\_PBSSNIC.CPP >> TC.RSP
if exist BUILDTC\_PBSSNIC.OBJ del BUILDTC\_PBSSNIC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_PBSSNIC.OBJ goto error

echo [277/416] Compiling SRC\MAIN\V\T\COMMON\STATISTI\POBOSOST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOST.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\STATISTI\POBOSOST.CPP >> TC.RSP
if exist BUILDTC\POBOSOST.OBJ del BUILDTC\POBOSOST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOST.OBJ goto error

echo [278/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\FIXTUR\SITEGELI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SITEGELI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\FIXTUR\SITEGELI.CPP >> TC.RSP
if exist BUILDTC\SITEGELI.OBJ del BUILDTC\SITEGELI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SITEGELI.OBJ goto error

echo [279/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\FIXTUR\CSKUBOFI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSKUBOFI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\FIXTUR\CSKUBOFI.CPP >> TC.RSP
if exist BUILDTC\CSKUBOFI.OBJ del BUILDTC\CSKUBOFI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSKUBOFI.OBJ goto error

echo [280/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\VALIDA\_POBOSFV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\_POBOSFV.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\GEP\PSO\VALIDA\_POBOSFV.CPP >> TC.RSP
if exist BUILDTC\_POBOSFV.OBJ del BUILDTC\_POBOSFV.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\_POBOSFV.OBJ goto error

echo [281/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\PARCURPE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PARCURPE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\PARCURPE.CPP >> TC.RSP
if exist BUILDTC\PARCURPE.OBJ del BUILDTC\PARCURPE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PARCURPE.OBJ goto error

echo [282/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\PABICUPP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PABICUPP.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\PABICUPP.CPP >> TC.RSP
if exist BUILDTC\PABICUPP.OBJ del BUILDTC\PABICUPP.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PABICUPP.OBJ goto error

echo [283/416] Compiling SRC\MAIN\V\T\IO\IMAGE\RGCOPAPE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RGCOPAPE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\IMAGE\RGCOPAPE.CPP >> TC.RSP
if exist BUILDTC\RGCOPAPE.OBJ del BUILDTC\RGCOPAPE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RGCOPAPE.OBJ goto error

echo [284/416] Compiling SRC\MAIN\ORG\W3C\DOM\NODE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\NODE.OBJ >> TC.RSP
echo SRC\MAIN\ORG\W3C\DOM\NODE.CPP >> TC.RSP
if exist BUILDTC\NODE.OBJ del BUILDTC\NODE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\NODE.OBJ goto error

echo [285/416] Compiling SRC\MAIN\JAVAX\XML\PARSERS\DOCUBUIL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DOCUBUIL.OBJ >> TC.RSP
echo SRC\MAIN\JAVAX\XML\PARSERS\DOCUBUIL.CPP >> TC.RSP
if exist BUILDTC\DOCUBUIL.OBJ del BUILDTC\DOCUBUIL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\DOCUBUIL.OBJ goto error

echo [286/416] Compiling SRC\MAIN\JAVAX\XML\TRANSFOR\TRANSFOR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRANSFOR.OBJ >> TC.RSP
echo SRC\MAIN\JAVAX\XML\TRANSFOR\TRANSFOR.CPP >> TC.RSP
if exist BUILDTC\TRANSFOR.OBJ del BUILDTC\TRANSFOR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRANSFOR.OBJ goto error

echo [287/416] Compiling SRC\MAIN\V\T\GUI\WIDGET\WIDGET.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGET.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\WIDGET\WIDGET.CPP >> TC.RSP
if exist BUILDTC\WIDGET.OBJ del BUILDTC\WIDGET.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WIDGET.OBJ goto error

echo [288/416] Compiling SRC\MAIN\V\T\GUI\WIDGET\WIDBUTGR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDBUTGR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\WIDGET\WIDBUTGR.CPP >> TC.RSP
if exist BUILDTC\WIDBUTGR.OBJ del BUILDTC\WIDBUTGR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WIDBUTGR.OBJ goto error

echo [289/416] Compiling SRC\MAIN\V\T\GUI\WIDGET\WIDGCOMM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGCOMM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\WIDGET\WIDGCOMM.CPP >> TC.RSP
if exist BUILDTC\WIDGCOMM.OBJ del BUILDTC\WIDGCOMM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WIDGCOMM.OBJ goto error

echo [290/416] Compiling SRC\MAIN\V\T\GUI\WIDGET\WIDGDIAL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGDIAL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\WIDGET\WIDGDIAL.CPP >> TC.RSP
if exist BUILDTC\WIDGDIAL.OBJ del BUILDTC\WIDGDIAL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WIDGDIAL.OBJ goto error

echo [291/416] Compiling SRC\MAIN\V\T\GUI\WIDGET\WIDDIAEL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDDIAEL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\WIDGET\WIDDIAEL.CPP >> TC.RSP
if exist BUILDTC\WIDDIAEL.OBJ del BUILDTC\WIDDIAEL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WIDDIAEL.OBJ goto error

echo [292/416] Compiling SRC\MAIN\V\T\GUI\DIALOG\COMPONEN\DIAFILFI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DIAFILFI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\DIALOG\COMPONEN\DIAFILFI.CPP >> TC.RSP
if exist BUILDTC\DIAFILFI.OBJ del BUILDTC\DIAFILFI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\DIAFILFI.OBJ goto error

echo [293/416] Compiling SRC\MAIN\V\T\GUI\DIALOG\COMPONEN\DIATRENO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DIATRENO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\DIALOG\COMPONEN\DIATRENO.CPP >> TC.RSP
if exist BUILDTC\DIATRENO.OBJ del BUILDTC\DIATRENO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\DIATRENO.OBJ goto error

echo [294/416] Compiling SRC\MAIN\V\T\GUI\DIALOG\RUNTIME\DIALSESS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\DIALSESS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\DIALOG\RUNTIME\DIALSESS.CPP >> TC.RSP
if exist BUILDTC\DIALSESS.OBJ del BUILDTC\DIALSESS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\DIALSESS.OBJ goto error

echo [295/416] Compiling SRC\MAIN\V\T\GUI\WIDGET\WIDGMENU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGMENU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\WIDGET\WIDGMENU.CPP >> TC.RSP
if exist BUILDTC\WIDGMENU.OBJ del BUILDTC\WIDGMENU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WIDGMENU.OBJ goto error

echo [296/416] Compiling SRC\MAIN\V\T\GUI\WIDGET\WIDMENEL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDMENEL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\WIDGET\WIDMENEL.CPP >> TC.RSP
if exist BUILDTC\WIDMENEL.OBJ del BUILDTC\WIDMENEL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WIDMENEL.OBJ goto error

echo [297/416] Compiling SRC\MAIN\V\T\GUI\WIDGET\WIDMENIT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDMENIT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\WIDGET\WIDMENIT.CPP >> TC.RSP
if exist BUILDTC\WIDMENIT.OBJ del BUILDTC\WIDMENIT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WIDMENIT.OBJ goto error

echo [298/416] Compiling SRC\MAIN\V\T\GUI\VIEWPORT\VIEWPORT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VIEWPORT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\VIEWPORT\VIEWPORT.CPP >> TC.RSP
if exist BUILDTC\VIEWPORT.OBJ del BUILDTC\VIEWPORT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VIEWPORT.OBJ goto error

echo [299/416] Compiling SRC\MAIN\V\T\GUI\VIEWPORT\VIEELESC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VIEELESC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\VIEWPORT\VIEELESC.CPP >> TC.RSP
if exist BUILDTC\VIEELESC.OBJ del BUILDTC\VIEELESC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VIEELESC.OBJ goto error

echo [300/416] Compiling SRC\MAIN\V\T\GUI\VIEWPORT\VIEWPSET.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VIEWPSET.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\VIEWPORT\VIEWPSET.CPP >> TC.RSP
if exist BUILDTC\VIEWPSET.OBJ del BUILDTC\VIEWPSET.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VIEWPSET.OBJ goto error

echo [301/416] Compiling SRC\MAIN\V\T\GUI\VIEWPORT\VISEINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VISEINTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\VIEWPORT\VISEINTE.CPP >> TC.RSP
if exist BUILDTC\VISEINTE.OBJ del BUILDTC\VISEINTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VISEINTE.OBJ goto error

echo [302/416] Compiling SRC\MAIN\V\T\GUI\VIEWPORT\VIEINTTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\VIEINTTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\VIEWPORT\VIEINTTE.CPP >> TC.RSP
if exist BUILDTC\VIEINTTE.OBJ del BUILDTC\VIEINTTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\VIEINTTE.OBJ goto error

echo [303/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\GIZSOLTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GIZSOLTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\GIZSOLTE.CPP >> TC.RSP
if exist BUILDTC\GIZSOLTE.OBJ del BUILDTC\GIZSOLTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GIZSOLTE.OBJ goto error

echo [304/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\GIVEARBU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GIVEARBU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\GIVEARBU.CPP >> TC.RSP
if exist BUILDTC\GIVEARBU.OBJ del BUILDTC\GIVEARBU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GIVEARBU.OBJ goto error

echo [305/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\INPUGIZM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INPUGIZM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\INPUGIZM.CPP >> TC.RSP
if exist BUILDTC\INPUGIZM.OBJ del BUILDTC\INPUGIZM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\INPUGIZM.OBJ goto error

echo [306/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\INGIVACR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INGIVACR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\INGIVACR.CPP >> TC.RSP
if exist BUILDTC\INGIVACR.OBJ del BUILDTC\INGIVACR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\INGIVACR.OBJ goto error

echo [307/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\REFFRAGI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\REFFRAGI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\REFFRAGI.CPP >> TC.RSP
if exist BUILDTC\REFFRAGI.OBJ del BUILDTC\REFFRAGI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\REFFRAGI.OBJ goto error

echo [308/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\TRANGIZM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRANGIZM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\TRANGIZM.CPP >> TC.RSP
if exist BUILDTC\TRANGIZM.OBJ del BUILDTC\TRANGIZM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRANGIZM.OBJ goto error

echo [309/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\TRGIINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRGIINTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\TRGIINTE.CPP >> TC.RSP
if exist BUILDTC\TRGIINTE.OBJ del BUILDTC\TRGIINTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRGIINTE.OBJ goto error

echo [310/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\ROTAGIZM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ROTAGIZM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\ROTAGIZM.CPP >> TC.RSP
if exist BUILDTC\ROTAGIZM.OBJ del BUILDTC\ROTAGIZM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\ROTAGIZM.OBJ goto error

echo [311/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\ROGIINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ROGIINTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\ROGIINTE.CPP >> TC.RSP
if exist BUILDTC\ROGIINTE.OBJ del BUILDTC\ROGIINTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\ROGIINTE.OBJ goto error

echo [312/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\CACOGIZM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CACOGIZM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\CACOGIZM.CPP >> TC.RSP
if exist BUILDTC\CACOGIZM.OBJ del BUILDTC\CACOGIZM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CACOGIZM.OBJ goto error

echo [313/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\CACOGIAN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CACOGIAN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\CACOGIAN.CPP >> TC.RSP
if exist BUILDTC\CACOGIAN.OBJ del BUILDTC\CACOGIAN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CACOGIAN.OBJ goto error

echo [314/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\CCGIINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CCGIINTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\CCGIINTE.CPP >> TC.RSP
if exist BUILDTC\CCGIINTE.OBJ del BUILDTC\CCGIINTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CCGIINTE.OBJ goto error

echo [315/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\SCALGIZM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SCALGIZM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\SCALGIZM.CPP >> TC.RSP
if exist BUILDTC\SCALGIZM.OBJ del BUILDTC\SCALGIZM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SCALGIZM.OBJ goto error

echo [316/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\SCGIINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SCGIINTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\SCGIINTE.CPP >> TC.RSP
if exist BUILDTC\SCGIINTE.OBJ del BUILDTC\SCGIINTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SCGIINTE.OBJ goto error

echo [317/416] Compiling SRC\MAIN\V\T\GUI\PRESELEM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRESELEM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\PRESELEM.CPP >> TC.RSP
if exist BUILDTC\PRESELEM.OBJ del BUILDTC\PRESELEM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PRESELEM.OBJ goto error

echo [318/416] Compiling SRC\MAIN\V\T\ENVI\BACKGROU\BACKGROU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BACKGROU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\BACKGROU\BACKGROU.CPP >> TC.RSP
if exist BUILDTC\BACKGROU.OBJ del BUILDTC\BACKGROU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BACKGROU.OBJ goto error

echo [319/416] Compiling SRC\MAIN\V\T\MEDIA\MEDIENTI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\MEDIENTI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\MEDIA\MEDIENTI.CPP >> TC.RSP
if exist BUILDTC\MEDIENTI.OBJ del BUILDTC\MEDIENTI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\MEDIENTI.OBJ goto error

echo [320/416] Compiling SRC\MAIN\V\T\GUI\CAMECONT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CAMECONT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\CAMECONT.CPP >> TC.RSP
if exist BUILDTC\CAMECONT.OBJ del BUILDTC\CAMECONT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CAMECONT.OBJ goto error

echo [321/416] Compiling SRC\MAIN\V\T\GUI\COMMLIST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\COMMLIST.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\COMMLIST.CPP >> TC.RSP
if exist BUILDTC\COMMLIST.OBJ del BUILDTC\COMMLIST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\COMMLIST.OBJ goto error

echo [322/416] Compiling SRC\MAIN\V\T\GUI\CONTROLL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CONTROLL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\CONTROLL.CPP >> TC.RSP
if exist BUILDTC\CONTROLL.OBJ del BUILDTC\CONTROLL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CONTROLL.OBJ goto error

echo [323/416] Compiling SRC\MAIN\V\T\GUI\GIZMO\GIZMO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GIZMO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\GIZMO\GIZMO.CPP >> TC.RSP
if exist BUILDTC\GIZMO.OBJ del BUILDTC\GIZMO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GIZMO.OBJ goto error

echo [324/416] Compiling SRC\MAIN\V\T\GUI\FEEDBACK\PROGMONI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PROGMONI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\FEEDBACK\PROGMONI.CPP >> TC.RSP
if exist BUILDTC\PROGMONI.OBJ del BUILDTC\PROGMONI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PROGMONI.OBJ goto error

echo [325/416] Compiling SRC\MAIN\V\T\GUI\WIDGET\WIDGELEM.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGELEM.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\WIDGET\WIDGELEM.CPP >> TC.RSP
if exist BUILDTC\WIDGELEM.OBJ del BUILDTC\WIDGELEM.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WIDGELEM.OBJ goto error

echo [326/416] Compiling SRC\MAIN\V\T\GUI\WIDGET\VARIABLE\WIDGVARI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\WIDGVARI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\GUI\WIDGET\VARIABLE\WIDGVARI.CPP >> TC.RSP
if exist BUILDTC\WIDGVARI.OBJ del BUILDTC\WIDGVARI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\WIDGVARI.OBJ goto error

echo [327/416] Compiling SRC\MAIN\V\T\COMMON\NUMERICS\FLOAINTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\FLOAINTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\COMMON\NUMERICS\FLOAINTE.CPP >> TC.RSP
if exist BUILDTC\FLOAINTE.OBJ del BUILDTC\FLOAINTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\FLOAINTE.OBJ goto error

echo [328/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYCROSS.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYCROSS.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYCROSS.CPP >> TC.RSP
if exist BUILDTC\RAYCROSS.OBJ del BUILDTC\RAYCROSS.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYCROSS.OBJ goto error

echo [329/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYCROQU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYCROQU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYCROQU.CPP >> TC.RSP
if exist BUILDTC\RAYCROQU.OBJ del BUILDTC\RAYCROQU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYCROQU.OBJ goto error

echo [330/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYCROSI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYCROSI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYCROSI.CPP >> TC.RSP
if exist BUILDTC\RAYCROSI.OBJ del BUILDTC\RAYCROSI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYCROSI.OBJ goto error

echo [331/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYBOUND.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYBOUND.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYBOUND.CPP >> TC.RSP
if exist BUILDTC\RAYBOUND.OBJ del BUILDTC\RAYBOUND.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYBOUND.OBJ goto error

echo [332/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYSOLBO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYSOLBO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYSOLBO.CPP >> TC.RSP
if exist BUILDTC\RAYSOLBO.OBJ del BUILDTC\RAYSOLBO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYSOLBO.OBJ goto error

echo [333/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYSOLCL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYSOLCL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYSOLCL.CPP >> TC.RSP
if exist BUILDTC\RAYSOLCL.OBJ del BUILDTC\RAYSOLCL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYSOLCL.OBJ goto error

echo [334/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYINSPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYINSPA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYINSPA.CPP >> TC.RSP
if exist BUILDTC\RAYINSPA.OBJ del BUILDTC\RAYINSPA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYINSPA.OBJ goto error

echo [335/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYINSST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYINSST.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\RAYINSST.CPP >> TC.RSP
if exist BUILDTC\RAYINSST.OBJ del BUILDTC\RAYINSST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYINSST.OBJ goto error

echo [336/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\GEOMBOUN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GEOMBOUN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\GEOMBOUN.CPP >> TC.RSP
if exist BUILDTC\GEOMBOUN.OBJ del BUILDTC\GEOMBOUN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GEOMBOUN.OBJ goto error

echo [337/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\GEOQUEWO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GEOQUEWO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\ELEMENT\GEOQUEWO.CPP >> TC.RSP
if exist BUILDTC\GEOQUEWO.OBJ del BUILDTC\GEOQUEWO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GEOQUEWO.OBJ goto error

echo [338/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\TRMEININ.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRMEININ.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\TRMEININ.CPP >> TC.RSP
if exist BUILDTC\TRMEININ.OBJ del BUILDTC\TRMEININ.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRMEININ.OBJ goto error

echo [339/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\AFFELLDI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AFFELLDI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\AFFELLDI.CPP >> TC.RSP
if exist BUILDTC\AFFELLDI.OBJ del BUILDTC\AFFELLDI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\AFFELLDI.OBJ goto error

echo [340/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\AFELDIBO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AFELDIBO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\AFELDIBO.CPP >> TC.RSP
if exist BUILDTC\AFELDIBO.OBJ del BUILDTC\AFELDIBO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\AFELDIBO.OBJ goto error

echo [341/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\AFELDUBO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AFELDUBO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\AFELDUBO.CPP >> TC.RSP
if exist BUILDTC\AFELDUBO.OBJ del BUILDTC\AFELDUBO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\AFELDUBO.OBJ goto error

echo [342/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\AFELMIDI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AFELMIDI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\AFELMIDI.CPP >> TC.RSP
if exist BUILDTC\AFELMIDI.OBJ del BUILDTC\AFELMIDI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\AFELMIDI.OBJ goto error

echo [343/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\ORTELLDI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\ORTELLDI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\ORTELLDI.CPP >> TC.RSP
if exist BUILDTC\ORTELLDI.OBJ del BUILDTC\ORTELLDI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\ORTELLDI.OBJ goto error

echo [344/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\QUAINTBO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\QUAINTBO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\QUAINTBO.CPP >> TC.RSP
if exist BUILDTC\QUAINTBO.OBJ del BUILDTC\QUAINTBO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\QUAINTBO.OBJ goto error

echo [345/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOEU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOEU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOEU.CPP >> TC.RSP
if exist BUILDTC\POBOSOEU.OBJ del BUILDTC\POBOSOEU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOEU.OBJ goto error

echo [346/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOFC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOFC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOFC.CPP >> TC.RSP
if exist BUILDTC\POBOSOFC.OBJ del BUILDTC\POBOSOFC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOFC.OBJ goto error

echo [347/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOMC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POBOSOMC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POBS\POBOSOMC.CPP >> TC.RSP
if exist BUILDTC\POBOSOMC.OBJ del BUILDTC\POBOSOMC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POBOSOMC.OBJ goto error

echo [348/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\AFFIPLAC.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AFFIPLAC.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\AFFIPLAC.CPP >> TC.RSP
if exist BUILDTC\AFFIPLAC.OBJ del BUILDTC\AFFIPLAC.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\AFFIPLAC.OBJ goto error

echo [349/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\AFFTOLMA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AFFTOLMA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\AFFTOLMA.CPP >> TC.RSP
if exist BUILDTC\AFFTOLMA.OBJ del BUILDTC\AFFTOLMA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\AFFTOLMA.OBJ goto error

echo [350/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\INRACRBU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\INRACRBU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\INRACRBU.CPP >> TC.RSP
if exist BUILDTC\INRACRBU.OBJ del BUILDTC\INRACRBU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\INRACRBU.OBJ goto error

echo [351/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\SCENSOLI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SCENSOLI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\SCENSOLI.CPP >> TC.RSP
if exist BUILDTC\SCENSOLI.OBJ del BUILDTC\SCENSOLI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SCENSOLI.OBJ goto error

echo [352/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\BVH\AABBINHI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AABBINHI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\BVH\AABBINHI.CPP >> TC.RSP
if exist BUILDTC\AABBINHI.OBJ del BUILDTC\AABBINHI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\AABBINHI.OBJ goto error

echo [353/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\BVH\TRIACCST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\TRIACCST.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\BVH\TRIACCST.CPP >> TC.RSP
if exist BUILDTC\TRIACCST.OBJ del BUILDTC\TRIACCST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\TRIACCST.OBJ goto error

echo [354/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\BVH\BINSAHHI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BINSAHHI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\BVH\BINSAHHI.CPP >> TC.RSP
if exist BUILDTC\BINSAHHI.OBJ del BUILDTC\BINSAHHI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BINSAHHI.OBJ goto error

echo [355/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\BVH\BISATRHI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BISATRHI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\BVH\BISATRHI.CPP >> TC.RSP
if exist BUILDTC\BISATRHI.OBJ del BUILDTC\BISATRHI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BISATRHI.OBJ goto error

echo [356/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\BVH\BISAINHI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BISAINHI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\BVH\BISAINHI.CPP >> TC.RSP
if exist BUILDTC\BISAINHI.OBJ del BUILDTC\BISAINHI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BISAINHI.OBJ goto error

echo [357/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\BVH\GRTRIHIE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\GRTRIHIE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\BVH\GRTRIHIE.CPP >> TC.RSP
if exist BUILDTC\GRTRIHIE.OBJ del BUILDTC\GRTRIHIE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\GRTRIHIE.OBJ goto error

echo [358/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\BVH\AABBOUVO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AABBOUVO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\BVH\AABBOUVO.CPP >> TC.RSP
if exist BUILDTC\AABBOUVO.OBJ del BUILDTC\AABBOUVO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\AABBOUVO.OBJ goto error

echo [359/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\BVH\AXALBOBO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\AXALBOBO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\BVH\AXALBOBO.CPP >> TC.RSP
if exist BUILDTC\AXALBOBO.OBJ del BUILDTC\AXALBOBO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\AXALBOBO.OBJ goto error

echo [360/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\BVH\BOUVOLHI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BOUVOLHI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\BVH\BOUVOLHI.CPP >> TC.RSP
if exist BUILDTC\BOUVOLHI.OBJ del BUILDTC\BOUVOLHI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BOUVOLHI.OBJ goto error

echo [361/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\BVH\PRRAHIEL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PRRAHIEL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\BVH\PRRAHIEL.CPP >> TC.RSP
if exist BUILDTC\PRRAHIEL.OBJ del BUILDTC\PRRAHIEL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PRRAHIEL.OBJ goto error

echo [362/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\BVH\RAYSLARE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\RAYSLARE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\BVH\RAYSLARE.CPP >> TC.RSP
if exist BUILDTC\RAYSLARE.OBJ del BUILDTC\RAYSLARE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\RAYSLARE.OBJ goto error

echo [363/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\SURFACE\SUBBEZPA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SUBBEZPA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\SURFACE\SUBBEZPA.CPP >> TC.RSP
if exist BUILDTC\SUBBEZPA.OBJ del BUILDTC\SUBBEZPA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SUBBEZPA.OBJ goto error

echo [364/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\BLOBCOMP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BLOBCOMP.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\BLOBCOMP.CPP >> TC.RSP
if exist BUILDTC\BLOBCOMP.OBJ del BUILDTC\BLOBCOMP.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BLOBCOMP.OBJ goto error

echo [365/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\BLOBSOLI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BLOBSOLI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\BLOBSOLI.CPP >> TC.RSP
if exist BUILDTC\BLOBSOLI.OBJ del BUILDTC\BLOBSOLI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BLOBSOLI.OBJ goto error

echo [366/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\HEIFIESO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\HEIFIESO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\HEIFIESO.CPP >> TC.RSP
if exist BUILDTC\HEIFIESO.OBJ del BUILDTC\HEIFIESO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\HEIFIESO.OBJ goto error

echo [367/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\IMPLSOLI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\IMPLSOLI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\IMPLSOLI.CPP >> TC.RSP
if exist BUILDTC\IMPLSOLI.OBJ del BUILDTC\IMPLSOLI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\IMPLSOLI.OBJ goto error

echo [368/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\PLAHALSP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\PLAHALSP.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\PLAHALSP.CPP >> TC.RSP
if exist BUILDTC\PLAHALSP.OBJ del BUILDTC\PLAHALSP.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\PLAHALSP.OBJ goto error

echo [369/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POLYSURF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\POLYSURF.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\POLYSURF.CPP >> TC.RSP
if exist BUILDTC\POLYSURF.OBJ del BUILDTC\POLYSURF.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\POLYSURF.OBJ goto error

echo [370/416] Compiling SRC\MAIN\V\T\ENVI\GEOM\VOLUM\QUADSURF.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\QUADSURF.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\GEOM\VOLUM\QUADSURF.CPP >> TC.RSP
if exist BUILDTC\QUADSURF.OBJ del BUILDTC\QUADSURF.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\QUADSURF.OBJ goto error

echo [371/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\READALTI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\READALTI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\READALTI.CPP >> TC.RSP
if exist BUILDTC\READALTI.OBJ del BUILDTC\READALTI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\READALTI.OBJ goto error

echo [372/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\REMIQUGR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\REMIQUGR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\REMIQUGR.CPP >> TC.RSP
if exist BUILDTC\REMIQUGR.OBJ del BUILDTC\REMIQUGR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\REMIQUGR.OBJ goto error

echo [373/416] Compiling SRC\MAIN\V\T\IO\GEOMETRY\REMITRGR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\REMITRGR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\IO\GEOMETRY\REMITRGR.CPP >> TC.RSP
if exist BUILDTC\REMITRGR.OBJ del BUILDTC\REMITRGR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\REMITRGR.OBJ goto error

echo [374/416] Compiling SRC\MAIN\V\T\PROCESSI\BEZPATTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\BEZPATTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\PROCESSI\BEZPATTE.CPP >> TC.RSP
if exist BUILDTC\BEZPATTE.OBJ del BUILDTC\BEZPATTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\BEZPATTE.OBJ goto error

echo [375/416] Compiling SRC\MAIN\V\T\PROCESSI\SOBOTERE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SOBOTERE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\PROCESSI\SOBOTERE.CPP >> TC.RSP
if exist BUILDTC\SOBOTERE.OBJ del BUILDTC\SOBOTERE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SOBOTERE.OBJ goto error

echo [376/416] Compiling SRC\MAIN\V\T\PROCESSI\SOLBOUTE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SOLBOUTE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\PROCESSI\SOLBOUTE.CPP >> TC.RSP
if exist BUILDTC\SOLBOUTE.OBJ del BUILDTC\SOLBOUTE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SOLBOUTE.OBJ goto error

echo [377/416] Compiling SRC\MAIN\V\T\RENDER\RAYTRACI\SNELREFR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SNELREFR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\RAYTRACI\SNELREFR.CPP >> TC.RSP
if exist BUILDTC\SNELREFR.OBJ del BUILDTC\SNELREFR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SNELREFR.OBJ goto error

echo [378/416] Compiling SRC\MAIN\V\T\RENDER\SHADERS\SHADTRAN.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\SHADTRAN.OBJ >> TC.RSP
echo SRC\MAIN\V\T\RENDER\SHADERS\SHADTRAN.CPP >> TC.RSP
if exist BUILDTC\SHADTRAN.OBJ del BUILDTC\SHADTRAN.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\SHADTRAN.OBJ goto error

echo [379/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGBODY.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGBODY.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGBODY.CPP >> TC.RSP
if exist BUILDTC\CSGBODY.OBJ del BUILDTC\CSGBODY.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGBODY.OBJ goto error

echo [380/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGBODOP.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGBODOP.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGBODOP.CPP >> TC.RSP
if exist BUILDTC\CSGBODOP.OBJ del BUILDTC\CSGBODOP.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGBODOP.OBJ goto error

echo [381/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSCRMEFI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSCRMEFI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSCRMEFI.CPP >> TC.RSP
if exist BUILDTC\CSCRMEFI.OBJ del BUILDTC\CSCRMEFI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSCRMEFI.OBJ goto error

echo [382/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGDYADI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGDYADI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGDYADI.CPP >> TC.RSP
if exist BUILDTC\CSGDYADI.OBJ del BUILDTC\CSGDYADI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGDYADI.OBJ goto error

echo [383/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGDYASO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGDYASO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGDYASO.CPP >> TC.RSP
if exist BUILDTC\CSGDYASO.OBJ del BUILDTC\CSGDYASO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGDYASO.OBJ goto error

echo [384/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGDYATR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGDYATR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGDYATR.CPP >> TC.RSP
if exist BUILDTC\CSGDYATR.OBJ del BUILDTC\CSGDYATR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGDYATR.OBJ goto error

echo [385/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXAAR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGEXAAR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXAAR.CPP >> TC.RSP
if exist BUILDTC\CSGEXAAR.OBJ del BUILDTC\CSGEXAAR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGEXAAR.OBJ goto error

echo [386/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXAPL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGEXAPL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXAPL.CPP >> TC.RSP
if exist BUILDTC\CSGEXAPL.OBJ del BUILDTC\CSGEXAPL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGEXAPL.OBJ goto error

echo [387/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXAQU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGEXAQU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXAQU.CPP >> TC.RSP
if exist BUILDTC\CSGEXAQU.OBJ del BUILDTC\CSGEXAQU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGEXAQU.OBJ goto error

echo [388/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXATR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGEXATR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXATR.CPP >> TC.RSP
if exist BUILDTC\CSGEXATR.OBJ del BUILDTC\CSGEXATR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGEXATR.OBJ goto error

echo [389/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXPRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGEXPRE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXPRE.CPP >> TC.RSP
if exist BUILDTC\CSGEXPRE.OBJ del BUILDTC\CSGEXPRE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGEXPRE.OBJ goto error

echo [390/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXPEQ.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGEXPEQ.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGEXPEQ.CPP >> TC.RSP
if exist BUILDTC\CSGEXPEQ.OBJ del BUILDTC\CSGEXPEQ.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGEXPEQ.OBJ goto error

echo [391/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGOPEVI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGOPEVI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGOPEVI.CPP >> TC.RSP
if exist BUILDTC\CSGOPEVI.OBJ del BUILDTC\CSGOPEVI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGOPEVI.OBJ goto error

echo [392/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPLAEX.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGPLAEX.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPLAEX.CPP >> TC.RSP
if exist BUILDTC\CSGPLAEX.OBJ del BUILDTC\CSGPLAEX.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGPLAEX.OBJ goto error

echo [393/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOIAT.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGPOIAT.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOIAT.CPP >> TC.RSP
if exist BUILDTC\CSGPOIAT.OBJ del BUILDTC\CSGPOIAT.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGPOIAT.OBJ goto error

echo [394/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOICA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGPOICA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOICA.CPP >> TC.RSP
if exist BUILDTC\CSGPOICA.OBJ del BUILDTC\CSGPOICA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGPOICA.OBJ goto error

echo [395/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOICL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGPOICL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOICL.CPP >> TC.RSP
if exist BUILDTC\CSGPOICL.OBJ del BUILDTC\CSGPOICL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGPOICL.OBJ goto error

echo [396/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOIEV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGPOIEV.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOIEV.CPP >> TC.RSP
if exist BUILDTC\CSGPOIEV.OBJ del BUILDTC\CSGPOIEV.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGPOIEV.OBJ goto error

echo [397/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOIPR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGPOIPR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOIPR.CPP >> TC.RSP
if exist BUILDTC\CSGPOIPR.OBJ del BUILDTC\CSGPOIPR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGPOIPR.OBJ goto error

echo [398/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOIWI.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGPOIWI.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOIWI.CPP >> TC.RSP
if exist BUILDTC\CSGPOIWI.OBJ del BUILDTC\CSGPOIWI.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGPOIWI.OBJ goto error

echo [399/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOIWO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGPOIWO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGPOIWO.CPP >> TC.RSP
if exist BUILDTC\CSGPOIWO.OBJ del BUILDTC\CSGPOIWO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGPOIWO.OBJ goto error

echo [400/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGRAYCL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGRAYCL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGRAYCL.CPP >> TC.RSP
if exist BUILDTC\CSGRAYCL.OBJ del BUILDTC\CSGRAYCL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGRAYCL.OBJ goto error

echo [401/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRAEVBA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSRAEVBA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRAEVBA.CPP >> TC.RSP
if exist BUILDTC\CSRAEVBA.OBJ del BUILDTC\CSRAEVBA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSRAEVBA.OBJ goto error

echo [402/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRAEVCU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSRAEVCU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRAEVCU.CPP >> TC.RSP
if exist BUILDTC\CSRAEVCU.OBJ del BUILDTC\CSRAEVCU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSRAEVCU.OBJ goto error

echo [403/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRAEVGR.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSRAEVGR.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRAEVGR.CPP >> TC.RSP
if exist BUILDTC\CSRAEVGR.OBJ del BUILDTC\CSRAEVGR.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSRAEVGR.OBJ goto error

echo [404/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRAEVQU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSRAEVQU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRAEVQU.CPP >> TC.RSP
if exist BUILDTC\CSRAEVQU.OBJ del BUILDTC\CSRAEVQU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSRAEVQU.OBJ goto error

echo [405/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRAEVRE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSRAEVRE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRAEVRE.CPP >> TC.RSP
if exist BUILDTC\CSRAEVRE.OBJ del BUILDTC\CSRAEVRE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSRAEVRE.OBJ goto error

echo [406/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGRAYLE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGRAYLE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGRAYLE.CPP >> TC.RSP
if exist BUILDTC\CSGRAYLE.OBJ del BUILDTC\CSGRAYLE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGRAYLE.OBJ goto error

echo [407/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRALEBA.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSRALEBA.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRALEBA.CPP >> TC.RSP
if exist BUILDTC\CSRALEBA.OBJ del BUILDTC\CSRALEBA.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSRALEBA.OBJ goto error

echo [408/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRALEQU.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSRALEQU.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRALEQU.CPP >> TC.RSP
if exist BUILDTC\CSRALEQU.OBJ del BUILDTC\CSRALEQU.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSRALEQU.OBJ goto error

echo [409/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGRAYPL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGRAYPL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGRAYPL.CPP >> TC.RSP
if exist BUILDTC\CSGRAYPL.OBJ del BUILDTC\CSGRAYPL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGRAYPL.OBJ goto error

echo [410/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRASUEV.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSRASUEV.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRASUEV.CPP >> TC.RSP
if exist BUILDTC\CSRASUEV.OBJ del BUILDTC\CSRASUEV.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSRASUEV.OBJ goto error

echo [411/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGRAYWO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGRAYWO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGRAYWO.CPP >> TC.RSP
if exist BUILDTC\CSGRAYWO.OBJ del BUILDTC\CSGRAYWO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGRAYWO.OBJ goto error

echo [412/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRORACL.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSRORACL.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSRORACL.CPP >> TC.RSP
if exist BUILDTC\CSRORACL.OBJ del BUILDTC\CSRORACL.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSRORACL.OBJ goto error

echo [413/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGSOLBO.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGSOLBO.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGSOLBO.CPP >> TC.RSP
if exist BUILDTC\CSGSOLBO.OBJ del BUILDTC\CSGSOLBO.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGSOLBO.OBJ goto error

echo [414/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGSOLEQ.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGSOLEQ.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGSOLEQ.CPP >> TC.RSP
if exist BUILDTC\CSGSOLEQ.OBJ del BUILDTC\CSGSOLEQ.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGSOLEQ.OBJ goto error

echo [415/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGTREE.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGTREE.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGTREE.CPP >> TC.RSP
if exist BUILDTC\CSGTREE.OBJ del BUILDTC\CSGTREE.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGTREE.OBJ goto error

echo [416/416] Compiling SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGVERST.CPP
echo -c > TC.RSP
echo -ml >> TC.RSP
echo -f87 >> TC.RSP
echo -O1 >> TC.RSP
echo -d >> TC.RSP
echo -v- >> TC.RSP
echo -vi- >> TC.RSP
echo -w-inl >> TC.RSP
echo -DVITRAL_WITHOUT_PTHREADS >> TC.RSP
echo -DTURBO_C_286_ENABLED >> TC.RSP
echo -IC:\TURBOC\INCLUDE >> TC.RSP
echo -I..\DOS\SRC\MAIN >> TC.RSP
echo -I..\DOS\SRC\MAIN\V\KERNEL\286 >> TC.RSP
echo -ISRC;SRC\MAIN >> TC.RSP
echo -oBUILDTC\CSGVERST.OBJ >> TC.RSP
echo SRC\MAIN\V\T\ENVI\SCENE\CSG\CSGVERST.CPP >> TC.RSP
if exist BUILDTC\CSGVERST.OBJ del BUILDTC\CSGVERST.OBJ
tcc @TC.RSP > TC.LOG
if errorlevel 1 goto error
if not exist BUILDTC\CSGVERST.OBJ goto error

echo Base objects compiled successfully.
goto end
:error
type TC.LOG
echo Build failed. See the compiler output above.
:end

