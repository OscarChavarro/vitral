/* The i386 image starts directly in protected mode: no PAE or long mode transition. */
    .text
    .code16
    .globl _Z15enter_protectedv
_Z15enter_protectedv:
    cli
    lgdt gdt_desc
    movl %cr0, %eax
    orl $1, %eax
    movl %eax, %cr0
    ljmpl $0x08, $pm32

    .code32
pm32:
    movw $0x10, %ax
    movw %ax, %ds
    movw %ax, %es
    movw %ax, %fs
    movw %ax, %gs
    movw %ax, %ss
    movl $0x80000, %esp
    movl 0x1018, %eax           /* BootInfo::kernelEntry */
    jmp *%eax

    .balign 8
gdt:
    .quad 0
    .quad 0x00CF9A000000FFFF
    .quad 0x00CF92000000FFFF
gdt_end:
gdt_desc:
    .word gdt_end - gdt - 1
    .long gdt
