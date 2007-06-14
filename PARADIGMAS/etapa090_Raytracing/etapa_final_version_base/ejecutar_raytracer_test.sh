#!/bin/sh
#make absurdo

# Ejecucion normal
cd testsuite/framework/visual
time make run

# Ejecucion + profiler
#time java -classpath ./classes -Xrunhprof:heap=all,cpu=times,file=reporte_del_profiler.log APLICACION $*

if [ -f ./salida.ppm ]; then
    display salida.ppm
fi

cd ../../..
