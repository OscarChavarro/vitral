/* Start-up code of an application processor (AP) for the i386 kernel. The BSP copies this blob to physical 0x8000
   (AP_TRAMPOLINE_BASE) and sends it a SIPI for page 0x08. The AP starts in real mode and goes 16 -> 32 bits (no
   paging), then calls Smp::apMain() on its own stack. The data slots are the same as in the x86_64 trampoline (and
   filled the same way by Smp::startProcessor()); ap_data_cr3 is not used. Only the low half of each slot is read. */
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
    movw %ax, %ss
    xorl %eax, %eax
    movw %ax, %fs
    movw %ax, %gs
    movl BASE + (ap_data_stack - ap_trampoline_start), %esp
    xorl %ebp, %ebp
    jmpl *BASE + (ap_data_entry - ap_trampoline_start)

    .balign 8
ap_gdt:
    .quad 0
    .quad 0x00CF9A000000FFFF    /* 0x08: code 32 */
    .quad 0x00CF92000000FFFF    /* 0x10: data */
ap_gdt_end:
ap_gdt_desc:
    .word ap_gdt_end - ap_gdt - 1
    .long BASE + (ap_gdt - ap_trampoline_start)

    .balign 8
ap_data_cr3:    .quad 0
ap_data_stack:  .quad 0
ap_data_entry:  .quad 0
ap_trampoline_end:
