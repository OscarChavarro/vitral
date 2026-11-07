/* Multiboot2 support for the i386 image: lets GRUB (or any Multiboot2 loader) boot kernel32.bin directly.

   As in boot/multiboot2.S, the header follows the KernelHeader and describes the flat image with an address tag. The
   loader enters mb2_entry32 in 32-bit protected mode with flat segments, paging off, A20 on, EAX = 0x36D76289 and
   EBX = physical address of the Multiboot2 information, which is all the i386 kernel needs: mb2_entry32 records the
   information and continues at _start32 (entry32kernel.S), which builds the BootInfo from it (kernel/Multiboot2.cpp).

   There is no framebuffer tag: with one, GRUB 2.06 sets a graphics mode even when gfxpayload=text and the tag is
   optional. The console flags tag instead asks for the EGA text console, so GRUB leaves the VGA text mode, which the
   kernel keeps and the VGA programs change by programming the VGA registers. */
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
    .long mb2_entry32
    .long 0                                         /* padding to 8 bytes */

    .short 4, 0                                     /* console flags tag */
    .long 12
    .long 2                                         /* EGA text supported */
    .long 0                                         /* padding to 8 bytes */

    .short 0, 0                                     /* end tag */
    .long 8
mb2_header_end:

    .section .data
    .globl multiboot_info
    .balign 4
multiboot_info: .long 0                             /* physical address of the info; 0 = not Multiboot2 */

    .section .rodata
mb2_msg_loader: .asciz "MULTIBOOT2 ERROR: not started by a Multiboot2 loader (bad magic)."

    .section .multiboot_entry, "ax"
    .code32
    .globl mb2_entry32
mb2_entry32:
    cli
    cld
    cmpl $0x36D76289, %eax
    jne 1f
    movl %ebx, multiboot_info
    jmp _start32

1:  movl $mb2_msg_loader, %esi  /* white on red at the top left of the VGA text screen */
    movl $0xB8000, %edi
    movb $0x4F, %ah
2:  lodsb
    testb %al, %al
    jz 3f
    stosw
    jmp 2b
3:  cli
    hlt
    jmp 3b
