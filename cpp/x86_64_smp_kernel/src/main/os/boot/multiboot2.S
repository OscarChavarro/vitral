/* Multiboot2 support: lets GRUB (or any Multiboot2 loader) boot the kernel image directly.

   kernel64.bin is a flat image linked at 1 MB. Its first 16 bytes are our own KernelHeader (used by
   the BIOS boot path in boot/), and right after it comes this Multiboot2 header, which describes the
   image with an "address tag" (no ELF needed) and names mb2_entry as the entry point.

   The loader enters mb2_entry in 32-bit protected mode, paging off, A20 on, EAX = 0x36D76289 and
   EBX = physical address of the Multiboot2 information. mb2_entry checks that the CPU has long mode,
   builds a 4 GB identity map, switches to long mode and continues at _start64 (entry64.S), which then
   converts the loader's memory map into the BootInfo the kernel uses (kernel/multiboot2.cpp). */
#define MB2_MAGIC 0xE85250D6

    .section .multiboot2, "a"
    .balign 8
mb2_header_start:
    .long MB2_MAGIC
    .long 0                                         /* architecture: i386 */
    .long mb2_header_end - mb2_header_start
    .long (0x100000000 - (MB2_MAGIC + (mb2_header_end - mb2_header_start))) & 0xFFFFFFFF

    .short 2, 0                                     /* address tag */
    .long 24
    .long mb2_header_start                          /* header_addr */
    .long __image_start                             /* load_addr */
    .long __image_end                               /* load_end_addr: end of the file's data */
    .long __kernel_end                              /* bss_end_addr: the loader zeroes up to here */

    .short 3, 0                                     /* entry address tag */
    .long 12
    .long mb2_entry
    .long 0                                         /* padding to 8 bytes */

    .short 5, 0                                     /* framebuffer tag: loader-selected mode */
    .long 20
    .long 0                                         /* width */
    .long 0                                         /* height */
    .long 0                                         /* depth */
    .long 0                                         /* padding to 8 bytes */

    .short 0, 0                                     /* end tag */
    .long 8
mb2_header_end:

    .section .data
    .globl multiboot_info
    .balign 8
multiboot_info: .quad 0                             /* physical address of the info; 0 = not Multiboot2 */

    .section .rodata
msg_loader: .asciz "MULTIBOOT2 ERROR: not started by a Multiboot2 loader (bad magic)."
msg_cpu:    .asciz "CPU ERROR: long mode x86_64 not supported (needs AMD64 / Intel 64 CPU)."

    .balign 8
mb2_gdt:
    .quad 0
    .quad 0x00CF9A000000FFFF    /* 0x08: code, 32-bit */
    .quad 0x00CF92000000FFFF    /* 0x10: data */
    .quad 0x00AF9A000000FFFF    /* 0x18: code, 64-bit */
mb2_gdt_end:
mb2_gdt_ptr:
    .word mb2_gdt_end - mb2_gdt - 1
    .long mb2_gdt

/* Boot-time page tables and stack. Outside the range _start64 clears as .bss (it would wipe the
   tables in use) but inside the kernel's memory, so the physical allocator never hands them out. */
    .section .boottables, "aw", @nobits
    .balign 4096
mb2_pml4:   .skip 4096
mb2_pdpt:   .skip 4096
mb2_pd:     .skip 4 * 4096
mb2_stack:  .skip 4096
mb2_stack_top:

    .section .multiboot_entry, "ax"
    .code32
    .globl mb2_entry
mb2_entry:
    cli
    cld
    cmpl $0x36D76289, %eax
    jne error_loader
    movl %ebx, multiboot_info
    movl $mb2_stack_top, %esp

    /* CPUID present? (EFLAGS.ID can be toggled) */
    pushfl
    popl %eax
    movl %eax, %ecx
    xorl $0x200000, %eax
    pushl %eax
    popfl
    pushfl
    popl %eax
    pushl %ecx
    popfl
    xorl %ecx, %eax
    jz error_cpu
    /* long mode? (CPUID.80000001h:EDX[29]) */
    movl $0x80000000, %eax
    cpuid
    cmpl $0x80000001, %eax
    jb error_cpu
    movl $0x80000001, %eax
    cpuid
    btl $29, %edx
    jnc error_cpu

    /* Identity map of the first 4 GB with 2 MB pages: PML4 -> PDPT -> 4 PDs. */
    movl $mb2_pml4, %edi
    xorl %eax, %eax
    movl $(6 * 1024), %ecx
    rep stosl
    movl $mb2_pdpt + 0x3, %eax
    movl %eax, mb2_pml4
    movl $mb2_pd + 0x3, %eax
    movl %eax, mb2_pdpt
    movl $mb2_pd + 0x1003, %eax
    movl %eax, mb2_pdpt + 8
    movl $mb2_pd + 0x2003, %eax
    movl %eax, mb2_pdpt + 16
    movl $mb2_pd + 0x3003, %eax
    movl %eax, mb2_pdpt + 24
    movl $mb2_pd, %edi
    movl $0x83, %eax            /* present, writable, 2 MB page */
    movl $2048, %ecx
1:  movl %eax, (%edi)
    addl $8, %edi
    addl $0x200000, %eax
    loop 1b

    lgdt mb2_gdt_ptr
    ljmpl $0x08, $2f
2:  movw $0x10, %ax
    movw %ax, %ds
    movw %ax, %es
    movw %ax, %fs
    movw %ax, %gs
    movw %ax, %ss
    movl %cr4, %eax
    orl $0x20, %eax             /* PAE */
    movl %eax, %cr4
    movl $mb2_pml4, %eax
    movl %eax, %cr3
    movl $0xC0000080, %ecx      /* EFER.LME */
    rdmsr
    orl $0x100, %eax
    wrmsr
    movl %cr0, %eax
    orl $0x80000000, %eax       /* PG */
    movl %eax, %cr0
    ljmpl $0x18, $mb2_long

error_loader:
    movl $msg_loader, %esi
    jmp error_show
error_cpu:
    movl $msg_cpu, %esi
error_show:                     /* white on red at the top left of the VGA text screen */
    movl $0xB8000, %edi
    movb $0x4F, %ah
3:  lodsb
    testb %al, %al
    jz 4f
    stosw
    jmp 3b
4:  cli
    hlt
    jmp 4b

    .code64
mb2_long:
    movw $0x10, %ax
    movw %ax, %ds
    movw %ax, %es
    movw %ax, %ss
    xorl %eax, %eax
    movw %ax, %fs
    movw %ax, %gs
    jmp _start64
