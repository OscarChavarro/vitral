; Hardware 80x87 fixup ABI from Borland FP87, without its DOS initializer.
.286p
.287
.model large
public FIARQQ, FICRQQ, FIDRQQ, FISRQQ, FIWRQQ, FIERQQ
public FJARQQ, FJCRQQ, FJSRQQ
FIARQQ equ 0
FICRQQ equ 0
FIDRQQ equ 0
FISRQQ equ 0
FIWRQQ equ 0
FIERQQ equ 0
FJARQQ equ 0
FJCRQQ equ 0
FJSRQQ equ 0
.data
fpuStatus dw 0FFFFh
.code
public _crt286InitializeFpu
_crt286InitializeFpu proc far
    smsw ax
    and ax, 0FFF3h ; Clear EM/TS without changing PE.
    lmsw ax
    ; No WAIT until presence is confirmed: an absent 287 must not hang.
    mov fpuStatus, 0FFFFh
    fninit
    fnstsw fpuStatus
    mov cx, 0FFFFh
pollFpu:
    cmp fpuStatus, 0
    je fpuReady
    loop pollFpu
    xor ax, ax
    retf
fpuReady:
    mov ax, 1
    retf
_crt286InitializeFpu endp
public _crt286TangentQuarter
_crt286TangentQuarter proc far
    push bp
    mov bp, sp
    fld qword ptr [bp+6]
    ; |argument| <= pi/4. FPTAN is native on the 287 and leaves
    ; numerator in ST(1), denominator in ST(0).
    fptan
    fdivp st(1), st
    pop bp
    retf
_crt286TangentQuarter endp
public _crt286AtanUnit
_crt286AtanUnit proc far
    push bp
    mov bp, sp
    fld qword ptr [bp+6]
    fld1
    ; 287 FPATAN requires 0 <= ST(1) <= ST(0).
    fpatan
    pop bp
    retf
_crt286AtanUnit endp
public _crt286Log2
_crt286Log2 proc far
    push bp
    mov bp, sp
    fld1
    fld qword ptr [bp+6]
    fyl2x
    pop bp
    retf
_crt286Log2 endp
public _crt286Exp2
_crt286Exp2 proc far
    push bp
    mov bp, sp
    fld qword ptr [bp+6]
    fld st(0)
    frndint
    fxch st(1)
    fsub st, st(1)
    ; FNINIT selects round-to-nearest, so fraction is in [-.5,.5],
    ; the domain supported by the 287 F2XM1 instruction.
    f2xm1
    fld1
    faddp st(1), st
    fscale
    fstp st(1)
    pop bp
    retf
_crt286Exp2 endp
end
