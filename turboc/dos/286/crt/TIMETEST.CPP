#include "../Protected286Ports.h"
#include <sys/timeb.h>
#include <assert.h>
#include <errno.h>
static unsigned char registers[128], selected;
Protected286Byte Protected286Ports::inByte(Protected286Word port)
{ return port == 0x70 ? selected : registers[selected & 127]; }
void Protected286Ports::outByte(Protected286Word, Protected286Byte value) { selected = value; }
int main()
{
    struct timeb t;
    selected = 0x85; /* Preserve NMI-disable state and selected register. */
    registers[11] = 2;
    registers[0x32] = 0x20;
    registers[9] = 0; registers[8] = 1; registers[7] = 1;
    ftime(&t);
    assert(t.time == 946684800L && !t.millitm && selected == 0x85);
    registers[9] = 0x24; registers[8] = 2; registers[7] = 0x29;
    registers[4] = 0x12; registers[2] = 0x34; registers[0] = 0x56;
    ftime(&t);
    assert(t.time == 1709210096L);
    registers[7] = 0x30;
    errno = 0; ftime(&t); assert(errno == EIO);
    registers[10] = 0x80;
    errno = 0; ftime(&t); assert(errno == EIO);
    return 0;
}
