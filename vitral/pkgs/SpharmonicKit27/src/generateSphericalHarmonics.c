#include <errno.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "FST_semi_disk.h"
#include "cospmls.h"
#include "primitive_FST.h"

#define max(A, B) ((A) > (B) ? (A) : (B))

/**************************************************************/
/**************************************************************/

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
    printf("MIN: %.2f, MAX: %.2f\n", min, max);
}

int main(int argc, char **argv)
{
    FILE *errorsfp, *textFd;
    int i, j, l, m, bw, size, dummy;
    double *rcoeffs, *icoeffs, *rdata, *idata, *rresult, *iresult;
    double *workspace, *disk_array;
    double *relerror, *curmax, granderror, grandrelerror;
    double ave_error, ave_relerror;
    double stddev_error, stddev_relerror;
    double realtmp, imagtmp,origmag, tmpmag;
    double fudge;
    double *rcoeffs2, *icoeffs2;

    granderror = 0.0; grandrelerror = 0.0; 

    if (argc != 2) {
        fprintf(stdout,"Usage: %s imageFile.ppm\n", argv[0]);
        exit(0);
    }

    bw = 32;
    size = 2*bw;

    rcoeffs = (double *) malloc(sizeof(double) * (bw * bw));
    icoeffs = (double *) malloc(sizeof(double) * (bw * bw));
    rdata = (double *) malloc(sizeof(double) * (size * size));
    idata = (double *) malloc(sizeof(double) * (size * size));
    rresult = (double *) malloc(sizeof(double) * (bw * bw));
    iresult = (double *) malloc(sizeof(double) * (bw * bw));

    workspace = (double *) malloc(sizeof(double) * 
                                  ((8 * (bw*bw)) + 
                                   (33 * bw)));

    /* should be PLENTY of space */
    disk_array = (double *) malloc(sizeof(double) *
                                   2 * TableSize(0,bw)) ;



    relerror = (double *) malloc(sizeof(double));
    curmax = (double *) malloc(sizeof(double));


    /****
    At this point, check to see if all the memory has been
    allocated. If it has not, there's no point in going further.
    ****/
    if ( (rcoeffs == NULL) || (icoeffs == NULL) ||
         (rdata == NULL) || (idata == NULL) ||
         (rresult == NULL) || (iresult == NULL) ||
         (workspace == NULL) ||
         (disk_array == NULL) )
    {
        perror("Error in allocating memory");
        exit( 1 ) ;
    }

    /*** generate a seed, needed to generate random data ***/
    /**  Change for Version 2.6
    Since I want to calculate the error between the original
    coefficients and the calculated ones, I need space to copy the
    originals. Why? Because I'm renormalizing the coefficients
    inside the inverse transform. If I wasn't interested in the
    error, I wouldn't have to do this.    ***/

    rcoeffs2 = (double *) malloc(sizeof(double) * (bw * bw));
    icoeffs2 = (double *) malloc(sizeof(double) * (bw * bw));

    /* now do the forward spherical transform */
    printf("Array of [%dx%d] spherical function values...\n", size, size);

    importPPM(argv[1], rdata, size);

    FST_semi_disk(rdata, /* Input image in spatial range */
                  idata,
                  rresult,
                  iresult,
                  size,
                  workspace,
                  1, /* Data is real (not complex) */
                  disk_array); /* precomputed spherical harmonics calculation data */

    printf("1st 16 Harmonics (output from image): \n");

    for ( i = 0; i < bw*bw /*&& i < 16*/; i++ ) {
        printf("[%d]: <%.2f, %.2fi>\n", i, rresult[i], iresult[i]);
    }

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

    /*exportPPM("output.ppm", rdata, size);*/

    /* now to compute the error */
    relerror[i] = 0.0;
    curmax[i] = 0.0;
    for(j=0;j<(bw*bw);j++){
        realtmp = rresult[j]-rcoeffs[j];
        imagtmp = iresult[j]-icoeffs[j];
        origmag = sqrt((rcoeffs[j]*rcoeffs[j]) + (icoeffs[j]*icoeffs[j]));
        tmpmag  = sqrt((realtmp*realtmp) + (imagtmp*imagtmp));
        relerror[i] = max(relerror[i],tmpmag/(origmag + pow(10.0, -50.0)));
        curmax[i]  = max(curmax[i],tmpmag);
    }
    
    fprintf(stdout,"r-o error\t = %.12f\n", curmax[i]);
    fprintf(stdout,"(r-o)/o error\t = %.12f\n\n", relerror[i]);
    
    granderror += curmax[i];
    grandrelerror += relerror[i];

    ave_error = granderror;
    ave_relerror = grandrelerror;
    stddev_error = 0.0 ; stddev_relerror = 0.0;
  
    stddev_error += pow( ave_error - curmax[i] , 2.0 );
    stddev_relerror += pow( ave_relerror - relerror[i] , 2.0 );

    fprintf(stderr,"Program: test_FST_semi_disk\n");
    fprintf(stderr,"Bandwidth = %d\n", bw);
  
    if (argc == 4) {
        fudge = -1.0;
        errorsfp = fopen(argv[3],"w");
        for(m = 0 ; m < bw ; m++ )
        {
            fudge *= -1.0;
            for(l = m ; l< bw ; l++ )
            {
                dummy = seanindex(m,l,bw);
              
                fprintf(errorsfp,
                        "dummy = %d\t m = %d\tl = %d\t%.10f  %.10f\n",
                        dummy, m, l,
                        fabs(rcoeffs[dummy] - rresult[dummy]),
                        fabs(icoeffs[dummy] - iresult[dummy]));
              
                dummy = seanindex(-m,l,bw);
              
                fprintf(errorsfp,
                        "dummy = %d\t m = %d\tl = %d\t%.10f  %.10f\n",
                        dummy, -m, l,
                        fabs(rcoeffs[dummy] - rresult[dummy]),
                        fabs(icoeffs[dummy] - iresult[dummy]));
              
            }
        }
      
        fclose(errorsfp);
      
    }

    free(icoeffs2);
    free(rcoeffs2);
    free(curmax);
    free(relerror);
    free(disk_array);
    free(workspace);
    free(iresult);
    free(rresult);
    free(idata);
    free(rdata);
    free(icoeffs);
    free(rcoeffs);

    return 0;
}
