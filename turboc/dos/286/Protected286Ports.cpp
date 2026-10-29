#include "Protected286Ports.h"

Protected286Byte
Protected286Ports::inByte(Protected286Word port)
{
    Protected286Byte value;
    asm mov dx, port;
    asm in al, dx;
    asm mov value, al;
    return value;
}

void
Protected286Ports::outByte(Protected286Word port, Protected286Byte value)
{
    asm mov dx, port;
    asm mov al, value;
    asm out dx, al;
}

Protected286Word
Protected286Ports::inWord(Protected286Word port)
{
    Protected286Word value;
    asm mov dx, port;
    asm in ax, dx;
    asm mov value, ax;
    return value;
}

void
Protected286Ports::outWord(Protected286Word port, Protected286Word value)
{
    asm mov dx, port;
    asm mov ax, value;
    asm out dx, ax;
}
