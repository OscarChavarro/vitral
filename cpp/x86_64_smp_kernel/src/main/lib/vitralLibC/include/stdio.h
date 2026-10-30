#ifndef __STDIO__
#define __STDIO__
#include <stddef.h>
#include <stdarg.h>
#include <sys/types.h>
#define EOF (-1)
#define BUFSIZ 1024
#define SEEK_SET 0
#define SEEK_CUR 1
#define SEEK_END 2
#define _IOFBF 0
#define _IOLBF 1
#define _IONBF 2
#define FILENAME_MAX 255
#define FOPEN_MAX 16
#define L_tmpnam 20
#define TMP_MAX 238328
struct FILE {
    int descriptor;
    int flags;
    int error;
    int eof;
    int pushed;
};
typedef long fpos_t;
extern FILE *stdin;
extern FILE *stdout;
extern FILE *stderr;
int
printf(const char *format, ...);
int
fprintf(FILE *stream, const char *format, ...);
int
sprintf(char *buffer, const char *format, ...);
int
snprintf(char *buffer, size_t size, const char *format, ...);
int
vprintf(const char *format, va_list arguments);
int
vfprintf(FILE *stream, const char *format, va_list arguments);
int
vsprintf(char *buffer, const char *format, va_list arguments);
int
vsnprintf(char *buffer, size_t size, const char *format, va_list arguments);
int
scanf(const char *format, ...);
int
fscanf(FILE *stream, const char *format, ...);
int
sscanf(const char *text, const char *format, ...);
int
vsscanf(const char *text, const char *format, va_list arguments);
int
puts(const char *text);
int
putchar(int c);
int
fputs(const char *text, FILE *stream);
int
fputc(int c, FILE *stream);
int
putc(int c, FILE *stream);
int
getchar();
int
fgetc(FILE *stream);
int
getc(FILE *stream);
int
ungetc(int c, FILE *stream);
char *
fgets(char *buffer, int size, FILE *stream);
FILE *
fopen(const char *path, const char *mode);
FILE *
fdopen(int descriptor, const char *mode);
FILE *
freopen(const char *path, const char *mode, FILE *stream);
FILE *
tmpfile();
int
fclose(FILE *stream);
int
fflush(FILE *stream);
size_t
fread(void *buffer, size_t size, size_t count, FILE *stream);
size_t
fwrite(const void *buffer, size_t size, size_t count, FILE *stream);
int
fseek(FILE *stream, long offset, int whence);
long
ftell(FILE *stream);
void
rewind(FILE *stream);
int
fgetpos(FILE *stream, fpos_t *position);
int
fsetpos(FILE *stream, const fpos_t *position);
int
feof(FILE *stream);
int
ferror(FILE *stream);
void
clearerr(FILE *stream);
int
fileno(FILE *stream);
int
setvbuf(FILE *stream, char *buffer, int mode, size_t size);
void
setbuf(FILE *stream, char *buffer);
int
remove(const char *path);
int
rename(const char *from, const char *to);
void
perror(const char *prefix);
FILE *
popen(const char *command, const char *mode);
int
pclose(FILE *stream);

#endif
