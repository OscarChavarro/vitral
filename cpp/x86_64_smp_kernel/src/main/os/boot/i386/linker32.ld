/* Flat i386 kernel image, linked at 1 MB: the same layout as linker64.ld without the Multiboot2 header. Starts with
   the KernelHeader (boot/i386/entry32kernel.S). The real mode loader copies the file there; .bss is not in the file
   and is cleared by _start32. */
ENTRY(_start32)
SECTIONS
{
    . = 0x100000;
    __image_start = .;
    .header : { KEEP(*(.header)) }
    .text : {
        KEEP(*(.entry32))
        *(.text .text.*)
    }
    .rodata : { *(.rodata .rodata.*) }
    /* Real mode start-up code of the application processors, copied to low memory at run time. */
    .trampoline : { KEEP(*(.trampoline)) }
    /* Global constructors, run by Runtime::init(). */
    .init_array : {
        . = ALIGN(4);
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
    __bss32_start = .;
    .bss : { *(.bss .bss.*) *(COMMON) }
    __bss32_end = .;
    . = ALIGN(4096);
    __kernel_end = .;
    /DISCARD/ : { *(.comment) *(.eh_frame*) *(.gcc_except_table*) *(.note*) }
}
