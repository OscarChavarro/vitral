/* Flat 64-bit kernel image, linked at 1 MB. Starts with the KernelHeader (entry64.S). The real
   mode loader copies the file there; .bss is not in the file and is cleared by _start64. */
ENTRY(_start64)
SECTIONS
{
    . = 0x100000;
    __image_start = .;
    .header : { KEEP(*(.header)) KEEP(*(.multiboot2)) }  /* KernelHeader first, then the Multiboot2 header */
    .text : {
        KEEP(*(.entry64))
        KEEP(*(.multiboot_entry))
        *(.text .text.*)
    }
    .rodata : { *(.rodata .rodata.*) }
    /* Real mode start-up code of the application processors, copied to low memory at run time. */
    .trampoline : { KEEP(*(.trampoline)) }
    /* Global constructors, run by Runtime::init(). */
    .init_array : {
        . = ALIGN(8);
        __init_array_start = .;
        KEEP(*(.init_array .init_array.*))
        __init_array_end = .;
    }
    /* Built-in programs: one constant Program each (kernel/Program.h). */
    .programs : {
        . = ALIGN(8);
        __programs_start = .;
        KEEP(*(.programs))
        __programs_end = .;
    }
    .data : {
        *(.data .data.*)
        . = ALIGN(8);
    }
    /* The FAT image of loopKernelRootFileSystem: the memory behind /dev/loop0 (drivers/block/loop_image.S). */
    .loopimage : {
        . = ALIGN(4096);
        __loop_image_start = .;
        KEEP(*(.loopimage))
        __loop_image_end = .;
    }
    __image_end = .;
    __image_size = __image_end - __image_start;
    __bss64_start = .;
    .bss : { *(.bss .bss.*) *(COMMON) }
    __bss64_end = .;
    /* Page tables of the Multiboot2 entry: not cleared by _start64, but part of the kernel's memory. */
    .boottables (NOLOAD) : {
        . = ALIGN(4096);
        *(.boottables)
    }
    . = ALIGN(4096);
    __kernel_end = .;
    /DISCARD/ : { *(.comment) *(.eh_frame*) *(.gcc_except_table*) *(.note*) }
}
