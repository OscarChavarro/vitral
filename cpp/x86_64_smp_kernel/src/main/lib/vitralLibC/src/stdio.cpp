#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdlib.h>
#include "format_engine.h"
#include "drivers/tty/Tty.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "fs/Err.h"
#include "kernel/Scheduler.h"
#include "lib/vitralKernel/Memory.h"
#include "syscalls/Syscalls.h"
#include "lib/vitralKernel/String.h"
#include "fs/OpenFlag.h"

static FILE standardStreams[3] = {{0, 0, 0, 0, -1}, {1, 0, 0, 0, -1}, {2, 0, 0, 0, -1}};

FILE *stdin = &standardStreams[0];
FILE *stdout = &standardStreams[1];
FILE *stderr = &standardStreams[2];

class ConsoleSink : public PrintSink {
public:
    void put(const char *text, size_t count) override {
        unsigned console = Syscalls::console();
        for (size_t i = 0; i < count; i++) {
            VirtualConsole::putc(console, text[i]);
        }
    }
};

class BufferSink : public PrintSink {
public:
    BufferSink(char *buffer, size_t size) : out(buffer), room(size), written(0) {}
    void put(const char *text, size_t count) override {
        for (size_t i = 0; i < count; i++, written++) {
            if (written + 1 < room) {
                out[written] = text[i];
            }
        }
    }
    void finish() {
        if (room) {
            out[written < room ? written : room - 1] = 0;
        }
    }
private:
    char *out;
    size_t room;
    size_t written;
};

static bool
isConsoleStream(FILE *stream) {
    return stream && (stream->descriptor == 1 || stream->descriptor == 2);
}

static bool
isFileStream(FILE *stream) {
    return stream && stream->descriptor >= 3;
}

static constexpr int FLAG_READ = 1;
static constexpr int FLAG_WRITE = 2;

static int
failWith(int64_t result) {
    errno = (int)-result;
    return -1;
}

class FileSink : public PrintSink {
public:
    explicit FileSink(FILE *file) : stream(file) {}
    void put(const char *text, size_t count) override {
        if (count && Syscalls::write(stream->descriptor, text, count) != (int64_t)count) {
            stream->error = 1;
        }
    }
private:
    FILE *stream;
};

int
vfprintf(FILE *stream, const char *format, va_list arguments) {
    if (isFileStream(stream)) {
        FileSink sink(stream);
        return formatTo(sink, format, arguments);
    }
    if (!isConsoleStream(stream)) {
        errno = EBADF;
        return -1;
    }
    ConsoleSink sink;
    return formatTo(sink, format, arguments);
}

int
vprintf(const char *format, va_list arguments) { return vfprintf(stdout, format, arguments); }

int
vsnprintf(char *buffer, size_t size, const char *format, va_list arguments) {
    BufferSink sink(buffer, size);
    int n = formatTo(sink, format, arguments);
    sink.finish();
    return n;
}

int
vsprintf(char *buffer, const char *format, va_list arguments) {
    return vsnprintf(buffer, (size_t)-1 >> 1, format, arguments);
}

int
printf(const char *format, ...) {
    va_list arguments;
    va_start(arguments, format);
    int n = vfprintf(stdout, format, arguments);
    va_end(arguments);
    return n;
}

int
fprintf(FILE *stream, const char *format, ...) {
    va_list arguments;
    va_start(arguments, format);
    int n = vfprintf(stream, format, arguments);
    va_end(arguments);
    return n;
}

int
sprintf(char *buffer, const char *format, ...) {
    va_list arguments;
    va_start(arguments, format);
    int n = vsprintf(buffer, format, arguments);
    va_end(arguments);
    return n;
}

int
snprintf(char *buffer, size_t size, const char *format, ...) {
    va_list arguments;
    va_start(arguments, format);
    int n = vsnprintf(buffer, size, format, arguments);
    va_end(arguments);
    return n;
}

int
vsscanf(const char *text, const char *format, va_list arguments) { return scanText(text, format, arguments); }

int
sscanf(const char *text, const char *format, ...) {
    va_list arguments;
    va_start(arguments, format);
    int n = scanText(text, format, arguments);
    va_end(arguments);
    return n;
}

int
scanf(const char *, ...) { return EOF; }
int
fscanf(FILE *, const char *, ...) { return EOF; }

int
fputc(int c, FILE *stream) {
    if (isFileStream(stream)) {
        char byte = (char)c;
        if (Syscalls::write(stream->descriptor, &byte, 1) != 1) {
            stream->error = 1;
            return EOF;
        }
        return (unsigned char)c;
    }
    if (!isConsoleStream(stream)) {
        errno = EBADF;
        return EOF;
    }
    VirtualConsole::putc(Syscalls::console(), (char)c);
    return (unsigned char)c;
}

int
putc(int c, FILE *stream) { return fputc(c, stream); }
int
putchar(int c) { return fputc(c, stdout); }

int
fputs(const char *text, FILE *stream) {
    if (isFileStream(stream)) {
        return fwrite(text, 1, strlen(text), stream) == strlen(text) ? 0 : EOF;
    }
    if (!isConsoleStream(stream)) {
        errno = EBADF;
        return EOF;
    }
    for (; *text; text++) {
        VirtualConsole::putc(Syscalls::console(), *text);
    }
    return 0;
}

int
puts(const char *text) {
    if (fputs(text, stdout) == EOF) {
        return EOF;
    }
    return fputc('\n', stdout) == EOF ? EOF : 0;
}

size_t
fwrite(const void *buffer, size_t size, size_t count, FILE *stream) {
    if (isFileStream(stream)) {
        if (!size || !count) {
            return 0;
        }
        int64_t n = Syscalls::write(stream->descriptor, buffer, size * count);
        if (n < 0) {
            stream->error = 1;
            errno = (int)-n;
            return 0;
        }
        return (size_t)n / size;
    }
    if (!isConsoleStream(stream)) {
        errno = EBADF;
        return 0;
    }
    const char *bytes = (const char *)buffer;
    unsigned console = Syscalls::console();
    for (size_t i = 0; i < size * count; i++) {
        VirtualConsole::putc(console, bytes[i]);
    }
    return count;
}

int
fgetc(FILE *stream) {
    if (!stream || (stream->descriptor != 0 && !isFileStream(stream))) {
        errno = EBADF;
        return EOF;
    }
    if (stream->pushed >= 0) {
        int c = stream->pushed;
        stream->pushed = -1;
        return c;
    }
    if (isFileStream(stream)) {
        unsigned char byte;
        int64_t n = Syscalls::read(stream->descriptor, &byte, 1);
        if (n == 1) {
            return byte;
        }
        if (n == 0) {
            stream->eof = 1;
        } else {
            stream->error = 1;
        }
        return EOF;
    }
    uint8_t byte;
    if (Tty::read(&byte, 1) == 1) {
        return byte;
    }
    stream->eof = 1;
    return EOF;
}

int
getc(FILE *stream) { return fgetc(stream); }
int
getchar() { return fgetc(stdin); }

int
ungetc(int c, FILE *stream) {
    if (!stream || c == EOF) {
        return EOF;
    }
    stream->pushed = c;
    return c;
}

char *
fgets(char *buffer, int size, FILE *stream) {
    if (size <= 0) {
        return nullptr;
    }
    int n = 0;
    while (n + 1 < size) {
        int c = fgetc(stream);
        if (c == EOF) {
            break;
        }
        buffer[n++] = (char)c;
        if (c == '\n') {
            break;
        }
    }
    if (n == 0) {
        return nullptr;
    }
    buffer[n] = 0;
    return buffer;
}

size_t
fread(void *buffer, size_t size, size_t count, FILE *stream) {
    char *bytes = (char *)buffer;
    size_t total = size * count;
    size_t i = 0;
    if (isFileStream(stream) && total) {
        if (stream->pushed >= 0) {
            bytes[i++] = (char)stream->pushed;
            stream->pushed = -1;
        }
        while (i < total) {
            int64_t n = Syscalls::read(stream->descriptor, bytes + i, total - i);
            if (n < 0) {
                stream->error = 1;
                errno = (int)-n;
                break;
            }
            if (n == 0) {
                stream->eof = 1;
                break;
            }
            i += (size_t)n;
        }
        return i / size;
    }
    for (; i < total; i++) {
        int c = fgetc(stream);
        if (c == EOF) {
            break;
        }
        bytes[i] = (char)c;
    }
    return size ? i / size : 0;
}

static int
modeFlags(const char *mode, int *streamFlags) {
    if (!mode) {
        return -1;
    }
    bool plus = false;
    for (const char *c = mode + 1; *c; c++) {
        if (*c == '+') {
            plus = true;
        }
    }
    int flags;
    switch (mode[0]) {
    case 'r': flags = plus ? OpenFlag::RDWR : OpenFlag::RDONLY; break;
    case 'w': flags = (plus ? OpenFlag::RDWR : OpenFlag::WRONLY) | OpenFlag::CREAT | OpenFlag::TRUNC; break;
    case 'a': flags = (plus ? OpenFlag::RDWR : OpenFlag::WRONLY) | OpenFlag::CREAT | OpenFlag::APPEND; break;
    default: return -1;
    }
    *streamFlags = (mode[0] == 'r' || plus ? FLAG_READ : 0) | (mode[0] != 'r' || plus ? FLAG_WRITE : 0);
    return flags;
}

static FILE *
newStream(int descriptor, int streamFlags) {
    FILE *stream = (FILE *)malloc(sizeof(FILE));
    if (!stream) {
        errno = ENOMEM;
        return nullptr;
    }
    stream->descriptor = descriptor;
    stream->flags = streamFlags;
    stream->error = stream->eof = 0;
    stream->pushed = -1;
    return stream;
}

FILE *
fopen(const char *path, const char *mode) {
    int streamFlags = 0;
    int flags = modeFlags(mode, &streamFlags);
    if (flags < 0 || !path) {
        errno = EINVAL;
        return nullptr;
    }
    if (String::equals(path, "/dev/stdout") || String::equals(path, "/dev/stderr")) {
        return newStream(path[5] == 's' && path[8] == 'o' ? 1 : 2, streamFlags);
    }
    if (!Scheduler::cpuCount()) {
        errno = ENOSYS;
        return nullptr;
    }
    int64_t fd = Syscalls::open(path, flags);
    if (fd < 0) {
        errno = (int)-fd;
        return nullptr;
    }
    FILE *stream = newStream((int)fd, streamFlags);
    if (!stream) {
        Syscalls::close((int)fd);
    }
    return stream;
}

FILE *
fdopen(int descriptor, const char *mode) {
    int streamFlags = 0;
    if (descriptor < 3 || modeFlags(mode, &streamFlags) < 0) {
        errno = EINVAL;
        return nullptr;
    }
    return newStream(descriptor, streamFlags);
}

FILE *
freopen(const char *path, const char *mode, FILE *stream) {
    if (!isFileStream(stream) || !path) {
        errno = EINVAL;
        return nullptr;
    }
    FILE *opened = fopen(path, mode);
    if (!opened) {
        return nullptr;
    }
    Syscalls::close(stream->descriptor);
    *stream = *opened;
    free(opened);
    return stream;
}

FILE *
tmpfile() {
    errno = ENOSYS;
    return nullptr;
}

FILE *
popen(const char *, const char *) {
    errno = ENOSYS;
    return nullptr;
}

int
pclose(FILE *) { return -1; }

int
fclose(FILE *stream) {
    if (isConsoleStream(stream) && (stream < standardStreams || stream >= standardStreams + 3)) {
        free(stream);
        return 0;
    }
    if (!isFileStream(stream)) {
        return EOF;
    }
    int64_t r = Syscalls::close(stream->descriptor);
    free(stream);
    return r < 0 ? failWith(r) : 0;
}

int
fflush(FILE *) { return 0; }

int
fseek(FILE *stream, long offset, int whence) {
    if (!isFileStream(stream)) {
        errno = ESPIPE;
        return -1;
    }
    if (whence == SEEK_CUR && stream->pushed >= 0) {
        offset--;
    }
    int64_t r = Syscalls::lseek(stream->descriptor, offset, whence);
    if (r < 0) {
        return failWith(r);
    }
    stream->pushed = -1;
    stream->eof = 0;
    return 0;
}

long
ftell(FILE *stream) {
    if (!isFileStream(stream)) {
        errno = ESPIPE;
        return -1;
    }
    int64_t r = Syscalls::lseek(stream->descriptor, 0, SEEK_CUR);
    if (r < 0) {
        return failWith(r);
    }
    return (long)r - (stream->pushed >= 0 ? 1 : 0);
}

void
rewind(FILE *stream) {
    if (fseek(stream, 0, SEEK_SET) == 0) {
        stream->error = 0;
    }
}

int
fgetpos(FILE *stream, fpos_t *position) {
    long at = ftell(stream);
    if (at < 0) {
        return -1;
    }
    *position = at;
    return 0;
}

int
fsetpos(FILE *stream, const fpos_t *position) { return fseek(stream, (long)*position, SEEK_SET); }

int
feof(FILE *stream) { return stream ? stream->eof : 1; }
int
ferror(FILE *stream) { return stream ? stream->error : 1; }

void
clearerr(FILE *stream) {
    if (stream) {
        stream->error = stream->eof = 0;
    }
}

int
fileno(FILE *stream) { return stream ? stream->descriptor : -1; }
int
setvbuf(FILE *, char *, int, size_t) { return 0; }
void
setbuf(FILE *, char *) {}

int
remove(const char *path) {
    int64_t r = Syscalls::unlink(path);
    if (r == -Err::ISDIR) {
        r = Syscalls::rmdir(path);
    }
    return r < 0 ? failWith(r) : 0;
}

int
rename(const char *, const char *) {
    errno = ENOSYS;
    return -1;
}

void
perror(const char *prefix) {
    if (prefix && *prefix) {
        fprintf(stderr, "%s: ", prefix);
    }
    fprintf(stderr, "%s\n", strerror(errno));
}
