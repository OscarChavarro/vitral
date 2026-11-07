/* i386 kernel image: header (read by the real mode loader), then the entry point. The image is linked at 1 MB
   (boot/i386/linker32.ld) and started by boot/i386/pm32kernel.S in 32-bit protected mode, with BootInfo in place. */
    .code32
    .section .header, "a"
    .long 0x4E524B56            /* KernelHeader::MAGIC "VKRN" */
    .long __image_size
    .long _start32
    .long __kernel_end

/* Entry: clears .bss, sets the boot stack and enters C++ through the mangled name of kernelMain(). Started by the BIOS
   boot path (boot/i386/pm32kernel.S) with BootInfo already in place, or by a Multiboot2 loader
   (boot/i386/multiboot2_32.S), in which case multiboot_info is set and the BootInfo is built here. */
    .section .entry32, "ax"
    .globl _start32
_start32:
    cld
    movl $__bss32_start, %edi
    movl $__bss32_end, %ecx
    subl %edi, %ecx
    xorl %eax, %eax
    rep stosb
    movl $0x80000, %esp
    xorl %ebp, %ebp
    movl multiboot_info, %eax
    testl %eax, %eax
    jz 2f
    pushl %eax
    calll _ZN10Multiboot212fillBootInfoEj   /* Multiboot2::fillBootInfo(uint32_t) */
    addl $4, %esp
2:  calll _Z10kernelMainv       /* kernelMain() */
1:  cli
    hlt
    jmp 1b
