; 80286 real-mode to protected-mode bootstrap.
; The first protected-mode test maps the current DOS image, prints directly
; through VGA at ring 0, waits for a polled key and resets the 286.
.286p
jumps
.model large, c, farstack
; Keep every DGROUP member contiguous. Without DOSSEG, TLINK can place
; PMSTACK between _DATA and _BSS, making DGROUP span more than 64 KiB even
; though the stack is not a member of its OMF group record.
DOSSEG
; Far C++ entry points live in other code segments. Declare them outside
; this module's CODE segment so TASM emits external-segment call fixups.
extrn C protected286KernelMain:far
extrn C protected286PrepareImage:far
extrn C crt286InitializeFpu:far
; The protected stack has its own selector; it must not consume DGROUP.
; Declare it explicitly and use DOSSEG above to keep it outside both the
; membership and the address span of the C++ near-data group.
PMSTACK segment para stack 'STACK'
    db 32768 dup (?)
PMSTACK ends
.data
extrn C protected286CodeBase:dword
extrn C protected286DataBase:dword
extrn C protected286StackBase:dword
extrn C protected286GatewaySegment:word
extrn C protected286DescriptorCount:word
extrn C protected286DescriptorBases:dword
extrn C protected286DescriptorAccess:byte
.code
public C init
public C enterProtected286

; The real-mode entry returns to DOS on setup failure. Once protected mode
; is entered, the protected return path resets the processor.
init proc far
    mov ax, @data
    mov ds, ax
    cld
    mov si, offset traceStart
    xor di, di
    call realTrace
    mov ax, cs
    mov protected286GatewaySegment, ax
    call enterProtected286
init endp

enterProtected286 proc far
    mov si, offset traceLoader
    mov di, 160
    call realTrace
    ; BIOS and INT 21h are real-mode services and may require interrupts.
    ; Do not mask them until the loader has returned successfully.
    call far ptr protected286PrepareImage
    or ax, ax
    jz loaderFailure
    call enableA20
    or ax, ax
    jz loaderFailure
    call far ptr crt286InitializeFpu
    or ax, ax
    jz loaderFailure
    mov si, offset traceLoaded
    mov di, 1600
    call realTrace
    cli
    mov si, offset protected286CodeBase
    mov di, offset gdtCode
    call setHighBase
    mov si, offset protected286DataBase
    mov di, offset gdtData
    call setHighBase
    mov si, offset protected286StackBase
    mov di, offset gdtStack
    call setHighBase
    call installRelocationDescriptors
    call setGdtBase
    lgdt fword ptr cs:[gdtPointer]
    lidt fword ptr cs:[idtPointer]
    mov si, offset traceSwitch
    mov di, 1760
    call realTrace
    mov ax, 1
    lmsw ax
    db 0EAh
    dw offset protectedEntry
    dw 0008h

protectedEntry:
    mov ax, 0018h
    mov es, ax
    mov word ptr es:[1920], 0A50h ; 'P': far jump reached protected code.
    mov ax, 0010h
    mov ds, ax
    mov ax, 0020h
    mov ss, ax
    mov sp, 8000h
    mov ax, 0018h
    mov es, ax
    mov si, offset traceProtected
    mov di, 1920
    call traceCharacters
    ; This deliberately enters the protected kernel, not the Borland CRT.
    ; Its VGA, keyboard and ATA/FAT paths use no real-mode DOS service.
    call far ptr protected286KernelMain
resetWait:
    in al, 64h
    test al, 2
    jnz resetWait
    mov al, 0FEh
    out 64h, al
halted:
    hlt
    jmp halted
loaderFailure:
    ; Still in real mode: release the DOS process and return an error code.
    ; Any memory-manager diagnostic was printed before reaching this point.
    mov ax, 4C01h
    int 21h
    hlt                         ; DOS termination must not return.
    jmp loaderFailure
enterProtected286 endp

; Bootstrap diagnostics never call DOS, BIOS or any C runtime routine.
realTrace proc near
    push ax
    push es
    mov ax, 0B800h
    mov es, ax
    call traceCharacters
    pop es
    pop ax
    ret
realTrace endp

; AT 8042 A20 gate. BIOS block transfers may restore A20 to its old state,
; so enable it again before the permanent protected-mode transition.
enableA20 proc near
    pushf
    cli
    push bx
    push cx
    call waitController
    jz a20Failed
    mov al, 0ADh
    out 64h, al
    mov cx, 32
drainController:
    in al, 64h
    test al, 1
    jz readControllerOutput
    in al, 60h
    loop drainController
    jmp a20Failed
readControllerOutput:
    call waitController
    jz a20Failed
    mov al, 0D0h
    out 64h, al
    mov cx, 0FFFFh
waitControllerOutput:
    in al, 64h
    test al, 1
    jnz controllerOutputReady
    loop waitControllerOutput
    jmp a20Failed
controllerOutputReady:
    in al, 60h
    or al, 3
    mov bl, al
    call waitController
    jz a20Failed
    mov al, 0D1h
    out 64h, al
    call waitController
    jz a20Failed
    mov al, bl
    out 60h, al
    call waitController
    jz a20Failed
    mov al, 0AEh
    out 64h, al
    mov ax, 1
    jmp a20Finished
a20Failed:
    mov al, 0AEh
    out 64h, al
    xor ax, ax
a20Finished:
    pop cx
    pop bx
    popf
    ret
enableA20 endp
waitController proc near
    mov cx, 0FFFFh
waitControllerBusy:
    in al, 64h
    test al, 2
    jz controllerWritable
    loop waitControllerBusy
    xor cx, cx
controllerWritable:
    or cx, cx
    ret
waitController endp
traceCharacters proc near
    push ax
    push si
    push di
    pushf
    cld
traceCharacter:
    mov al, cs:[si]
    inc si
    or al, al
    jz traceFinished
    mov ah, 0Fh
    stosw
    jmp traceCharacter
traceFinished:
    popf
    pop di
    pop si
    pop ax
    ret
traceCharacters endp

; DS:SI addresses a 32-bit physical base; DI is the descriptor's GDT offset.
setHighBase proc near
    mov ax, word ptr ds:[si]
    mov word ptr cs:[di+2], ax
    mov al, byte ptr ds:[si+2]
    mov byte ptr cs:[di+4], al
    ret
setHighBase endp

; The relocation pass assigns code selectors starting at 28h.  It gives every
; 64 KiB high-image block a descriptor before the far jump into the copy.
installRelocationDescriptors proc near
    mov cx, word ptr ds:[protected286DescriptorCount]
    mov si, offset protected286DescriptorBases
    mov bx, offset protected286DescriptorAccess
    mov di, 28h
installDescriptorLoop:
    jcxz installDescriptorDone
    mov word ptr cs:[di+gdt], 0FFFFh
    mov ax, word ptr ds:[si]
    mov word ptr cs:[di+gdt+2], ax
    mov al, byte ptr ds:[si+2]
    mov byte ptr cs:[di+gdt+4], al
    mov al, byte ptr ds:[bx]
    mov byte ptr cs:[di+gdt+5], al
    add si, 4
    inc bx
    add di, 8
    loop installDescriptorLoop
installDescriptorDone:
    ; Dedicated data descriptors for 1--16 MiB. The allocator exposes only
    ; blocks inside the exclusively owned BIOS RAM range, excluding the image.
    mov di, 800h
    mov bx, 10h
    mov cx, 240
installMemoryLoop:
    mov word ptr cs:[di+gdt], 0FFFFh
    mov word ptr cs:[di+gdt+2], 0
    mov byte ptr cs:[di+gdt+4], bl
    mov byte ptr cs:[di+gdt+5], 92h
    inc bx
    add di, 8
    loop installMemoryLoop
    ret
installRelocationDescriptors endp

setGdtBase proc near
    mov ax, cs
    xor dx, dx
    mov cl, 4
gdtShift:
    shl ax, 1
    rcl dx, 1
    dec cl
    jnz gdtShift
    add ax, offset gdt
    adc dx, 0
    mov word ptr cs:[gdtPointer+2], ax
    mov word ptr cs:[gdtPointer+4], dx
    ret
setGdtBase endp

align 8
gdt:
    dq 0
gdtCode:
    dw 0FFFFh, 0, 09A00h, 0
gdtData:
    dw 0FFFFh, 0, 09200h, 0
gdtVideo:
    dw 07FFFh, 8000h, 0920Bh, 0
gdtStack:
    dw 0FFFFh, 0, 09200h, 0
; Selectors 28h..7E0h are populated by installRelocationDescriptors.
gdtRelocations db (248 * 8) dup (0)
    db (3 * 8) dup (0)
gdtMemory db (240 * 8) dup (0)
gdtEnd label byte
gdtPointer:
    dw gdtEnd - gdt - 1
    dd 0
idtPointer:
    dw 0
    dd 0

traceStart db '286 loader: [00] Real-mode entry reached.',0
traceLoader db '286 loader: [01] Preparing physical image (BIOS/DOS used only here).',0
traceLoaded db '286 loader: [10] Protected image prepared; constructing GDT.',0
traceSwitch db '286 loader: [11] GDT/IDT loaded; setting PE and taking far jump.',0
traceProtected db '286 loader: [12] Protected mode active; selectors loaded; entering C++.',0

; With no Borland C0L startup object linked, this END operand is the DOS EXE
; entry address.  It must remain a real-mode address because the loader has
; not copied the image or set PE yet.
end init
