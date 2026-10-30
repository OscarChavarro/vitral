/* Flat image: sector 0 = boot (0x7C00), sectors 1..N = stage 2 (0x7E00). The kernel image is
   appended after it by scripts/common.sh. */
ENTRY(_start)
SECTIONS
{
    . = 0x7C00;
    .boot : { *(.boot) }
    . = 0x7E00;
    .stage2 : {
        *(.entry16)
        *(.text .text.*)
        *(.rodata .rodata.*)
        *(.data .data.*)
        . = ALIGN(512);
    }
    __stage2_end = .;
    .bss (NOLOAD) : { *(.bss) *(.bss.[!p]*) *(COMMON) }
    ASSERT(. <= 0x10000, "16-bit .bss must stay inside the first 64 KB")
    /* PAE page tables (PML4, PDPT, 4 PDs = 24 KB): fixed, 4 KB aligned, above the stage 2 image. */
    . = 0x10000;
    .paging (NOLOAD) : { *(.bss.paging) }
    /DISCARD/ : { *(.comment) *(.eh_frame*) *(.note*) }
}
__stage2_sectors = (__stage2_end - 0x7E00) / 512;
/* The kernel image follows the boot sector and stage 2 on the disk. */
__kernel_lba = 1 + __stage2_sectors;
ASSERT(__stage2_end <= 0x10000, "stage 2 overlaps the page tables")
ASSERT(__stage2_sectors <= 60, "stage 2 does not fit below the page tables (60 sectors)")
