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

#include "compress.h"

#include <stdlib.h>

/*
 * Set USERMEM to the maximum amount of physical user memory available
 * in bytes.  USERMEM is used to determine the maximum BITS that can be used
 * for compression.
 */
#define USERMEM 450000 /* default user memory */
#define PBITS 16
#define BITS PBITS /* Preferred BITS for this memory size */
#define HSIZE 69001 /* 95% occupancy */
#define BIT_MASK 0x1f          /* Defines for third byte of header */
#define BLOCK_MASK 0x80
#define INIT_BITS 9            /* initial number of bits/code */
#define CHECK_GAP 10000    /* ratio check interval */
/*
 * the next two codes should not be changed lightly, as they must not
 * lie within the contiguous general code space.
 */ 
#define FIRST    257    /* first free entry */
#define CLEAR    256    /* table clear output code */

/*
 * To save much memory, we overlay the table used by compress() with those
 * used by decompress().  The tab_prefix table is the same size and type
 * as the codetab.  The tab_suffix table needs 2**BITS characters.  We
 * get this from the beginning of htab.  The output stack uses the rest
 * of htab, and contains characters.  There is plenty of room for any
 * possible stack (stack used to be 8000 characters).
 */
#define tab_prefixof(i)    codetabof(i)
#define tab_suffixof(i)    ((char_type *)(htab))[i]
#define de_stack           ((char_type *)&tab_suffixof(1<<BITS))

#define MAXCODE(n_bits)    ((1 << (n_bits)) - 1)
#define htabof(i)    htab[i]
#define codetabof(i)    codetab[i]
#define MIN(a,b) (((a)<(b))?(a):(b))

/* a code_int must be able to hold 2**BITS values of type int, and also -1 */
typedef long int code_int;
typedef long int count_int;
typedef unsigned char char_type;

static char_type magic_header[] = { "\037\235" };    /* 1F 9D */
static int n_bits;                        /* number of bits/code */
static int maxbits = BITS;                /* user settable max # bits/code */
static code_int maxcode;                  /* maximum code, given n_bits */
static code_int maxmaxcode = 1 << BITS;   /* should NEVER generate this code */
static count_int htab [HSIZE];
static unsigned short codetab [HSIZE];
static code_int hsize = HSIZE;            /* for dynamic table sizing */
static code_int free_ent = 0;             /* first unused entry */
static int exit_stat = 0;                 /* per-file status */

/*
 * block compression parameters -- after all codes are used up,
 * and compression rate changes, start over.
 */
/* If block_compress == 0, compressed files will be compatible with
   compress 2.0 */
static int block_compress = BLOCK_MASK;
static int clear_flg = 0;
static long int ratio = 0;
static count_int checkpoint = CHECK_GAP;
static int offset;
static long int in_count = 1;             /* length of input */
static long int bytes_out;                /* length of compressed output */
static long int out_count = 0;            /* # of codes output (for debugging) */
static char buf[BITS];
static char_type lmask[9] = {0xff, 0xfe, 0xfc, 0xf8, 0xf0, 0xe0, 0xc0, 0x80, 0x00};
static char_type rmask[9] = {0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff};

/*
Note that fread and fwrite functions are generalized!
*/
size_t (*readFunction)(void *, size_t, size_t, void *);
size_t (*writeFunction)(void *, size_t, size_t, void *);
void (*flushFunction)(void *);

int
getcFunction(void *descriptor)
{
    unsigned char byte = -1;
    size_t v;

    v = readFunction(&byte, 1, 1, descriptor);

    if ( v == -1 ) {
        return EOF;
    }
    if ( v == 0 ) {
        return EOF;
    }
    
    return (int)byte;
}

int
putcFunction(int data, void *descriptor)
{
    unsigned char byte = (unsigned char)data;
    writeFunction(&byte, 1, 1, descriptor);

    return 0;
}

/* reset code table */
static void cl_hash(register count_int hsize)
{
    register count_int *htab_p = htab+hsize;
    register long i;
    register long m1 = -1;

    i = hsize - 16;
     do {                /* might use Sys V memset(3) here */
        *(htab_p-16) = m1;
        *(htab_p-15) = m1;
        *(htab_p-14) = m1;
        *(htab_p-13) = m1;
        *(htab_p-12) = m1;
        *(htab_p-11) = m1;
        *(htab_p-10) = m1;
        *(htab_p-9) = m1;
        *(htab_p-8) = m1;
        *(htab_p-7) = m1;
        *(htab_p-6) = m1;
        *(htab_p-5) = m1;
        *(htab_p-4) = m1;
        *(htab_p-3) = m1;
        *(htab_p-2) = m1;
        *(htab_p-1) = m1;
        htab_p -= 16;
    } while ((i -= 16) >= 0);
        for ( i += 16; i > 0; i-- )
        *--htab_p = m1;
}

static void
writeerr(char *msg)
{
    fprintf(stderr, "Internal LZW error [%s]. Aborting program.\n", msg);
    fflush(stderr);
    exit(1);
}

/**
Output the given code.
Inputs:
    code: A n_bits-bit integer.  If == -1, then EOF.  This assumes
       that (n_bits <= (long)wordsize - 1).
Outputs:
    Outputs code to the file.
Assumptions:
   Chars are 8 bits long.
Algorithm:
    Maintain a BITS character long buffer (so that 8 codes will
    fit in it exactly).  When the buffer fills up empty it and start over.
*/
static int
writeByteCode(void *fdOut, code_int code)
{
    register int r_off = offset, bits= n_bits;
    register char * bp = buf;

    if ( code >= 0 ) {
        /* A byte/bit numbering VAX operation is simulated here */
        /* Get to the first byte. */
        bp += (r_off >> 3);
        r_off &= 7;
        /* Since code is always >= 8 bits, only need to mask the first
         * hunk on the left. */
        *bp = ( ((*bp) & rmask[r_off]) | ((code << r_off) & (lmask[r_off])) );
        bp++;
        bits -= (8 - r_off);
        code >>= 8 - r_off;
        /* Get any 8 bit parts in the middle (<=1 for up to 16 bits). */
        if ( bits >= 8 ) {
            *bp++ = code;
            code >>= 8;
            bits -= 8;
        }
        /* Last bits. */
        if(bits)
            *bp = code;
        offset += n_bits;
        if ( offset == (n_bits << 3) ) {
            bp = buf;
            bits = n_bits;
            bytes_out += bits;
            do {
                putcFunction(*bp++, fdOut);
/*
                if ( ferror(fdOut) ) {
                    writeerr("1");
                    return 0;
                }
*/
            } while(--bits);
            offset = 0;
        }

        /*
         * If the next entry is going to be too big for the code size,
         * then increase it, if possible.
         */
        if ( free_ent > maxcode || (clear_flg > 0))
        {
            /*
             * Write the whole buffer, because the input side won't
             * discover the size increase until after it has read it.
             */
            if ( offset > 0 ) {
                if( writeFunction(buf, 1, n_bits, fdOut) != n_bits ) {
                    writeerr("2");
                    return 0;
                }
                bytes_out += n_bits;
            }
            offset = 0;
    
            if ( clear_flg ) {
                    maxcode = MAXCODE (n_bits = INIT_BITS);
                clear_flg = 0;
            }
            else {
                n_bits++;
                if ( n_bits == maxbits )
                maxcode = maxmaxcode;
                else
                maxcode = MAXCODE(n_bits);
            }
        }
    }
    else {
        /* At EOF, write the rest of the buffer. */
        if ( offset > 0 ) {
            offset = (offset + 7) / 8;
            if( writeFunction( buf, 1, offset, fdOut) != offset ) {
                writeerr("3");
                return 0;
            }
            bytes_out += offset;
        }
        offset = 0;
        flushFunction(fdOut);
/*
        if( ferror(fdOut) ) {
            writeerr("4");
            return 0;
        }
*/
    }
    return 1;
}

static void
cl_block(void *fdOut)        /* table clear for block compress */
{
    register long int rat;

    checkpoint = in_count + CHECK_GAP;
    if ( in_count > 0x007fffff ) {
        /* Shift will overflow */
        rat = bytes_out >> 8;
        if ( rat == 0 ) {
            /* Don't divide by zero */
            rat = 0x7fffffff;
          }
          else {
            rat = in_count / rat;
        }
      }
      else {
        rat = (in_count << 8) / bytes_out; /* 8 fractional bits */
    }

    if ( rat > ratio ) {
        ratio = rat;
      }
      else {
        ratio = 0;
        cl_hash ( (count_int) hsize );
        free_ent = FIRST;
        clear_flg = 1;
        writeByteCode(fdOut, (code_int) CLEAR );
    }
}

/**
compress stdin to stdout
Algorithm:  use open addressing double hashing (no chaining) on the 
prefix code / next character combination.  We do a variant of Knuth's
algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
secondary probe.  Here, the modular division first probe is gives way
to a faster exclusive-or manipulation.  Also do block compression with
an adaptive reset, whereby the code table is cleared when the compression
ratio decreases, but after the table fills.  The variable-length output
codes are re-sized at this point, and a special CLEAR code is generated
for the decompressor.  Late addition:  construct the table according to
file size for noticeable speed improvement on small files.  Please direct
questions about this implementation to ames!jaw.
*/
void
compress(void *fdIn, void *fdOut)
{
    register long fcode;
    register code_int i = 0;
    register int c;
    register code_int ent;
    register int disp;
    register code_int hsize_reg;
    register int hshift;

    putcFunction(magic_header[0], fdOut);
    putcFunction(magic_header[1], fdOut);
    putcFunction((char)(maxbits | block_compress), fdOut);
/*
    if( ferror(fdOut) ) {
        writeerr("5");
    }
*/
    offset = 0;
    bytes_out = 3;        /* includes 3-byte header mojo */
    out_count = 0;
    clear_flg = 0;
    ratio = 0;
    in_count = 1;
    checkpoint = CHECK_GAP;
    maxcode = MAXCODE(n_bits = INIT_BITS);
    free_ent = ((block_compress) ? FIRST : 256 );

    ent = getcFunction(fdIn);

    hshift = 0;
    for ( fcode = (long) hsize;  fcode < 65536L; fcode *= 2L )
        hshift++;
    hshift = 8 - hshift;        /* set hash code range bound */

    hsize_reg = hsize;
    cl_hash( (count_int) hsize_reg);        /* clear hash table */

    while ( (c = getcFunction(fdIn)) != EOF ) {
    in_count++;
    fcode = (long) (((long) c << maxbits) + ent);
     i = ((c << hshift) ^ ent);    /* xor hashing */

    if ( htabof (i) == fcode ) {
        ent = codetabof (i);
        continue;
    } else if ( (long)htabof (i) < 0 )    /* empty slot */
        goto nomatch;
     disp = hsize_reg - i;        /* secondary hash (after G. Knott) */
    if ( i == 0 )
        disp = 1;
probe:
    if ( (i -= disp) < 0 )
        i += hsize_reg;

    if ( htabof (i) == fcode ) {
        ent = codetabof (i);
        continue;
    }
    if ( (long)htabof (i) > 0 ) 
        goto probe;
nomatch:
    writeByteCode(fdOut, (code_int)ent);
    out_count++;
     ent = c;
    if ( free_ent < maxmaxcode ) {
         codetabof (i) = free_ent++;    /* code -> hashtable */
        htabof (i) = fcode;
    }
    else if ( (count_int)in_count >= checkpoint && block_compress )
        cl_block(fdOut);
    }
    /*
     * Put out the final code.
     */
    writeByteCode(fdOut, (code_int)ent);
    out_count++;
    writeByteCode(fdOut, (code_int)-1);

    /*
     * Print out stats on stderr
     */
    if(bytes_out > in_count)    /* exit(2) if no savings */
    exit_stat = 2;

    flushFunction(fdIn);
    flushFunction(fdOut);

    return;
}

/**
Read one code from the standard input.  If EOF, return -1.
Inputs:
    stdin
Outputs:
    code or -1 is returned.
*/
static code_int
readByteCode(void *fdIn) {
    register code_int code;
    static int offset = 0, size = 0;
    static char_type buffer[BITS];
    register int r_off, bits;
    register char_type *bp = buffer;

    if ( clear_flg > 0 || offset >= size || free_ent > maxcode ) {
        /* If the next entry will be too big for the current code
         * size, then we must increase the size.  This implies reading
         * a new buffer full, too. */
        if ( free_ent > maxcode ) {
            n_bits++;
            if ( n_bits == maxbits ) {
                /* won't get any bigger now */
                maxcode = maxmaxcode;    
              }
              else {
                maxcode = MAXCODE(n_bits);
            }
        }
        if ( clear_flg > 0) {
            maxcode = MAXCODE (n_bits = INIT_BITS);
            clear_flg = 0;
        }
        size = readFunction(buffer, 1, n_bits, fdIn);
        if ( size <= 0 ) {
            /* end of file */
            return -1;
        }
        offset = 0;
        /* Round size down to integral number of codes */
        size = (size << 3) - (n_bits - 1);
    }
    r_off = offset;
    bits = n_bits;
    /* Get to the first byte. */
    bp += (r_off >> 3);
    r_off &= 7;
    /* Get first part (low order bits) */
    code = ((*bp++ >> r_off) & rmask[8 - r_off]) & 0xff;
    bits -= (8 - r_off);
    r_off = 8 - r_off;        /* now, offset into code word */
    /* Get any 8 bit parts in the middle (<=1 for up to 16 bits). */
    if ( bits >= 8 ) {
        code |= (*bp++ & 0xff) << r_off;
        r_off += 8;
        bits -= 8;
    }
    /* high order bits. */
    code |= (*bp & rmask[bits]) << r_off;
    offset += n_bits;

    return code;
}

/**
Decompress stdin to stdout.  This routine adapts to the codes in the
file building the "string" table on-the-fly; requiring no table to
be stored in the compressed file.  The tables used herein are shared
with those of the compress() routine.  See the definitions above.
*/
void
decompress(void *fdIn, void *fdOut)
{
    register char_type *stackp;
    register int finchar;
    register code_int code, oldcode, incode;
    int n, nwritten, offset;    /* Variables for buffered write */
    char buff[BUFSIZ];          /* Buffer for buffered write */

    /* Check the magic number */
    if ( (getcFunction(fdIn) != (magic_header[0] & 0xFF)) ||
         (getcFunction(fdIn) != (magic_header[1] & 0xFF))) {
        fprintf(stderr, "input not in compressed format\n");
        return;
    }
    maxbits = getcFunction(fdIn);    /* set -b from file */
    block_compress = maxbits & BLOCK_MASK;
    maxbits &= BIT_MASK;
    maxmaxcode = 1 << maxbits;
    if( maxbits > BITS ) {
        fprintf(stderr,
            "input compressed with %d bits, can only handle %d bits\n",
            maxbits, BITS);
        return;
    }


    /* As above, initialize the first 256 entries in the table. */
    maxcode = MAXCODE(n_bits = INIT_BITS);
    for ( code = 255; code >= 0; code-- ) {
        tab_prefixof(code) = 0;
        tab_suffixof(code) = (char_type)code;
    }
    free_ent = ((block_compress) ? FIRST : 256 );

    finchar = oldcode = readByteCode(fdIn);
    if ( oldcode == -1 ) {
        /* EOF already? */
        return; /* Get out of here */
    }

    /* first code must be 8 bits = char */
    n = 0;
    buff[n++] = (char)finchar;
    stackp = de_stack;

    while ( ( code = readByteCode(fdIn) ) > -1 ) {
        if ( (code == CLEAR) && block_compress ) {
            for ( code = 255; code >= 0; code-- ) {
                tab_prefixof(code) = 0;
            }
            clear_flg = 1;
            free_ent = FIRST - 1;
            if ( ( code = readByteCode(fdIn) ) == -1 ) {
                /* O, untimely death! */
                break;
            }
        }
        incode = code;
        /*
         * Special case for KwKwK string.
         */
        if ( code >= free_ent ) {
            *stackp++ = finchar;
            code = oldcode;
        }

        /*
         * Generate output characters in reverse order
         */
        while ( code >= 256 ) {
            *stackp++ = tab_suffixof(code);
            code = tab_prefixof(code);
        }
        *stackp++ = finchar = tab_suffixof(code);

        /*
         * And put them out in forward order
         */
        do {
            /*
             * About 60% of the time is spent in the putchar() call
             * that appeared here.  It was originally
             *        putchar ( *--stackp );
             * If we buffer the writes ourselves, we can go faster (about
             * 30%).
             *
             * At this point, the next line is the next *big* time
             * sink in the code.  It takes up about 10% of the time.
             */
             buff[n++] = *--stackp;
             if ( n == BUFSIZ ) {
                 offset = 0;
                 do {
                     nwritten = writeFunction(&buff[offset], n, 1, fdOut);
                     nwritten *= n;
                     if ( nwritten < 0 ) {
                         writeerr("6");
                     }
                     offset += nwritten;
                 } while ( (n -= nwritten) > 0 );
             }
        } while ( stackp > de_stack );

        /*
         * Generate the new entry.
         */
        if ( (code=free_ent) < maxmaxcode ) {
            tab_prefixof(code) = (unsigned short)oldcode;
            tab_suffixof(code) = finchar;
            free_ent = code+1;
        } 
        /*
         * Remember previous code.
         */
        oldcode = incode;
    }

    /*
     * Flush the stuff remaining in our buffer...
     */
    offset = 0;
    while ( n > 0 ) {
        nwritten = writeFunction(&buff[offset], n, 1, fdOut);
        nwritten *= n;
        if ( nwritten < 0 ) {
            writeerr("7");
        }
        offset += nwritten;
        n -= nwritten;
    }
    flushFunction(fdIn);
    flushFunction(fdOut);
}

void
initLZW(long int sizeEstimate,
	size_t (*readf)(void *, size_t, size_t, void *),
        size_t (*writef)(void *, size_t, size_t, void *),
        void (*flushf)(void *))
{
    readFunction = readf;
    writeFunction = writef;
    flushFunction = flushf;
    /*----------------------------------------------------------------------*/
    exit_stat = 0;
    if ( maxbits < INIT_BITS ) {
        maxbits = INIT_BITS;
    }
    if ( maxbits > BITS ) {
        maxbits = BITS;
    }
    maxmaxcode = 1 << maxbits;

    /*----------------------------------------------------------------------*/
    hsize = HSIZE;
    if ( sizeEstimate > 0 ) {
        if ( sizeEstimate < (1 << 12) )      hsize = MIN (5003, HSIZE);
        else if ( sizeEstimate < (1 << 13) ) hsize = MIN (9001, HSIZE);
        else if ( sizeEstimate < (1 << 14) ) hsize = MIN (18013, HSIZE);
        else if ( sizeEstimate < (1 << 15) ) hsize = MIN (35023, HSIZE);
        else if ( sizeEstimate < 47000 )     hsize = MIN (50021, HSIZE);
    }
}

/*=========================================================================
= EOF                                                                     =
=========================================================================*/
