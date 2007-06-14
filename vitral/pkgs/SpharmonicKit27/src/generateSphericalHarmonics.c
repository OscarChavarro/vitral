#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "FST_semi_disk.h"
#include "cospmls.h"

/*=========================================================================*/

#define MAX_LINE 1024

void
importPPM(char *filename, double *data, int size)
{
    int xsize = 0, ysize = 0;
    int i, j;
    char buffer[MAX_LINE + 1];
    char *ptr = NULL;
    int tipo = 6;
    unsigned char r, g, b;
    int index;
    FILE *fd;
    double val;

    /*- Verificacion de que el archivo esta bien --------------------------*/
    fd = fopen(filename, "rb");
    if ( !fd ) {
        fprintf(stderr, "<IMAGEN_RGB> ERROR: No se puede abrir el archivo.\n");
        fflush(stderr);
        exit(1);
    }

    /*- Lea y procese el encabezado ppm ----------------------------------*/
    for ( i = 0; !feof(fd); i++ ) {
        if ( i >= MAX_LINE ) i = 0;
        fread(&buffer[i], sizeof(unsigned char), 1, fd);
        buffer[i + 1] = '\0';

        if ( buffer[i] == '\n' && buffer[0] == 'P' && buffer[1] == '5' ) {
            tipo = 5;
        }

        if ( buffer[i] == '\n' && strcmp(buffer, "255\n") == 0 ) {
            break;
        }
        else if ( buffer[i] == '\n' ) {
            ptr = strtok(buffer, " \n");
            if ( ptr ) {
                xsize = atoi(ptr);
                ptr = strtok(NULL, " \n");
                if ( ptr ) {
                    ysize = atoi(ptr);
                }
            }
            i = -1;
        }          
    }

    /*- Verifique que el tamanno de la imagen sea coherente ---------------*/
    if ( xsize <= 0 || xsize > 2048 || ysize <=0 || ysize > 2048 ) {
        fprintf(stderr,
                "<IMAGEN_RGB> Imagen con tamanno %d X %d no aceptada.\n",
                xsize, ysize);
        fflush(stderr);
        exit(1);
    }

    /*- Cree la imagen y lea sus pixels del archivo ppm -------------------*/
    if ( xsize != size || ysize != size ) {
        fprintf(stderr, "Incorrect size.\n");
        exit(1);
    }

    if ( tipo == 6 ) {
        index = 0;
        for ( i = ysize - 1; i >= 0 ; i-- ) {
            for ( j = 0; j < xsize ; j++ ) {
                fread(&r, sizeof(unsigned char), 1, fd);
                fread(&g, sizeof(unsigned char), 1, fd);
                fread(&b, sizeof(unsigned char), 1, fd);
                val = ((double)r + (double)g + (double)b) / (3*255);
                data[index] = val;
                index++;
            }
        }
    }
}

void
exportPPM(char *filename, double *data, int size)
{
    unsigned char pixel;
    FILE *fd;
    int i;
    double min = 1e30;
    double max = -1e30;

    fd = fopen(filename, "wb");
    fprintf(fd, "P6\n%d %d\n255\n", size, size);
    for ( i = 0; i < size*size; i++ ) {
        if ( data[i] > max ) max = data[i];
        if ( data[i] < min ) min = data[i];
    }

    for ( i = 0; i < size*size; i++ ) {
        pixel = (unsigned char)(((data[i] - min) / (max - min)) * 255);
        fwrite(&pixel, sizeof(unsigned char), 1, fd);
        fwrite(&pixel, sizeof(unsigned char), 1, fd);
        fwrite(&pixel, sizeof(unsigned char), 1, fd);
    }

    fclose(fd);
}

int main(int argc, char **argv)
{
    /*-----------------------------------------------------------------*/
    FILE *textFd;
    int i, bw, size;
    double *rdata, *idata, *rresult, *iresult;
    double *workspace, *disk_array;

    if (argc != 2) {
        fprintf(stdout,"Usage: %s imageFile.ppm\n", argv[0]);
        exit(0);
    }

    bw = 32;
    size = 2*bw;
    rdata = (double *)malloc(sizeof(double) * (size * size));
    idata = (double *)malloc(sizeof(double) * (size * size));
    rresult = (double *)malloc(sizeof(double) * (bw * bw));
    iresult = (double *)malloc(sizeof(double) * (bw * bw));
    workspace = (double *)malloc(sizeof(double) * 
                                  ((8 * (bw*bw)) + 
                                   (33 * bw)));
    disk_array = (double *)malloc(sizeof(double) *
                                   2 * TableSize(0,bw));

    if ( (rdata == NULL) || (idata == NULL) ||
         (rresult == NULL) || (iresult == NULL) ||
         (workspace == NULL) ||
         (disk_array == NULL) ) {
        perror("Error in allocating memory");
        exit( 1 ) ;
    }

    /* now do the forward spherical transform */
    importPPM(argv[1], rdata, size);

    /*-----------------------------------------------------------------*/
    FST_semi_disk(rdata, /* Input image in spatial range */
                  idata,
                  rresult,
                  iresult,
                  size,
                  workspace,
                  1, /* Data is real (not complex) */
                  disk_array); /* precomputed spherical harmonics calculation data */

    /*-----------------------------------------------------------------*/
    textFd = fopen("output.txt", "wb");
    for ( i = 0; i < bw*bw && i < 16; i++ ) {
        fprintf(textFd, "%f, %f\n", rresult[i], iresult[i]);
    }    
    fclose(textFd);

    InvFST_semi_disk(rresult, iresult, 
                     rdata, idata, 
                     size,
                     workspace,
                     1,
                     disk_array);
    exportPPM("output.ppm", rdata, size);


    free(rdata);
    free(idata);
    free(disk_array);
    free(workspace);
    free(rresult);
    free(iresult);

    return 0;
}

/*===========================================================================
= EOF                                                                       =
===========================================================================*/
