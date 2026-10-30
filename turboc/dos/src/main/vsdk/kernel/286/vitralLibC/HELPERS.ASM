; Borland large-model compiler ABI. Pure 16-bit operations, no OS calls.
; Long multiply: DX:AX * CX:BX -> DX:AX (low 32 bits).
; Long shifts: DX:AX by CL -> DX:AX.
; Structure copy: CX bytes, two far pointers on stack, callee pops 8 bytes.
.286
jumps
.model large
.code
public F_LXMUL@, F_LXLSH@, F_LXRSH@, F_LXURSH@, F_SCOPY@
public F_LDIV@, F_LUDIV@, F_LMOD@, F_LUMOD@
public F_SPUSH@

; Push a by-value structure: DX:AX source, CX bytes. Relocate the far
; return address below the new argument, preserving BP/DS/SI/DI.
F_SPUSH@ proc far
    pop bx
    pop es
    sub sp, cx
    push es
    push bx
    push bp
    mov bp, sp
    push ds
    push si
    push di
    lea di, [bp+6]
    mov si, ax
    mov ax, ss
    mov es, ax
    mov ds, dx
    cld
    rep movsb
    mov ax, si
    pop di
    pop si
    pop ds
    pop bp
    retf
F_SPUSH@ endp

; Division ABI: dividend then divisor are 32-bit stack arguments.
; Return DX:AX and remove eight argument bytes. Signed division truncates
; toward zero; signed remainder follows the dividend's sign.
F_LDIV@ proc far
    xor ax, ax
    jmp divideEntry
F_LDIV@ endp
F_LUDIV@ proc far
    mov ax, 1
    jmp divideEntry
F_LUDIV@ endp
F_LMOD@ proc far
    mov ax, 2
    jmp divideEntry
F_LMOD@ endp
F_LUMOD@ proc far
    mov ax, 3
    jmp divideEntry
F_LUMOD@ endp

divideEntry:
    push bp
    mov bp, sp
    push bx
    push cx
    push si
    push di
    sub sp, 6
    mov [bp-10], ax
    mov word ptr [bp-12], 0
    mov word ptr [bp-14], 32
    mov ax, [bp+6]
    mov dx, [bp+8]
    mov bx, [bp+10]
    mov cx, [bp+12]
    mov si, bx
    or si, cx
    jnz divisorPresent
    ; Preserve the CPU's arithmetic-fault semantics for division by zero.
    div bx
divisorPresent:
    test word ptr [bp-10], 1
    jnz magnitudesReady
    test dx, 8000h
    jz dividendPositive
    neg dx
    neg ax
    sbb dx, 0
    mov word ptr [bp-12], 3
dividendPositive:
    test cx, 8000h
    jz magnitudesReady
    neg cx
    neg bx
    sbb cx, 0
    xor word ptr [bp-12], 1
magnitudesReady:
    xor si, si
    xor di, di
divideBit:
    shl ax, 1
    rcl dx, 1
    rcl si, 1
    rcl di, 1
    jc subtractDivisor
    cmp di, cx
    jb nextDivideBit
    ja subtractDivisor
    cmp si, bx
    jb nextDivideBit
subtractDivisor:
    sub si, bx
    sbb di, cx
    or ax, 1
nextDivideBit:
    dec word ptr [bp-14]
    jnz divideBit
    test word ptr [bp-10], 2
    jz quotientResult
    mov ax, si
    mov dx, di
    test word ptr [bp-12], 2
    jmp checkResultSign
quotientResult:
    test word ptr [bp-12], 1
checkResultSign:
    jz divisionDone
    neg dx
    neg ax
    sbb dx, 0
divisionDone:
    add sp, 6
    pop di
    pop si
    pop cx
    pop bx
    pop bp
    retf 8

F_LXMUL@ proc far
    push bp
    mov bp, sp
    push si
    push di
    push bx
    push cx
    push ax
    push dx
    mul bx
    mov si, ax
    mov di, dx
    mov ax, [bp-12]
    mul bx
    add di, ax
    mov ax, [bp-10]
    mul word ptr [bp-8]
    add di, ax
    mov ax, si
    mov dx, di
    add sp, 4
    pop cx
    pop bx
    pop di
    pop si
    pop bp
    retf
F_LXMUL@ endp

F_LXLSH@ proc far
    push cx
    and cx, 00FFh
    jcxz leftDone
leftLoop:
    shl ax, 1
    rcl dx, 1
    loop leftLoop
leftDone:
    pop cx
    retf
F_LXLSH@ endp

F_LXRSH@ proc far
    push cx
    and cx, 00FFh
    jcxz signedDone
signedLoop:
    sar dx, 1
    rcr ax, 1
    loop signedLoop
signedDone:
    pop cx
    retf
F_LXRSH@ endp

F_LXURSH@ proc far
    push cx
    and cx, 00FFh
    jcxz unsignedDone
unsignedLoop:
    shr dx, 1
    rcr ax, 1
    loop unsignedLoop
unsignedDone:
    pop cx
    retf
F_LXURSH@ endp

F_SCOPY@ proc far
    push bp
    mov bp, sp
    push si
    push di
    push ds
    push es
    lds si, [bp+6]
    les di, [bp+10]
    cld
    rep movsb
    pop es
    pop ds
    pop di
    pop si
    pop bp
    retf 8
F_SCOPY@ endp
end
