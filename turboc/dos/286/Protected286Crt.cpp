#include "Protected286Crt.h"

long Protected286Crt::seek(int handle, long offset, int origin)
{
    return fileSystem == 0 ? -1L : fileSystem->seek(handle, offset, origin);
}
#include "Protected286Keyboard.h"
#include "Protected286VgaText.h"

Protected286FileSystem* Protected286Crt::fileSystem = 0;

int Protected286Crt::nextRootEntry(Protected286Word* cursor, char far* name)
{
    return fileSystem ? fileSystem->nextRootEntry(cursor, name) : -1;
}

void
Protected286Crt::initialize()
{
    fileSystem = 0;
}

void
Protected286Crt::write(const char* text)
{
    Protected286VgaText::write(text);
}

void
Protected286Crt::writeLine(const char* text)
{
    write(text);
    write("\r\n");
}

void
Protected286Crt::writeUnsigned(Protected286Dword value)
{
    char text[11];
    Protected286Word length = 0;
    do {
        text[length++] = (char)('0' + (value % 10));
        value /= 10;
    } while (value != 0 && length < sizeof(text));
    while (length != 0) {
        Protected286VgaText::putCharacter(text[--length]);
    }
}

int
Protected286Crt::getCharacter()
{
    return Protected286Keyboard::waitCharacter();
}

void
Protected286Crt::installFileSystem(Protected286FileSystem* backend)
{
    fileSystem = backend;
}

int
Protected286Crt::openRead(const char* path)
{
    return fileSystem == 0 ? -1 : fileSystem->open(path, 0);
}

int
Protected286Crt::openWrite(const char* path)
{
    return fileSystem == 0 ? -1 : fileSystem->open(path, 1);
}

long
Protected286Crt::read(int handle, void far* destination, long count)
{
    return fileSystem == 0 ? -1L : fileSystem->read(handle, destination, count);
}

long
Protected286Crt::write(int handle, const void far* source, long count)
{
    return fileSystem == 0 ? -1L : fileSystem->write(handle, source, count);
}

void
Protected286Crt::close(int handle)
{
    if (fileSystem != 0) {
        fileSystem->close(handle);
    }
}
