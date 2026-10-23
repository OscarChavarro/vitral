#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/BufferedReader.h"
#include "java/lang/StringBuilder.h"


BufferedReader::BufferedReader(InputStream *in) :
    in(in), position(0), count(0), skipLF(FALSE)
{
}

int
BufferedReader::readByte()
{
    if ( position >= count ) {
        count = in->read(buffer, 0, ((int)(sizeof(buffer))));
        position = 0;
        if ( count <= 0 ) {
            count = 0;
            return -1;
        }
    }
    return buffer[position++];
}

BOOLEAN
BufferedReader::readLine(String &outLine)
{
    StringBuilder line;
    int c = readByte();

    if ( skipLF && c == '\n' ) {
        c = readByte();
    }
    skipLF = FALSE;
    if ( c < 0 ) {
        return FALSE;
    }
    while ( c >= 0 && c != '\n' && c != '\r' ) {
        line.append((char)c);
        c = readByte();
    }
    // As Java, the "\n" of a "\r\n" is skipped in the next read, so this
    // one does not block waiting for it
    skipLF = c == '\r';
    outLine = line.toString();
    return TRUE;
}

