/*=========================================================================*/
/*
 * compress.c - File compression ala IEEE Computer, June 1984.
 *
 * Authors:
 *        Spencer W. Thomas    (decvax!utah-cs!thomas)
 *        Jim McKie        (decvax!mcvax!jim)
 *        Steve Davies        (decvax!vax135!petsd!peora!srd)
 *        Ken Turkowski        (decvax!decwrl!turtlevax!ken)
 *        James A. Woods        (decvax!ihnp4!ames!jaw)
 *        Joe Orost        (decvax!vax135!petsd!joe)
 * Code taken from BSD software distribution at:
 *     ftp://ftp.uu.net/systems/unix/bsd-sources/usr.bin
 * Adapted as JNI by Oscar Chavarro (May 2, 2008):
 *   - Original optimizations for old PDP11 and VAX computers where removed,
 *     only standard C (portable) implementation leaved.
 *   - Debugging code for printing messages removed.
 *   - Code was translated from K&R C to ANSI C
 *   - Code organized: all constants, macros definitions, type definitions
 *     and constants are in the beginning of the file, all the function
 *     definitions are on the end of the file.
 *   - On final version, there is only LZW functionality code.  Specific
 *     UNIX file handling routines where deleted (copystat function and
 *     main function details for handling files was ommited).
 *   - LZW functions modularized, main separated as external test program.
 * Note that LZW patent expired on 2003.
 */

#ifdef UNIX_ENABLED
#include <sys/stat.h>
#include <unistd.h>
#endif

#include <stdlib.h>
#include <string.h>

#include "compress.h"

#define MAXPATHLEN 4096

/*---------------------------------------------------------------------------*/
/*
Making this operations buffered will increase a lot LZW's algorithm performance
*/

size_t
myfread(void *data, size_t size, size_t n, void *descriptor)
{
    FILE *fd = (FILE *)descriptor;
    size_t r= fread(data, size, n, fd);

/*
    static int acum;
    acum+=r;
    printf("[%d] fread(%d, %d), nn: %d-> %c\n", acum, size, n, r, ((char*)data)[0]);
    fflush(stdout);
*/
    return r;
}

size_t
myfwrite(void *data, size_t size, size_t n, void *descriptor)
{
    FILE *fd = (FILE *)descriptor;
    return fwrite(data, size, n, fd);
}


void
myflush(void *descriptor)
{
    FILE *fd = (FILE *)descriptor;
    fflush(fd);
}

/*---------------------------------------------------------------------------*/

/**
Algorithm from "A Technique for High Performance Data Compression",
Terry A. Welch, IEEE Computer Vol 17, No 6 (June 1984), pp 8-19.

Usage: compress [-dfvc] [-b bits] [file ...]
Inputs:
    file:   File to be compressed.
Outputs:
   file.Z:   Compressed form of file 
Assumptions:
   When filenames are given, replaces with the compressed version
   (.Z suffix) only if the file decreases in size.
Algorithm:
    Modified Lempel-Ziv method (LZW).  Basically finds common
substrings and replaces them with a variable size code.  This is
deterministic, and can be done on the fly.  Thus, the decompression
procedure needs no input table, but tracks the way the table was built.
*/
int
main(int argc, char *argv[])
{
    #ifdef UNIX_ENABLED
    struct stat statbuf;
    long int fsize;
    #endif

    char **filename;
    char tempname[MAXPATHLEN];
    int do_decomp = 0;
    FILE *fdOut;
    FILE *fdIn = NULL;
    char ofname[100];

    /*----------------------------------------------------------------------*/
    if ( argc != 2 ) {
        fprintf(stderr, "Usage:\n\t%s file\n", argv[0]);
        fflush(stderr);
        return -1;
    }

    if ( strstr(argv[0], "uncompress") ) {
        do_decomp = 1;
    }

    filename = &(argv[1]);

    initLZW(-1, myfread, myfwrite, myflush);

    /*----------------------------------------------------------------------*/
    if ( do_decomp == 0 ) {
        /* PREPARE FOR COMPRESSION */
        if ( strcmp(*filename + strlen(*filename) - 2, ".Z") == 0 ) {
            fprintf(stderr, "%s: already has .Z suffix -- no change\n",
                *filename);
            return 4;
        }
        /* Open input file */
        fdIn = fopen(*filename, "rb");
        if ( !fdIn ) {
            perror(*filename);
            return 5;
        }

        /*
         * tune hash table size for small files -- ad hoc,
         * but the sizes match earlier #defines, which
         * serve as upper bounds on the number of output codes. 
         */
        #ifdef UNIX_ENABLED
        stat(*filename, &statbuf);
        fsize = (long)statbuf.st_size;
	initLZW(fsize, myfread, myfwrite, myflush);
        #endif

        /* Generate output filename */
        strcpy(ofname, *filename);
        strcat(ofname, ".Z");
      }
      else {
        /* PREPARE FOR DECOMPRESSION */
        /* Check for .Z suffix */
        if ( strcmp(*filename + strlen(*filename) - 2, ".Z" ) != 0) {
            /* No .Z: tack one on */
            strcpy(tempname, *filename);
            strcat(tempname, ".Z");
            *filename = tempname;
        }
        /* Open input file */
        fdIn = fopen(*filename, "rb");
        if ( !fdIn ) {
            perror(*filename);
            return 1;
        }
        /* Generate output filename */
        strcpy(ofname, *filename);
        ofname[strlen(*filename) - 2] = '\0';  /* Strip off .Z */
      }
    ;

    /*----------------------------------------------------------------------*/
    /* Open output file */
    fdOut = fopen(ofname, "wb");
    if ( !fdOut ) {
        perror(ofname);
        return 6;
    }

    /* Actually do the compression/decompression */
    if ( do_decomp == 0 ) {
        compress(fdIn, fdOut);
    }
    else {
        decompress(fdIn, fdOut);
    }
    fclose(fdOut);
    fclose(fdIn);

    return 0;
}

/*=========================================================================
= EOF                                                                     =
=========================================================================*/
