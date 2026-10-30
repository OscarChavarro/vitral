/* copyHigh(uint32_t dest, uint32_t bytes), mangled C++ name _Z8copyHighjj (16-bit, i386 cdecl).
   Copies from the bounce buffer 0x2000:0000 to the linear address `dest` using "unreal mode":
   a flat 4 GB data descriptor is loaded into FS in protected mode and the CPU returns to real mode
   keeping that limit, so FS:EDI can address memory above 1 MB. Needs A20 open. */
    .text
    .code16
    .globl _Z8copyHighjj
_Z8copyHighjj:
    pushl %esi
    pushl %edi
    pushl %ebx
    movl 16(%esp), %edi         /* dest */
    movl 20(%esp), %ecx         /* bytes */
    shrl $2, %ecx               /* dwords */
    cli
    lgdt unreal_gdt_desc
    movl %cr0, %eax
    orb $1, %al
    movl %eax, %cr0             /* protected mode, no segment reload besides FS */
    jmp 1f
1:  movw $0x08, %bx
    movw %bx, %fs               /* FS gets the 4 GB limit */
    andb $0xFE, %al
    movl %eax, %cr0             /* back to real mode: FS keeps its limit */
    jmp 2f
2:  pushw %ds
    movw $0x2000, %ax
    movw %ax, %ds
    xorl %esi, %esi
3:  movl (%esi), %eax
    movl %eax, %fs:(%edi)
    addl $4, %esi
    addl $4, %edi
    decl %ecx
    jnz 3b
    popw %ds
    sti
    popl %ebx
    popl %edi
    popl %esi
    retl

    .balign 8
unreal_gdt:
    .quad 0
    .quad 0x00CF92000000FFFF    /* 0x08: data, base 0, limit 4G */
unreal_gdt_end:
unreal_gdt_desc:
    .word unreal_gdt_end - unreal_gdt - 1
    .long unreal_gdt
