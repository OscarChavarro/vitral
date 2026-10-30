/* Interrupt entry stubs (32-bit protected mode): one 16-byte stub per vector, a common path that saves a TrapFrame
   (kernel/i386/TrapFrame.h), calls Interrupts::dispatch(TrapFrame *) (mangled C++ name) and resumes the TrapFrame it
   returns. That is how the scheduler switches processes: it returns the frame of another one. */
    .code32
    .text
    .globl interrupt_stubs
    .balign 16
interrupt_stubs:
    .set vec, 0
    .rept 256
    /* vectors for which the CPU pushes an error code: 8, 10-14, 17, 21, 29, 30 */
    .if (vec == 8) || ((vec >= 10) && (vec <= 14)) || (vec == 17) || (vec == 21) || (vec == 29) || (vec == 30)
    .else
    pushl $0
    .endif
    pushl $vec
    jmp interrupt_common
    .balign 16
    .set vec, vec + 1
    .endr

interrupt_common:
    pushal
    movl %esp, %eax
    cld
    pushl %eax                  /* TrapFrame * */
    calll _ZN10Interrupts8dispatchEP9TrapFrame   /* Interrupts::dispatch(TrapFrame *) */
    movl %eax, %esp             /* the frame to resume (the same one, or another process's) */
    popal
    addl $8, %esp               /* vector + error code */
    iretl
