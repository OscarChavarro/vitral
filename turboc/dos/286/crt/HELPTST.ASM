; Real-mode ABI probe for the OS-independent arithmetic/copy helpers.
; Link this with HELPERS.OBJ only: no Borland startup or runtime library.
.286
jumps
.model large
.stack 1024
.data
passed db 'CRT helper ABI checks passed.',13,10,'$'
failed db 'CRT helper ABI checks FAILED.',13,10,'$'
source db 'selector copy',0
destination db 14 dup (0)
.code
extrn F_LXMUL@:far, F_LXLSH@:far, F_LXRSH@:far, F_LXURSH@:far, F_SCOPY@:far
extrn F_LDIV@:far, F_LUDIV@:far, F_LMOD@:far, F_LUMOD@:far
extrn F_SPUSH@:far
start:
    mov ax, @data
    mov ds, ax
    ; 0xFFFFFFFF / 0x80000000 = 1. Exercises a high-bit unsigned divisor.
    push 8000h
    push 0
    push 0FFFFh
    push 0FFFFh
    call far ptr F_LUDIV@
    cmp ax, 1
    jne bad
    or dx, dx
    jne bad
    push 8000h
    push 0
    push 0FFFFh
    push 0FFFFh
    call far ptr F_LUMOD@
    cmp ax, 0FFFFh
    jne bad
    cmp dx, 7FFFh
    jne bad
    ; -100000 / 3 = -33333; -100000 % 3 = -1.
    push 0
    push 3
    push 0FFFEh
    push 7960h
    call far ptr F_LDIV@
    cmp ax, 7DCBh
    jne bad
    cmp dx, 0FFFFh
    jne bad
    push 0
    push 3
    push 0FFFEh
    push 7960h
    call far ptr F_LMOD@
    cmp ax, 0FFFFh
    jne bad
    cmp dx, 0FFFFh
    jne bad
    mov ax, 1
    mov dx, 1
    mov bx, 1
    mov cx, 1
    call far ptr F_LXMUL@
    cmp ax, 1
    jne bad
    cmp dx, 2
    jne bad
    mov ax, 5678h
    mov dx, 1234h
    mov cx, 17
    call far ptr F_LXLSH@
    or ax, ax
    jne bad
    cmp dx, 0ACF0h
    jne bad
    xor ax, ax
    mov dx, 8000h
    mov cx, 31
    call far ptr F_LXURSH@
    cmp ax, 1
    jne bad
    or dx, dx
    jne bad
    xor ax, ax
    mov dx, 8000h
    mov cx, 31
    call far ptr F_LXRSH@
    cmp ax, 0FFFFh
    jne bad
    cmp dx, 0FFFFh
    jne bad
    push ds
    mov ax, offset destination
    push ax
    push ds
    mov ax, offset source
    push ax
    mov cx, 14
    call far ptr F_SCOPY@
    push ds
    pop es
    mov si, offset source
    mov di, offset destination
    mov cx, 14
    cld
    repe cmpsb
    jne bad
    mov bp, sp
    mov ax, offset source
    mov dx, ds
    mov cx, 13 ; Exercise an odd byte count and exact stack adjustment.
    call far ptr F_SPUSH@
    mov ax, bp
    sub ax, 13
    cmp ax, sp
    jne bad
    mov ax, ss
    mov es, ax
    mov di, sp
    mov si, offset source
    mov cx, 13
    repe cmpsb
    jne bad
    add sp, 13
    cmp sp, bp
    jne bad
    mov dx, offset passed
    mov ah, 9
    int 21h
    mov ax, 4C00h
    int 21h
bad:
    mov dx, offset failed
    mov ah, 9
    int 21h
    mov ax, 4C01h
    int 21h
end start
