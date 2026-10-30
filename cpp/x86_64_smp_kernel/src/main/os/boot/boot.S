/* Boot sector: loads stage 2 with INT 13h and jumps to it. */
    .code16
    .section .boot, "ax"
    .globl _start
_start:
    cli
    xorw %ax, %ax
    movw %ax, %ds
    movw %ax, %es
    movw %ax, %ss
    movw $0x7C00, %sp
    sti
    movb %dl, boot_drive

    /* Geometry of the boot drive (INT 13h AH=08h: CL bits 0-5 = sectors per track, DH = last head). A hard disk
       (QEMU: 63 sectors, 16 heads) differs from a floppy; if the BIOS does not answer, assume a 1.44MB floppy. */
    movb $0x08, %ah
    movb boot_drive, %dl
    xorw %di, %di               /* ES:DI = 0:0 works around some BIOSes */
    int $0x13
    jc 1f
    andb $0x3F, %cl
    jz 1f
    movb %cl, track_sectors
    movb %dh, last_head
1:

    /* Read stage 2 (__stage2_sectors sectors from sector 2) to 0x7E00, one track at a time:
       a CHS read must not cross a track. ES advances by 32 paragraphs per sector, so the destination may pass 64 KB.
       8086 code. */
    movw $0x07E0, %ax
    movw %ax, %es
    xorw %bx, %bx
    movb $__stage2_sectors, remaining
    movb $2, %cl                /* sector 2 (sector 1 is this one) */
    xorb %dh, %dh               /* head 0 */
    xorb %ch, %ch               /* cylinder 0 */
read_track:
    movb track_sectors, %al
    incb %al
    subb %cl, %al               /* sectors left in this track: sectors per track + 1 - sector */
    cmpb remaining, %al
    jbe 1f
    movb remaining, %al
1:  movb %al, chunk
    movb $0x02, %ah             /* read sectors (CHS) */
    movb boot_drive, %dl
    int $0x13
    jc disk_error
    movb chunk, %al
    xorb %ah, %ah
    addw %ax, %ax               /* x32 paragraphs per 512-byte sector */
    addw %ax, %ax
    addw %ax, %ax
    addw %ax, %ax
    addw %ax, %ax
    movw %es, %si
    addw %ax, %si
    movw %si, %es
    movb chunk, %al
    subb %al, remaining
    jz loaded
    movb $1, %cl                /* next track: sector 1 of the next head */
    cmpb last_head, %dh
    je 2f
    incb %dh
    jmp read_track
2:  xorb %dh, %dh               /* back to head 0: next cylinder */
    incb %ch
    jmp read_track
loaded:
    xorw %ax, %ax
    movw %ax, %es
    movb boot_drive, %dl
    ljmp $0, $stage2_entry

disk_error:
    movw $msg_err, %si
1:  lodsb
    testb %al, %al
    jz 2f
    movb $0x0E, %ah
    int $0x10
    jmp 1b
2:  hlt
    jmp 2b

boot_drive: .byte 0
track_sectors: .byte 18
last_head:  .byte 1
remaining:  .byte 0
chunk:      .byte 0
msg_err:    .asciz "Disk error"

    .org 510
    .word 0xAA55
