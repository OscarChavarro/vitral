/* Protected mode 32-bit -> long mode (x86_64) transition.
   enter_long_mode(uint32_t entry), mangled C++ name _Z15enter_long_modej. Needs the PAE tables
   already in CR3 and CR4.PAE=1 (Pae::activate()) and the kernel image already at its address
   (loaded by the real mode stage). `entry` is the 64-bit entry point of the kernel. */
    .text
    .code32
    .globl _Z15enter_long_modej
_Z15enter_long_modej:
    movl 4(%esp), %ebx          /* entry */
    cli
    movl $0xC0000080, %ecx      /* EFER */
    rdmsr
    orl $0x100, %eax            /* LME */
    wrmsr
    movl %cr0, %eax
    orl $0x80000000, %eax       /* PG: with LME set this activates long mode (compatibility mode) */
    movl %eax, %cr0
    ljmpl $0x18, $lm64          /* load the 64-bit code segment: now in 64-bit mode */

    .code64
lm64:
    movw $0x10, %ax
    movw %ax, %ds
    movw %ax, %es
    movw %ax, %fs
    movw %ax, %gs
    movw %ax, %ss
    movl %ebx, %eax             /* zero-extends */
    jmpq *%rax                  /* kernel entry (entry64.S) */
