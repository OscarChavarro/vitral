/* Interrupt entry stubs: one 16-byte stub per vector, a common path that saves a TrapFrame, calls
   Interrupts::dispatch(TrapFrame *) (mangled C++ name) and resumes the TrapFrame it returns. That
   is how the scheduler switches processes: it returns the frame of another one. */
    .code64
    .text
    .globl interrupt_stubs
    .balign 16
interrupt_stubs:
    .set vec, 0
    .rept 256
    /* vectors for which the CPU pushes an error code: 8, 10-14, 17, 21, 29, 30 */
    .if (vec == 8) || ((vec >= 10) && (vec <= 14)) || (vec == 17) || (vec == 21) || (vec == 29) || (vec == 30)
    .else
    pushq $0
    .endif
    pushq $vec
    jmp interrupt_common
    .balign 16
    .set vec, vec + 1
    .endr

interrupt_common:
    pushq %rax
    pushq %rbx
    pushq %rcx
    pushq %rdx
    pushq %rsi
    pushq %rdi
    pushq %rbp
    pushq %r8
    pushq %r9
    pushq %r10
    pushq %r11
    pushq %r12
    pushq %r13
    pushq %r14
    pushq %r15
    movq %rsp, %rdi             /* TrapFrame * */
    cld
    callq _ZN10Interrupts8dispatchEP9TrapFrame
    movq %rax, %rsp             /* the frame to resume (the same one, or another process's) */
    popq %r15
    popq %r14
    popq %r13
    popq %r12
    popq %r11
    popq %r10
    popq %r9
    popq %r8
    popq %rbp
    popq %rdi
    popq %rsi
    popq %rdx
    popq %rcx
    popq %rbx
    popq %rax
    addq $16, %rsp              /* vector + error code */
    iretq
