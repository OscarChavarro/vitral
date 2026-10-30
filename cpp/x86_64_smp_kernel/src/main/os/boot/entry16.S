/* Stage 2 entry (real mode): checks the CPU can enter protected mode, sets up the stack
   and enters C++.  Until the CPU is known to be a 386+ only 8086 instructions are used,
   since this code may run on an 8086/8088/80186/80286. */
    .code16
    .section .entry16, "ax"
    .globl stage2_entry
stage2_entry:
    xorw %ax, %ax
    movw %ax, %ds               /* the error messages are addressed through DS=0 */
#if defined(SIMULATE_CPU)       /* test builds only (scripts/run16.sh) */
#if SIMULATE_CPU == 1
    movw $msg_8086, %si
#else
    movw $msg_286, %si
#endif
    jmp cpu_error
#endif
    pushfw
    /* Write 0 to FLAGS: 8086/80186 always read bits 12-15 back as 1. */
    pushw %ax
    popfw
    pushfw
    popw %ax
    andw $0xF000, %ax
    cmpw $0xF000, %ax
    movw $msg_8086, %si
    je cpu_error
    /* Write 1s to bits 12-15: on a 80286 in real mode they always read back as 0. */
    movw $0xF000, %ax
    pushw %ax
    popfw
    pushfw
    popw %ax
    andw $0xF000, %ax
    movw $msg_286, %si
    jz cpu_error
    popfw                       /* 80386 or later: restore the original FLAGS */

    movzbl %dl, %eax            /* BIOS boot drive (DL) -> BootInfo.bootDrive at 0x1004 */
    movl %eax, 0x1004

    xorw %ax, %ax
    movw %ax, %es
    movw %ax, %ss
    movl $0x7C00, %esp
    cld
    calll _Z5kmainv              /* kmain() */
1:  nop
    jmp 1b

cpu_error:                      /* SI = message; BIOS teletype, 8086-only code */
    cld
    lodsb
    testb %al, %al
    jz 2f
    movb $0x0E, %ah
    movw $0x0007, %bx
    int $0x10
    jmp cpu_error
2:  cli
    hlt
    jmp 2b

msg_8086: .asciz "CPU ERROR: 8086/8088/80186 detected. 32-bit protected mode needs an 80386 or later.\r\n"
msg_286:  .asciz "CPU ERROR: 80286 detected. 32-bit protected mode needs an 80386 or later.\r\n"
