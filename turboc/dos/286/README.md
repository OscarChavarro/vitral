# Vitral 80286 protected-mode runtime

Define TURBO_C_286_ENABLED for the ring-0 backend. Do not also define
TURBO_C_8086_REAL_MODE_ENABLED. XMS allocation, handles and move calls belong
only to the 8086 backend. DOS.BAT/DOSLINK.BAT default to the 286 objects;
passing 8086 builds the separate XMS library.

## Physical RAM ownership

Compile/transfer normally, then boot without HIMEM, EMM386, DPMI, SMARTDRV,
RAM disks or other extended-memory clients. DOS F5 startup bypass can run
the files already copied to C:. It also disables CD-ROM/network drivers.
The loader rejects an XMS manager or an already protected/V86 environment
rather than overwrite another owner's RAM.

BIOS 15h/88h reports contiguous RAM. The loader leaves the HMA unused and
takes 0x00110000 through min(BIOS RAM end, 0x01000000). It caps the physical
end, not a fixed allocation size, fixing the previous 64 KiB overrun.
The heap/RGB pool starts after the copied image, aligned to 64 KiB.

The small DOS launcher RAYTRACE.EXE streams the MZ payload RAY286.BIN into
extended memory. Keep RAY286.MAP beside both binaries: the loader reads its
segment classes and public symbols, including TLINK's optional `idle` marks.
Both links use the same PMSW286.OBJ first. DOSSEG keeps the separate 32 KiB
PMSTACK outside DGROUP's address span; its MZ stack pointer is 8000h. The
protected DS base is the paragraph base of DGROUP, not the possibly unaligned
start byte of _DATA.

Before the permanent PE transition, AT BIOS 15h/87h transfers copy the image
and patch MZ segment words. The BIOS owns its temporary transition/return.
No XMS allocation or move is used. The ASM gateway then enables A20 via the
keyboard controller, installs GDT/IDT, sets PE with LMSW and takes a far
jump. No BIOS or DOS service is called by the protected runtime.

## RGB and CRT

RGBImageUncompressed uses Protected286ModeRgbImage under the 286 macro.
Only the explicit 8086 macro selects XmsRgbImage. The protected class uses
selector:offset accesses directly, including pixels crossing a 64 KiB
boundary. Heap and RGB blocks share the physical allocator, which excludes
the loaded executable and cannot release reserved image blocks.

Protected286Crt provides VGA, polled AT keyboard and primary/master ATA PIO
FAT12/FAT16 root access. The stdio adapter in crt owns its FILE objects;
Borland DOS FILE state is invalid after PE. Scenes and PPM output use 8.3
files in C:\. The application excludes C0L.OBJ and CL.LIB.
The CRT buffers output in 512-byte blocks. FAT allocation caches its FAT
sector and resumes its free-cluster search, avoiding repeated full-table
scans on populated disks. Disk-read errors cannot be mistaken for free
clusters. The VGA progress monitor reserves the bottom five text rows so
its status survives console scrolling.

protected286KernelMain initializes devices, heap and Borland C++ startup
tables, then forwards the saved DOS command line to raytracerMain. The
portable application body remains shared with Unix main. Exit tables and
atexit callbacks run on completion, followed by a polled key and CPU reset.

The current build requires a hardware 287-compatible FPU. Compile with
-f87; the gateway verifies and initializes the FPU before PE. The CRT
provides hardware fixup symbols without EMU.LIB/FP87.LIB startup objects.
MATHL's pre-387 sin/cos/tan/atan/exp/pow paths use INT 3Eh shortcuts;
crt/TRIG.CPP and FPU.ASM replace these with native 287 operations. The
remaining MATHL functions are linked as needed. No DOS floating-point
interrupt handlers are installed in protected mode.

The no-XMS build was compiled and linked in the DOS VM under
C:\VITRAL\TC298. Its MAP includes PRGB286 and no XMS modules. Normal boot
correctly rejects HIMEM. With DOS F5 startup bypass, the VGA trace reaches
protected mode, the C++ kernel, heap setup and "IDE/FAT C: backend ready".
The shown physical range is base 0x00110000, bytes 0x00EF0000, ending at
0x01000000. A hardware boot probe alone does not establish rendering/PPM
output. Stage BALLS.RAY in C:\ before booting without CONFIG/AUTOEXEC and
test the full application with:

```bat
RAYTRACE --resolution 32x24 --scene C:\BALLS.RAY --output C:\RAYTRACE.PPM
```

Full validation in C:\VITRAL\TC299 rebuilt all 283 BASE objects, DOS/CRT
and the example. RUN.BAT rendered BALLS.RAY at 320x240, displayed VGA
progress to 100%, wrote a valid 230488-byte P6 file to C:\RAYTRACE.PPM and
returned zero before waiting for the reboot key. The RGB payload is 230400
bytes and spans four protected memory segments. The saved image was read
back independently from the VM's FAT partition and decoded successfully.
This validates the analytic-sphere example, not every geometry module or
operation of a general-purpose C runtime. A real 286/287 machine still
needs hardware validation beyond the VirtualBox test.

The split launcher/payload build was also validated in C:\VITRAL\TC303.
RAY286.BIN is 988992 bytes and RAYTRACE.EXE is 30300 bytes; both enter protected
code at offset 008Dh with SP=8000h. Their MAPs show PMSTACK with G=(none),
59422 bytes of payload _DATA and all eleven exported loader-state symbols.
After booting without CONFIG/AUTOEXEC, BALLS.RAY rendered at 320x240 in three
reported seconds and returned zero. C:\B303.PPM was independently read from
the virtual FAT disk and decoded as a complete 230488-byte P6 image with
230400 RGB bytes. CONFIG.SYS and AUTOEXEC.BAT were restored after the bare
boot without loading their drivers. A subsequent full rebuild of all 283
BASE objects, DOS/CRT and the example also rendered C:\B306.PPM with RGB bytes
identical to that original sphere result.

The same rebuilt program then processed balls_featured_object.ray, copied
unchanged apart from line endings to C:\APPEL3.RAY. It constructed Appel 3
with 54 vertices, 84 edges and 32 faces, matching the original cpp fixture
and retaining the boolean pipeline's strict topology validation. At 320x240
it reported 949 seconds of rendering, reached 100%, exported C:\APPEL3.PPM
and returned zero. The 230488-byte P6 file was independently extracted from
the VM's FAT disk, decoded and converted to PNG; its RGB payload is 230400
bytes. This validates both sphere rendering and the Appel BREP path in the VM.

The Appel check exposed a collection-port defect: Turbo's built-in far
pointer ordering compares offsets only. Ordered keys with equal offsets in
different selectors were being merged, sometimes changing topology counts
when heap placement changed. Comparator.h, TreeSet, TreeMap and compound
half-edge keys now use a total pointer order on Turbo. ORDTEST.CPP passes
under the hosted Turbo ABI, including distinct same-offset pointer keys and
ordinary value keys. The geometric operations and tolerances stay aligned
with cpp; no validation was disabled to make the fixture pass.

## Diagnostics and transfer

Bootstrap markers write directly to VGA, without DOS printf. They identify
ownership, BIOS RAM, range planning, copy, relocations, GDT/PE, C++ entry,
heap and FAT. The physical range is shown; failures deliberately halt.

Keep long host names. Run sh scripts/stageDos286.sh to copy sources, apply
map_to_83.py, validate DOS paths and generate VITRAL.TGZ/ISO. The mapped
VSDK/TOOL directories become V/T. Extract from C:\VITRAL, without another
nested prefix. An unused root (e.g. sh scripts/stageDos286.sh TC297)
preserves old builds. All guest working directories stay under C:\VITRAL.
