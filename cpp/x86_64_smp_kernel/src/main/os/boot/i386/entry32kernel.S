/* i386 kernel image: header (read by the real mode loader), then the entry point. The image is linked at 1 MB
   (boot/i386/linker32.ld) and started by boot/i386/pm32kernel.S in 32-bit protected mode, with BootInfo in place. */
    .code32
    .section .header, "a"
    .long 0x4E524B56            /* KernelHeader::MAGIC "VKRN" */
    .long __image_size
    .long _start32
    .long __kernel_end

/* Entry: clears .bss, sets the boot stack and enters C++ through the mangled name of kernelMain(). */
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
    calll _Z10kernelMainv       /* kernelMain() */
1:  cli
    hlt
    jmp 1b
