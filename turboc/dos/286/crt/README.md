# Minimal 80286 C++ runtime

Turbo C++ 3.0, large memory model, ring 0. No DOS, BIOS or XMS calls.
Source filenames already fit DOS 8.3. This directory is independent of
the portable raytracer and is not added to Unix builds.

`HEAP.CPP` supplies a first-fit allocator with splitting and coalescing.
Its address-ordered free lists skip occupied blocks, while retaining the
eight-byte Turbo block header and the original first-fit allocation order.
Per-arena free capacity skips exhausted arenas; free capacity includes the
headers of free regions. Host differential checks of 100000 randomized
operations preserved the original allocation positions and live payloads.
`TEST.CPP` also repeatedly fills, fragments and reuses both test arenas;
the same probe passes under Turbo C++ with the hosted large-model ABI.
The loader must zero BSS before calling `crt286Initialize`, then supply
disjoint, aligned, writable arenas through `crt286AddArena`. Each arena
must fit within one valid selector and is at most 65534 bytes. An allocation
never crosses a selector; larger images require multiple allocations.
The heap does not discover RAM or install GDT descriptors. Its arenas come
from Protected286Memory, after the bare loader bounds BIOS-reported RAM
by 16 MiB, installs data descriptors and excludes the loaded executable.
Resetting the heap while allocations exist is forbidden. No concurrency
or interrupt-time allocation is supported.

`CXX.CPP` implements Turbo C++ scalar `new` and `delete`, also used by that
compiler for arrays, and `atexit` (64 callbacks). Allocation failure returns
null; there are no exceptions or new-handler retry loops. `START.CPP`
runs explicitly supplied constructor callbacks and exit callbacks in reverse
registration order. Its Borland adapter validates six-byte far-call records
and executes INITDATA in priority order and EXITDATA in reverse order. The
real-mode loader reads table bounds from the detailed TLINK MAP before
relocating the image; the protected kernel dispatches the relocated records.
`MEMORY.CPP` supplies bytewise memory operations and basic C string functions.
It also supplies strcat/strncat for StringBuilder in the larger BREP link.
The string regression probe can be run on the host with:

```sh
g++ -x c++ -std=c++98 -O0 -fno-builtin -Wall -Wextra -fsanitize=undefined \
    MEMORY.CPP STRINGS.CPP STRTEST.CPP -o /tmp/crt286-strings
/tmp/crt286-strings
```

Run `BUILD.BAT` in this directory to produce `BUILD\CRT286.LIB`.
Link it explicitly with `TLINK /n /c`, without `C0L.OBJ` or `CL.LIB`.
This is a runtime foundation, not a complete replacement for Borland's CRT:
`STDIO.CPP` implements the Borland FILE layout and root-file access through
Protected286Crt/FAT, including seek/tell. Each output file has a 512-byte
buffer so pixel-by-pixel fputc does not issue ATA I/O for every byte.
fflush, fclose and positioning operations drain that buffer; ATA transfers
are synchronous. Standard streams
use VGA and the polled keyboard. The runtime initializes streams and ASCII
ctype tables explicitly before constructing application objects.
`STRINGS.CPP` adds string search/tokenization and checked integer conversion.
`ORDTEST.CPP` is a hosted regression for the base ordered containers. Compile
it with the mapped BASE include directories and BASE's OBJECT.OBJ, linking
the normal Borland CRT for the probe only. It verifies distinct far pointer
keys with the same offset, lookup/removal, value keys and the natural-order
comparator. Those keys are never dereferenced. This checks the total-order
contract needed to replace cpp's std::set/std::map in the BREP algorithms.
The C++ module also supplies malloc/free and diagnostic termination paths.
`HELPERS.ASM` implements the Borland register/stack ABI for long multiply,
signed/unsigned division and remainder, shifts and structure copy. The independent
`HELPTST.ASM` probe passed in the DOS VM, linked without C0L or CL:

```bat
tasm /m2 /ml HELPTST.ASM BUILD\HELPTST.OBJ
tlink /n /c BUILD\HELPTST.OBJ+BUILD\HELPERS.OBJ,BUILD\HELPTST.EXE
BUILD\HELPTST.EXE
```

This proves the tested arithmetic/copy calling conventions in real mode;
it does not prove protected-mode entry, descriptor setup or renderer startup.
`SCAN.CPP` implements the scanner subset needed by the MIT scene reader:
`%s` with optional field width, `%d`, `%i`, `%ld`, `%f`, `%lf`, `%e`, `%g`,
assignment suppression and literal percent signs. Floating conversion accepts
finite decimal input, including exponents. Numeric fields are limited to 255
characters; out-of-range floats fail conversion with ERANGE. Other directives
are unsupported and fail with EINVAL. This is not a full ISO scanf replacement.
`SCANTEST.CPP` passed both the Linux sanitizer check and Turbo C++'s hosted
real-mode ABI probe (`TSTSCAN.BAT`). Only that test uses the Borland CRT to
initialize the FPU and provide assert; the application link does not.
`FORMAT.CPP` supplies the common formatting engine for printf/fprintf and
sprintf/vsprintf. Its bounded `crt286Format` API always terminates nonempty
buffers and returns the length before truncation. Supported fields are
strings, characters, signed/unsigned decimal, hexadecimal, pointers and fixed
decimal floats (`f`, optional `l`/`L`), with left alignment, sign, space/zero
padding, field width and precision (including `*`). Width is limited to 4096,
floating precision to 18 and floating magnitude to DBL_MAX, including for
`%Lf`. Scientific/general formats and other flags are not implemented and
fail with EINVAL; range limits fail with ERANGE. The `ll` modifier follows
Turbo C++'s 32-bit-long ABI, not a modern 64-bit long long ABI. Standard
sprintf/vsprintf remain unbounded; callers must provide sufficient space.
`FMTTEST.CPP` passed on Linux and in Turbo C++ via `TSTFMT.BAT`.

`DIRS.CPP` implements Borland DIR handles over FAT root enumeration,
getcwd returning `C:\`, and an empty environment (getenv returns null).
Only the root can be opened; mkdir explicitly fails with EACCES because
creating subdirectories is outside this runtime's filesystem contract.
`TSTDIR.BAT` passed in the VM using a fake backend to check the directory
ABI; `FATTEST.CPP` separately tests FAT12/FAT16 root traversal and multi-cluster
round trips in RAM. Its populated FAT16 fixture also checks that allocation
uses cached sectors and that both FAT copies agree after writing.

`VECTOR.CPP` implements the Turbo large-model vector entry points: far cdecl
constructor callbacks, four-byte count cookies, optional element-size cookies,
reverse-order destruction and the hidden destructor argument. Each complete
allocation must fit one selector; oversized arrays fail allocation. Near,
Pascal and huge-vector modes are unsupported and take the diagnostic abort
path. The actual compiler-generated new[]/delete[] sequence passed in
`VECTEST.CPP` via `TSTREST.BAT`, with a hosted allocation backend.

`TIME.CPP` implements ftime by polling the AT CMOS RTC, validating stable
snapshots and decoding BCD/binary and 12/24-hour fields. It interprets the RTC
as UTC and returns second resolution (millitm is zero), with the signed
32-bit epoch limit. Invalid/busy hardware reports EIO; out-of-range dates
report ERANGE. Turbo's void ftime ABI requires callers to inspect errno.
`TIMETEST.CPP` checks conversion using mocked ports, both on Linux and in DOS.

The scanner also exports decimal strtod, removing the MATHL STRTOD module's
dependency on the Borland scanner trampoline. Decimal overflow returns signed
IEEE infinity with ERANGE; complete underflow returns signed zero. Hex floats,
textual infinity/NaN and correctly rounded full-precision conversion are not
provided by this minimal implementation.

The loader fills `_psp` and `_version` from real DOS before copying the image.
The signal-handler registry is intentionally null: Borland FPERR checks for
that case, prints an error and invokes our abort implementation.

The complete BASE/DOS/raytracer build linked successfully in the DOS VM at
`C:\VITRAL\TC294\TESTSUIT\OFFLEXAM\RAYOFFEX`, with TLINK /n and without
C0L.OBJ or CL.LIB. All seven previously unresolved symbols are now resolved
or eliminated by the replacement conversion routine.

`FPU.ASM` verifies and initializes a hardware 287-compatible FPU before PE,
supplies Borland's absolute hardware fixup symbols, and implements native
FPTAN/FPATAN/FYL2X/F2XM1 helpers. `TRIG.CPP` replaces the MATHL sin, cos,
tan, atan, exp and pow functions whose pre-387 paths require INT 3Eh. These
replacements are isolated from Unix builds. Software FPU emulation is not
provided. Compile every target module with -f87 and link MATHL after CRT286,
without EMU.LIB or FP87.LIB: their startup code uses DOS vector services.
This is a minimal math layer, not a complete ISO C floating-point environment;
round-to-nearest from FNINIT must remain in effect.

Host reference checks for the C++ math layer (long-double stand-ins do not
validate the native 287 instruction helpers):

```sh
g++ -fno-builtin TRIG.CPP TRIGTEST.CPP -o /tmp/crt286-trigtest
/tmp/crt286-trigtest
```

Host allocator/lifecycle check (does not test the Turbo ABI or selectors):

```sh
g++ -x c++ -std=c++98 -Wall -Wextra -pedantic -fsanitize=address,undefined \
    HEAP.CPP START.CPP TEST.CPP -o /tmp/crt286-test
/tmp/crt286-test
```

The protected bootstrap sequence is: establish DS/SS and clear BSS;
initialize heap; register arenas; initialize devices/filesystem; run C++
startup callbacks; call the portable application entry; finalize callbacks;
wait for a polled key; reset the machine.
