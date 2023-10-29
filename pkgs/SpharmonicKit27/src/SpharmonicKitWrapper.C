/*===========================================================================
=---------------------------------------------------------------------------=
= Module history:                                                           =
= - May 21 2007 - Oscar Chavarro: Original base version                     =
===========================================================================*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "FST_semi_disk.h"
#include "cospmls.h"

#include <jni.h>
//#include "vsdk_toolkit_processing_SpharmonicKitWrapper.h"

/*=========================================================================*/

/*
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
*/

extern "C" JNIEXPORT jboolean JNICALL
Java_vsdk_toolkit_processing_SpharmonicKitWrapper_executeSphericalHarmonics(
    JNIEnv *env, jclass,
    jbyteArray inImage,
    jdoubleArray outSphericalHarmonicsR,
    jdoubleArray outSphericalHarmonicsI)
{
    /*-----------------------------------------------------------------*/
    /*FILE *textFd;*/
    int i, bw, size;
    double *rdata, *idata, *rresult, *iresult;
    double *workspace, *disk_array;

    bw = 32;
    size = 2*bw;
    rdata = (double *)calloc(1, sizeof(double) * (size * size));
    idata = (double *)calloc(1, sizeof(double) * (size * size));
    rresult = (double *)calloc(1, sizeof(double) * (bw * bw));
    iresult = (double *)calloc(1, sizeof(double) * (bw * bw));
    workspace = (double *)calloc(1, sizeof(double) * 
                                  ((8 * (bw*bw)) + 
                                   (33 * bw)));
    disk_array = (double *)calloc(1, sizeof(double) *
                                   2 * TableSize(0,bw));

    /*-----------------------------------------------------------------*/
    jbyte *javaArray;
    javaArray = env->GetByteArrayElements(inImage, JNI_FALSE);
    for ( i = 0; i < env->GetArrayLength(inImage); i++ ) {
        if ( javaArray[i] == 0 ) {
            rdata[i] = 0.0;
        }
        else {
            rdata[i] = 1.0;
        }
    }

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
/*
    textFd = fopen("output.txt", "wb");
    for ( i = 0; i < bw*bw && i < 16; i++ ) {
        fprintf(textFd, "%f, %f\n", rresult[i], iresult[i]);
    }    
    fclose(textFd);
*/
    /*---------------------------------------------------------------*/
    for ( i = 0;
          i < bw*bw && i < env->GetArrayLength(outSphericalHarmonicsI);
          i++ ) {
        env->SetDoubleArrayRegion(outSphericalHarmonicsR, i, 1, &rresult[i]);
        env->SetDoubleArrayRegion(outSphericalHarmonicsI, i, 1, &iresult[i]);
    }
    /*---------------------------------------------------------------*/
/*
    InvFST_semi_disk(rresult, iresult, 
                     rdata, idata, 
                     size,
                     workspace,
                     1,
                     disk_array);
    exportPPM("output.ppm", rdata, size);
*/
    /*---------------------------------------------------------------*/

    free(rdata);
    free(idata);
    free(disk_array);
    free(workspace);
    free(rresult);
    free(iresult);

    return JNI_TRUE;
}

/*===========================================================================
= EOF                                                                       =
===========================================================================*/
