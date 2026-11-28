import type { InputStream } from "../../../java/io/InputStream.js";
import type { OutputStream } from "../../../java/io/OutputStream.js";
import { IllegalStateException } from "../../../java/lang/IllegalStateException.js";

/**
TypeScript replacement for the `LZW` native library that Java's `LzwWrapper`
loads through JNI (the CMake project `pkgs/LempelZivWelch`).

Two C files are ported literally, keeping their module-level state, which
persists from one call to the next as the C statics do:

- `src_lib/compress.c`: `initLZW`, `getcFunction`, `readByteCode` and
  `decompress`. `compress` is not ported, because Java's binding leaves it
  commented out.
- `src/LzwWrapper.c`: the JNI stream adapters `myfread`, `myfwrite` and
  `myflush`, including their quirks:
  - A read always copies the whole requested block from a reused Java
    array, so a short read leaves stale bytes.
  - The end of the stream is detected when two reads in a row leave
    `available()` false. JNI reads that `int` through `CallBooleanMethod`,
    which keeps only its low byte, so 256 counts as false.

The decoder tables overlay one byte area, as the C macros overlay `htab`:
`tab_suffixof` and the output stack `de_stack` share `htab`. When a corrupt
stream makes the stack run past that area, the C library writes outside its
tables and the JVM dies with a segmentation fault. Here the decoder throws an
`IllegalStateException` at that point instead.
*/

//= compress.c constants ==================================================
const PBITS = 16;
const BITS = PBITS;
const HSIZE = 69001;
const BIT_MASK = 0x1f;
const BLOCK_MASK = 0x80;
const INIT_BITS = 9;
const FIRST = 257;
const CLEAR = 256;
const EOF = -1;
const BUFSIZ = 8192;
/** `sizeof(count_int)`: `htab` is an array of `long int`. */
const COUNT_INT_SIZE = 8;
const HTAB_BYTES = HSIZE * COUNT_INT_SIZE;
/** `de_stack`: `&tab_suffixof(1<<BITS)`. */
const DE_STACK = 1 << BITS;

const MAXCODE = (nBits: number): number => (1 << nBits) - 1;
const MIN = (a: number, b: number): number => (a < b ? a : b);

const magic_header: Uint8Array = Uint8Array.of(0o37, 0o235);
const rmask: Uint8Array = Uint8Array.of(0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff);

//= compress.c state ======================================================
let n_bits = 0;
let maxbits: number = BITS;
let maxcode = 0;
let maxmaxcode: number = 1 << BITS;
/** `htab`, seen as the `char_type` array of `tab_suffixof` and `de_stack`. */
const htab: Uint8Array = new Uint8Array(HTAB_BYTES);
/** `codetab`, seen as `tab_prefixof`. */
const codetab: Uint16Array = new Uint16Array(HSIZE);
let hsize: number = HSIZE;
let free_ent = 0;
let exit_stat = 0;
let block_compress: number = BLOCK_MASK;
let clear_flg = 0;

// Statics of readByteCode
let readOffset = 0;
let readSize = 0;
const readBuffer: Uint8Array = new Uint8Array(BITS);

type ReadFunction = (data: Uint8Array, dataOffset: number, size: number, n: number, descriptor: InputStream) => number;
type WriteFunction = (
    data: Uint8Array,
    dataOffset: number,
    size: number,
    n: number,
    descriptor: OutputStream,
) => number;
type FlushFunction = (descriptor: InputStream | OutputStream) => void;

let readFunction: ReadFunction | null = null;
let writeFunction: WriteFunction | null = null;
let flushFunction: FlushFunction | null = null;

//= LzwWrapper.c state ====================================================
let GLOBAL_arrRead: Uint8Array | null = null;
let LAST_arrReadSize = -1;
let GLOBAL_arrWrite: Uint8Array | null = null;
let LAST_arrWriteSize = -1;
// Statics of myfread
let lastFlag = true;
let flag = true;

//= LzwWrapper.c ==========================================================
function myfread(data: Uint8Array, dataOffset: number, size: number, n: number, is: InputStream): number {
    if (n * size !== LAST_arrReadSize) {
        GLOBAL_arrRead = new Uint8Array(size * n);
        LAST_arrReadSize = n * size;
    }
    const arrRead: Uint8Array = GLOBAL_arrRead!;

    const nn: number = is.readBytes(arrRead);
    data.set(arrRead.subarray(0, size * n), dataOffset);

    // CallBooleanMethod on the int returning available() keeps its low byte
    flag = (is.available() & 0xff) !== 0;

    if (!flag && !lastFlag) {
        return 0;
    }
    lastFlag = flag;

    return nn;
}

function myfwrite(data: Uint8Array, dataOffset: number, size: number, n: number, os: OutputStream): number {
    if (n * size !== LAST_arrWriteSize) {
        GLOBAL_arrWrite = new Uint8Array(size * n);
        LAST_arrWriteSize = n * size;
    }
    const arrWrite: Uint8Array = GLOBAL_arrWrite!;

    arrWrite.set(data.subarray(dataOffset, dataOffset + size * n));
    for (let i = 0; i < n; i++) {
        os.writeBytes(arrWrite);
    }

    return n;
}

function myflush(_descriptor: InputStream | OutputStream): void {
    LAST_arrReadSize = -1;
    LAST_arrWriteSize = -1;
    GLOBAL_arrRead = null;
    GLOBAL_arrWrite = null;
}

//= compress.c ============================================================
function getcFunction(descriptor: InputStream): number {
    const byte: Uint8Array = Uint8Array.of(0xff);
    const v: number = readFunction!(byte, 0, 1, 1, descriptor);

    if (v === -1) {
        return EOF;
    }
    if (v === 0) {
        return EOF;
    }
    return byte[0]!;
}

/**
`exit(1)` in C. It cannot be reached from `decompress`, which never sees a
negative write count; per the library rule it throws instead of ending the
process.
*/
function writeerr(msg: string): never {
    console.error("Internal LZW error [" + msg + "]. Aborting program.");
    throw new IllegalStateException("Internal LZW error [" + msg + "]");
}

function readByteCode(fdIn: InputStream): number {
    let code: number;
    let r_off: number;
    let bits: number;
    let bp = 0;

    if (clear_flg > 0 || readOffset >= readSize || free_ent > maxcode) {
        // If the next entry will be too big for the current code size,
        // then we must increase the size. This implies reading a new
        // buffer full, too.
        if (free_ent > maxcode) {
            n_bits++;
            if (n_bits === maxbits) {
                // won't get any bigger now
                maxcode = maxmaxcode;
            } else {
                maxcode = MAXCODE(n_bits);
            }
        }
        if (clear_flg > 0) {
            maxcode = MAXCODE((n_bits = INIT_BITS));
            clear_flg = 0;
        }
        readSize = readFunction!(readBuffer, 0, 1, n_bits, fdIn);
        if (readSize <= 0) {
            // end of file
            return -1;
        }
        readOffset = 0;
        // Round size down to integral number of codes
        readSize = (readSize << 3) - (n_bits - 1);
    }
    r_off = readOffset;
    bits = n_bits;

    // Get to the first byte.
    bp += r_off >> 3;
    r_off &= 7;

    // Get first part (low order bits)
    code = (readBuffer[bp++]! >> r_off) & rmask[8 - r_off]! & 0xff;
    bits -= 8 - r_off;
    r_off = 8 - r_off; // now, offset into code word

    // Get any 8 bit parts in the middle (<=1 for up to 16 bits).
    if (bits >= 8) {
        code |= (readBuffer[bp++]! & 0xff) << r_off;
        r_off += 8;
        bits -= 8;
    }

    // high order bits. C reads one byte past the buffer here when the
    // mask is empty; the masked value is zero either way.
    code |= ((readBuffer[bp] ?? 0) & rmask[bits]!) << r_off;
    readOffset += n_bits;

    return code;
}

/** `*stackp++ = value`, guarding the end of the `htab` overlay. */
function pushStack(stackp: number, value: number): number {
    if (stackp >= HTAB_BYTES) {
        throw new IllegalStateException(
            "Corrupt LZW stream: the decoder stack left its table (the native library crashes here)",
        );
    }
    htab[stackp] = value;
    return stackp + 1;
}

export function decompress(fdIn: InputStream, fdOut: OutputStream): void {
    let stackp: number;
    let finchar: number;
    let code: number;
    let oldcode: number;
    let incode: number;
    let n: number;
    let nwritten: number;
    let offset: number;
    const buff: Uint8Array = new Uint8Array(BUFSIZ);

    // Check the magic number
    if (getcFunction(fdIn) !== (magic_header[0]! & 0xff) || getcFunction(fdIn) !== (magic_header[1]! & 0xff)) {
        console.error("input not in compressed format");
        return;
    }
    maxbits = getcFunction(fdIn); // set -b from file
    block_compress = maxbits & BLOCK_MASK;
    maxbits &= BIT_MASK;
    maxmaxcode = 1 << maxbits;
    if (maxbits > BITS) {
        console.error("input compressed with " + maxbits + " bits, can only handle " + BITS + " bits");
        return;
    }

    // As above, initialize the first 256 entries in the table.
    maxcode = MAXCODE((n_bits = INIT_BITS));
    for (code = 255; code >= 0; code--) {
        codetab[code] = 0;
        htab[code] = code;
    }
    free_ent = block_compress !== 0 ? FIRST : 256;

    finchar = oldcode = readByteCode(fdIn);
    if (oldcode === -1) {
        // EOF already?
        return; // Get out of here
    }

    // first code must be 8 bits = char
    n = 0;
    buff[n++] = finchar;

    stackp = DE_STACK;

    while ((code = readByteCode(fdIn)) > -1) {
        if (code === CLEAR && block_compress !== 0) {
            for (code = 255; code >= 0; code--) {
                codetab[code] = 0;
            }
            clear_flg = 1;
            free_ent = FIRST - 1;
            if ((code = readByteCode(fdIn)) === -1) {
                // O, untimely death!
                break;
            }
        }
        incode = code;

        // Special case for KwKwK string.
        if (code >= free_ent) {
            stackp = pushStack(stackp, finchar);
            code = oldcode;
        }

        // Generate output characters in reverse order
        while (code >= 256) {
            stackp = pushStack(stackp, htab[code]!);
            code = codetab[code]!;
        }
        finchar = htab[code]!;
        stackp = pushStack(stackp, finchar);

        // And put them out in forward order
        do {
            buff[n++] = htab[--stackp]!;
            if (n === BUFSIZ) {
                offset = 0;
                do {
                    nwritten = writeFunction!(buff, offset, n, 1, fdOut);
                    nwritten *= n;
                    if (nwritten < 0) {
                        writeerr("6");
                    }
                    offset += nwritten;
                } while ((n -= nwritten) > 0);
            }
        } while (stackp > DE_STACK);

        // Generate the new entry.
        if ((code = free_ent) < maxmaxcode) {
            codetab[code] = oldcode;
            htab[code] = finchar;
            free_ent = code + 1;
        }

        // Remember previous code.
        oldcode = incode;
    }

    // Flush the stuff remaining in our buffer...
    offset = 0;
    while (n > 0) {
        nwritten = writeFunction!(buff, offset, n, 1, fdOut);
        nwritten *= n;
        if (nwritten < 0) {
            writeerr("7");
        }
        offset += nwritten;
        n -= nwritten;
    }
    flushFunction!(fdIn);
    flushFunction!(fdOut);
}

export function initLZW(sizeEstimate: number): void {
    readFunction = myfread;
    writeFunction = myfwrite;
    flushFunction = myflush;

    exit_stat = 0;
    if (maxbits < INIT_BITS) {
        maxbits = INIT_BITS;
    }
    if (maxbits > BITS) {
        maxbits = BITS;
    }
    maxmaxcode = 1 << maxbits;

    hsize = HSIZE;
    if (sizeEstimate > 0) {
        if (sizeEstimate < 1 << 12) hsize = MIN(5003, HSIZE);
        else if (sizeEstimate < 1 << 13) hsize = MIN(9001, HSIZE);
        else if (sizeEstimate < 1 << 14) hsize = MIN(18013, HSIZE);
        else if (sizeEstimate < 1 << 15) hsize = MIN(35023, HSIZE);
        else if (sizeEstimate < 47000) hsize = MIN(50021, HSIZE);
    }
    // hsize and exit_stat only matter to compress(), which is not ported
    void hsize;
    void exit_stat;
}
