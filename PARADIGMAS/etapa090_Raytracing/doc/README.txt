===========================================================================

Notas:
  - El proyecto original viene de un enunciado de proyecto del curso de
    computacion grafica de MIT. La documentacion (i.e. enunciado) original 
    esta aun en ./doc
  - Como el proyecto original de MIT fue hecho en la epoca de alguna version
    antigua de java (usa metodos deprecados en JDK1.1) y como java NO
    es compatible con sigo mismo, fue necesario realizar algunos cambios para
    que compilara bien en la version de jdk 1.4.2.
  - Como eso puede volver a pasar, NO se garantiza que el programita corra en
    futuras versiones de java
  - Dadas las limitaciones de "seguridad" de los applets, la carpeta de datos
    (etc) debe estar dentro del punto de ejecucion (classes) para no obtener
    un error de tipo "acceso denegado".  Por eso lo extranno de tener el etc
    dentro de classes (debe tener cuidado de no borrarse) y un lazo simbolico
    por fuera.

===========================================================================
COSAS QUE NO SE HAN ENTENDIDO
  - En VECTOR se usan modificadores "final" y "static" en los METODOS. Que
    implicaciones tiene eso? Acaso hace que algo ande mas rapido o algo
    asi? (hacer pruebas de desempenno!)

===========================================================================
SEMEJANZAS ENCONTRADAS ENTRE EL RAYTRACER DE MIT Y EL RAYTRACER CRIOLLO
DE AQUYNZA

  - Para las clases basicas (APLICACION, RAYTRACER_?, GEOMETRIA, ESFERA, LUZ, 
    MATERIAL, RAYO y VECTOR), existe una correspondencia 1 a 1 entre el
    rautracer de MIT (MIT) y AQUYNZA (AQZ).
  - Hay una jerarquia de GEOMETRIAs.
  - Las clases VECTOR, RAYO, GEOMETRIA y ESFERA tenian implementado un metodo
    "toString", aparentemente para permitir hacer su persistencia o su
    introspeccion en un patron de disenno de cadena de responsabilidad al
    estilo AQZ. Sin embargo, esos metodos no se estaban usando y fueron
    eliminados.
  - La clase "Vector" de java es exactamente la misma idea de la clase
    ARREGLO de AQUYNZA: un vector que puede crecer, implementando una
    especie de LISTA, que ademas crece en incrementos especificables por
    el usuario.

===========================================================================
DIFERENCIAS ENCONTRADAS ENTRE EL RAYTRACER DE MIT Y EL RAYTRACER CRIOLLO
DE AQUYNZA

  - En la esfera del raytracer de MIT (MIT) se representa la posicion de su
    centro, mientras que en AQUYNZA (AQZ) siempre es una esfera centrada
    en el origen a la cual se le aplican transformaciones geometricas.
  - La clase VECTOR original de MIT tiene varias versiones sobrecargas de
    operaciones, mas de las que hay en VECTOR de AQZ.  Sin embargo no todas
    se estaban usando y algunas se eliminaron. Ver en particular el uso de
    VECTOR::producto_punto.
  - La version original MIT SI calcula una IMAGEN, y la va escribiendo
    a un contexto grafico Java dentro de un applet... y lo hace en un
    thread aparte al que realiza el raytracing. Notese que puede ser
    interesante...
  - La version original MIT no tiene una clase CAMARA (usa variables en
    APLICACION) ni una clase COLOR (usa grupos de variables en MATERIAL).
    Eso si se puede decir que es mejor en AQZ. Gran parte de la funcionalidad
    que deberia ir en la camara estaba implementada en el RAYTRACER_MIT. En
    AQZ se ha logrado construir una clase CAMARA con una funcionalidad clara
    u util que al parecer no era obvia.
  - El color de fondo es una parte integral del modelo de la escena... obvio!
    ese es el valor por defecto que se aplica cuando la recursion del
    raytracing llega a su limite de niveles... -> Revisar como se esta
    manejando esto en AQZ.
  - Parte de la funcionalidad del raytracer (que deberia ir en RAYTRACER_MIT)
    estaba originalmente programada en la clase RAYO. En este aspecto AQZ es
    mas claro.
  - En la version de MIT modificada para tener una CAMARA, se encontro que
    ademas de _up se preprocesa un _down, para obtener las coordenadas en
    U (screenspace) que aumentan hacia abajo... tendra alguna ventaja eso?
  - En MIT con CAMARA, parece que se maneja mejor la etapa de preprocesamiento
    (implica una generacion de rayo con menos operaciones!)... REVISAR! ->
    . La direccion del rayo no se normaliza al final como en AQZ
    . Los vectores precalculados no son _right y _up (o _down), sino dx y
      dy... (i.e. ya van con el escalamiento de coordenadas mi_x, mi_y
      precalculado)
  - En MIT se hace clamp a 1 del resultado del color del pixel en 
    MATERIAL::Shade, mientras que en AQZ se hace dentro del ciclo interno de
    RAYTRACER_*::ejecutar. Notese que esto impacta el comportamiento de los
    rayos reflejados, pues en MIT tambien se minimizan los rayos reflejados
    que puedan quedar saturados...
  - Las operaciones de ESFERA en MIT no parecen usar directamente una
    ecuacion cuadratica, sino algebra de vectores. Es interesante entrar
    a analizar y comparar los ordenes de complejidad...
  - La distancia a la interseccion se almacena en el rayo... y parece que
    eso permite hacer optimizaciones... pero desafortunadamente eso rompe
    la idea de que la operacion de interseccion se pueda usar en un contexto
    generico, y no solo dentro de un esquema de ordenamiento por distancias
    dentro de una escena.  Sin embargo, es interesante pensarlo en el caso
    de la interseccion con esferas envolventes / fase 1.

GRANDES CONCLUSIONES PARA AQUYNZA:
  - El FONDO es necesario e importantisimo para el proceso de raytracing.
    RAYTRACING_CRIOLLO::ejecutar debe recibir una referencia al fondo, y 
    los FONDOs deben proveer una operacion que dado un rayo, den su color
    en para ese rayo.
  - El modelo de iluminacion (claro!) hace parte del material y NO del
    raytracer. Asi es que la clase MATERIAL debe implementar los SHADERS
    (y eso es consistente con por ejemplo la idea de insertar codigo CG
    dentro del MATERIAL). Aun debe pensarse bien en exactamente que
    informacion requiere un shader para funcionar, pues el MATERIAL no
    debe depender de la GEOMETRIA, asi es que el RAYTRACER debe resolver
    y entregar esa informacion (i.e. pensar en renderman). El unico
    problema con esa idea es como lidiar con la recursion del shader,
    sin que eso implique que el MATERIAL dependa de la GEOMETRIA y en si
    de la escena de objetos rayables... SIN EMBARGO POR AHORA LO MEJOR
    ES DEJAR EL ESQUEMA AQUYNZA INTACTO, Y DOCUMENTAR QUE "LA CLASE
    MATERIAL CONTIENE INFORMACION Y ALGORITMOS INDEPENDIENTES DE LOS
    DATOS DE ESCENA... LO QUE TENGA QUE VER CON MATERIAL PERO QUE
    SEA INTERDEPENDIENTE DE LA ESCENA SE ELEVA A NIVEL DE VISORES..."
  - Es interesante ver como el raytracer MIT divide la operacion de 
    interseccion en 2 fase (interseccion e informacion_extra). Puede ser
    que en el caso de la esfera esto signifique que el sistema vaya mas
    rapido.  Puede ser interesante tener la primera fase implementada en
    la ESFERA AQZ, y usarla solo para las pruebas de volumen con esfera
    envolvente para cuando se piense implementar eficiencia de busqueda
    en la operacion rayo/escena.

===========================================================================
IMPORTANTE: INSPECCIONAR EL COMPORTAMIENTO DEL PROGRAMA CON UN PROFILER
  - Informacion de JVMPI: 
      http://java.sun.com/j2se/1.4.2/docs/guide/jvmpi/jvmpi.html#hprof-heap
      http://access1.sun.com/techarticles/JVMPI.html
      http://www.scorbett.ca/projects/tutorials/java-profiling.shtml
  - En resumen:
    . JVMPI VIENE con el JDK, y es un API que detalla un mecanismo para
      hacerle profiling a programas java, de tal manera que el profiler
      y su frontend puedan cambiar modularmente.
    . El profiler por defecto (que viene con el JDK) es HPROF
    . Un frontend que ofrece SUN es PerfAnal y otro (uno bonito pero lento)
      es jProf.
    . Una linea de comandos recomendada:

java -classpath ./classes -Xrunhprof:heap=all,cpu=times,file=reporte_del_profiler.log APLICACION

    . Salen unos errores y aparece un archivo "reporte_del_profiler.log",
      que es como enredado... pero todo eso se puede hacer con solo el SDK.

    . Para ver bonito los resultados:

?



