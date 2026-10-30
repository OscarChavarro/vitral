/* 64-bit kernel image: header (read by the real mode loader), then the entry point.
   The image is linked at 1 MB (linker64.ld); its first bytes are the header. */
    .section .header, "a"
    .globl kernel_header
kernel_header:
    .long 0x4E524B56            /* KernelHeader::MAGIC "VKRN" */
    .long __image_size
    .long _start64
    .long __kernel_end

/* Entry: clears .bss, sets the boot stack and enters C++ through the mangled name of kernelMain().
   Started by the BIOS boot path (boot/lm.S) with BootInfo already in place, or by a Multiboot2
   loader (boot/multiboot2.S), in which case multiboot_info is set and the BootInfo is built here. */
    .code64
    .section .entry64, "ax"
    .globl _start64
_start64:
    cld
    leaq __bss64_start(%rip), %rdi
    leaq __bss64_end(%rip), %rcx
    subq %rdi, %rcx
    xorl %eax, %eax
    rep stosb
    movq $0x80000, %rsp
    xorl %ebp, %ebp
    movl multiboot_info(%rip), %edi
    testl %edi, %edi
    jz 2f
    callq _ZN10Multiboot212fillBootInfoEj   /* Multiboot2::fillBootInfo(uint32_t) */
2:  callq _Z10kernelMainv       /* kernelMain() */
1:  cli
    hlt
    jmp 1b
