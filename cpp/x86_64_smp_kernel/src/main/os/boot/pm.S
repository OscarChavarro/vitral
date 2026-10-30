/* Real mode -> protected mode 32-bit transition (GDT, CR0.PE, far jump). The GDT also holds the
   64-bit code segment used by lm.S.
   Defines enter_protected() and calls kmain32() by their mangled C++ names. */
    .text
    .code16
    .globl _Z15enter_protectedv
_Z15enter_protectedv:
    cli
    lgdt gdt_desc
    movl %cr0, %eax
    orl $1, %eax                /* CR0.PE */
    movl %eax, %cr0
    ljmpl $0x08, $pm32          /* far jump: loads CS with the 32-bit code segment */

    .code32
pm32:
    movw $0x10, %ax             /* flat 32-bit data segment */
    movw %ax, %ds
    movw %ax, %es
    movw %ax, %fs
    movw %ax, %gs
    movw %ax, %ss
    movl $0x7C00, %esp

    calll _Z7kmain32v           /* kmain32(): C++ in 32-bit mode; never returns */
1:  hlt
    jmp 1b

    .balign 8
gdt:
    .quad 0                     /* 0x00: null */
    .quad 0x00CF9A000000FFFF    /* 0x08: code, base 0, limit 4G, 32-bit, ring 0 */
    .quad 0x00CF92000000FFFF    /* 0x10: data, base 0, limit 4G, 32-bit, ring 0 */
    .quad 0x00AF9A000000FFFF    /* 0x18: code, 64-bit (L=1, D=0), ring 0: for long mode */
gdt_end:
gdt_desc:
    .word gdt_end - gdt - 1
    .long gdt
