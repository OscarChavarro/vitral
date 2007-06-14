#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void
generar_archivo(int i)
{
    double a = (double)i;

    a = a * 3.14192 / 180;

    FILE *fd = fopen("/tmp/a.ray", "wt");

    fprintf(fd, "eye 2.1 1.3 1.7\n");
    fprintf(fd, "up 0 0 1\n");
    fprintf(fd, "lookat 0 0 0\n");
    fprintf(fd, "fov 45\n");
    fprintf(fd, "background 0.078 0.361 0.753\n");
    fprintf(fd, "rotation 0 %0.2f 0\n", a);
    fprintf(fd, "surface 1 0.75 0.33 0.15 1.0 0.0 1 0 0 1\n");
    fprintf(fd, "sphere 0 0 -100 99.5\n");
    fprintf(fd, "surface 0.5 0.45 0.35 0.3 1.0 1 3 0.5 0.0 1.0\n");
    fprintf(fd, "cylinder 0 0 0 0.5 0 1\n");
    fprintf(fd, "light 1 1 1 ambient\n");
    fprintf(fd, "light 0.4 0.4 0.4 point 4 3 2\n");
    fprintf(fd, "light 0.4 0.4 0.4 point 1 -4 4\n");
    fprintf(fd, "light 0.4 0.4 0.4 point -3 1 5\n");

    fclose(fd);
}

int
main()
{
    int i = 0;
    char comando[1024];

    for ( i = 0; i < 360; i+= 5 ) {
        generar_archivo(i);
        system("time java -Xms800m -Xmx800m -classpath ./classes:../../lib/vitral.jar:../../lib/vitral_transition.jar RaytracerSimple /tmp/a.ray");
        sprintf(comando, "mv salida.ppm salida%03d.ppm", i);
        system(comando);
    }
}
