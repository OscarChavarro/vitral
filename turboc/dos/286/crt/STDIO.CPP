/* Borland FILE ABI, backed exclusively by the protected FAT/VGA drivers. */
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <stdarg.h>
#include "FORMAT.H"
#include "../Protected286Crt.h"
#include "../Protected286VgaText.h"

extern "C" {
FILE _streams[9];
unsigned _nfile = 9;
int errno = 0;
}

/* FileOutputStream writes pixels through fputc. Keep those byte writes in
 * the protected CRT, instead of issuing one ATA read/write per byte. */
static unsigned char outputBuffers[4][512];
static unsigned outputBytes[4];

static int flushOutput(FILE* stream)
{
    unsigned slot = (unsigned)(stream - &_streams[5]);
    if (slot >= 4 || !outputBytes[slot]) return 0;
    long written = Protected286Crt::write(stream->fd, outputBuffers[slot],
                                        outputBytes[slot]);
    if (written < 0) written = 0;
    unsigned remaining = outputBytes[slot] - (unsigned)written;
    if (remaining) {
        memmove(outputBuffers[slot], outputBuffers[slot] + (unsigned)written,
                remaining);
        stream->flags |= _F_ERR;
        errno = EIO;
    }
    outputBytes[slot] = remaining;
    return remaining ? EOF : 0;
}

extern "C" void crt286InitializeStreams()
{
    unsigned i;
    for (i = 0; i < 9; ++i) {
        _streams[i].flags = 0;
        _streams[i].fd = -1;
        _streams[i].level = 0;
        _streams[i].buffer = _streams[i].curp = 0;
    }
    for (i = 0; i < 4; ++i) outputBytes[i] = 0;
    stdin->flags = _F_READ;
    stdout->flags = stderr->flags = _F_WRIT;
}

extern "C" FILE* fopen(const char* path, const char* mode)
{
    unsigned i;
    int handle;
    const char* basename;
    if (!path || !mode) { errno = EINVAL; return 0; }
    if (!strcmp(path, "/dev/stdout")) return stdout;
    if (!strcmp(path, "/dev/stderr")) return stderr;
    if (!strcmp(path, "/dev/stdin")) return stdin;
    /* Root-only FAT contract: resolve the portable scene path to its 8.3
     * filename. No directories are mounted in this runtime yet. */
    basename = path;
    while (*path) { if (*path == '/' || *path == '\\') basename = path + 1; ++path; }
    if (mode[0] != 'r' && mode[0] != 'w') { errno = EINVAL; return 0; }
    for (i = 5; i < 9 && _streams[i].flags; ++i) {}
    if (i == 9) { errno = EMFILE; return 0; }
    handle = mode[0] == 'r' ? Protected286Crt::openRead(basename) :
                            Protected286Crt::openWrite(basename);
    if (handle < 0) { errno = ENOENT; return 0; }
    _streams[i].fd = (char)handle;
    _streams[i].flags = mode[0] == 'r' ? _F_READ : _F_WRIT;
    outputBytes[i - 5] = 0;
    return &_streams[i];
}

extern "C" int fclose(FILE* stream)
{
    if (!stream) return EOF;
    if (stream == stdin || stream == stdout || stream == stderr) return 0;
    if (!stream->flags) return EOF;
    int result = fflush(stream);
    Protected286Crt::close(stream->fd);
    stream->flags = 0;
    return result;
}
extern "C" int fflush(FILE* stream)
{
    if (!stream) {
        int result = 0;
        for (unsigned i = 5; i < 9; ++i)
            if (_streams[i].flags && fflush(&_streams[i]) == EOF) result = EOF;
        return result;
    }
    if (stream != stdout && stream != stderr && (stream->flags & _F_WRIT))
        if (flushOutput(stream) == EOF) return EOF;
    return stream && (stream->flags & _F_ERR) ? EOF : 0;
}
extern "C" size_t fread(void* data, size_t size, size_t count, FILE* stream)
{
    long bytes;
    if (!size || !count) return 0;
    if (!stream || !(stream->flags & _F_READ) || stream == stdin) return 0;
    if ((unsigned long)size * count > 65535UL) { errno = EINVAL; return 0; }
    bytes = Protected286Crt::read(stream->fd, data, (long)size * count);
    if (bytes < 0) { stream->flags |= _F_ERR; return 0; }
    if ((unsigned long)bytes < (unsigned long)size * count) stream->flags |= _F_EOF;
    return (size_t)(bytes / size);
}
extern "C" size_t fwrite(const void* data, size_t size, size_t count, FILE* stream)
{
    unsigned long i, total = (unsigned long)size * count;
    if (!size || !count) return 0;
    if (!stream || !(stream->flags & _F_WRIT) || total > 65535UL) return 0;
    if (stream == stdout || stream == stderr) {
        for (i = 0; i < total; ++i)
            Protected286VgaText::putCharacter(((const char*)data)[(unsigned)i]);
        return count;
    }
    unsigned slot = (unsigned)(stream - &_streams[5]);
    if (slot >= 4 || (stream->flags & _F_ERR)) return 0;
    unsigned copied = 0;
    while (copied < (unsigned)total) {
        unsigned take = 512 - outputBytes[slot];
        if (take > (unsigned)total - copied) take = (unsigned)total - copied;
        memcpy(outputBuffers[slot] + outputBytes[slot],
               (const unsigned char*)data + copied, take);
        outputBytes[slot] += take;
        copied += take;
        if (outputBytes[slot] == 512 && flushOutput(stream) == EOF) break;
    }
    return (size_t)(copied / size);
}
extern "C" int fgetc(FILE* stream)
{
    unsigned char value;
    if (stream == stdin) return Protected286Crt::getCharacter();
    return fread(&value, 1, 1, stream) == 1 ? value : EOF;
}
extern "C" int fputc(int c, FILE* stream)
{
    unsigned char value = (unsigned char)c;
    return fwrite(&value, 1, 1, stream) == 1 ? value : EOF;
}
extern "C" char* fgets(char* buffer, int count, FILE* stream)
{
    int i = 0, c;
    if (!buffer || count <= 0) return 0;
    while (i < count - 1) {
        c = fgetc(stream);
        if (c == EOF) break;
        buffer[i++] = (char)c;
        if (c == '\n') break;
    }
    buffer[i] = 0;
    return i || count == 1 ? buffer : 0;
}
extern "C" int fputs(const char* text, FILE* stream)
{
    size_t length = strlen(text);
    return fwrite(text, 1, length, stream) == length ? 0 : EOF;
}
extern "C" int fseek(FILE* stream, long offset, int origin)
{
    if (!stream || stream == stdin || stream == stdout || stream == stderr ||
        fflush(stream) == EOF ||
        Protected286Crt::seek(stream->fd, offset, origin) < 0) return -1;
    stream->flags &= ~_F_EOF;
    return 0;
}
extern "C" long ftell(FILE* stream)
{
    if (!stream || stream == stdin || stream == stdout || stream == stderr) return -1;
    if (fflush(stream) == EOF) return -1;
    return Protected286Crt::seek(stream->fd, 0, SEEK_CUR);
}

static int streamOutput(char c, void* context)
{
    return fputc((unsigned char)c, (FILE*)context) != EOF;
}
extern "C" int vfprintf(FILE* stream, const char* format, va_list args)
{
    return crt286VFormat(streamOutput, stream, format, args);
}
extern "C" int fprintf(FILE* stream, const char* format, ...)
{
    va_list args;
    va_start(args, format);
    int result = vfprintf(stream, format, args);
    va_end(args);
    return result;
}
extern "C" int printf(const char* format, ...)
{
    va_list args;
    va_start(args, format);
    int result = vfprintf(stdout, format, args);
    va_end(args);
    return result;
}
extern "C" int vsprintf(char* buffer, const char* format, va_list args)
{
    return crt286VBuffer(buffer, (size_t)-1, format, args);
}
extern "C" int sprintf(char* buffer, const char* format, ...)
{
    va_list args;
    va_start(args, format);
    int result = vsprintf(buffer, format, args);
    va_end(args);
    return result;
}
