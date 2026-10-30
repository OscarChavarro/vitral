/* Start-up code of an application processor (AP). The BSP copies this blob to physical 0x8000
   (AP_TRAMPOLINE_BASE) and sends it a SIPI for page 0x08. The AP starts in real mode and goes
   16 -> 32 -> 64 bits using the kernel's page tables, then calls Smp::apMain() on its own stack.
   The three data slots are filled by the BSP before the SIPI (one AP is started at a time). */
#define BASE 0x8000

    .section .trampoline, "ax"
    .globl ap_trampoline_start, ap_trampoline_end, ap_data_cr3, ap_data_stack, ap_data_entry

    .code16
ap_trampoline_start:
    cli
    cld
    movw %cs, %ax               /* CS = BASE >> 4 after the SIPI */
    movw %ax, %ds
    lgdt ap_gdt_desc - ap_trampoline_start
    movl %cr0, %eax
    orl $1, %eax
    movl %eax, %cr0
    ljmpl $0x08, $(BASE + ap_protected - ap_trampoline_start)

    .code32
ap_protected:
    movw $0x10, %ax
    movw %ax, %ds
    movw %ax, %es
    movw %ax, %fs
    movw %ax, %gs
    movw %ax, %ss
    movl %cr4, %eax
    orl $0x20, %eax             /* PAE */
    movl %eax, %cr4
    movl BASE + (ap_data_cr3 - ap_trampoline_start), %eax
    movl %eax, %cr3
    movl $0xC0000080, %ecx      /* EFER.LME */
    rdmsr
    orl $0x100, %eax
    wrmsr
    movl %cr0, %eax
    orl $0x80000000, %eax       /* PG */
    movl %eax, %cr0
    ljmpl $0x18, $(BASE + ap_long - ap_trampoline_start)

    .code64
ap_long:
    movw $0x10, %ax
    movw %ax, %ds
    movw %ax, %es
    movw %ax, %ss
    xorl %eax, %eax
    movw %ax, %fs
    movw %ax, %gs
    movq ap_data_stack(%rip), %rsp
    xorl %ebp, %ebp
    jmpq *ap_data_entry(%rip)

    .balign 8
ap_gdt:
    .quad 0
    .quad 0x00CF9A000000FFFF    /* 0x08: code 32 */
    .quad 0x00CF92000000FFFF    /* 0x10: data */
    .quad 0x00AF9A000000FFFF    /* 0x18: code 64 */
ap_gdt_end:
ap_gdt_desc:
    .word ap_gdt_end - ap_gdt - 1
    .long BASE + (ap_gdt - ap_trampoline_start)

    .balign 8
ap_data_cr3:    .quad 0
ap_data_stack:  .quad 0
ap_data_entry:  .quad 0
ap_trampoline_end:
