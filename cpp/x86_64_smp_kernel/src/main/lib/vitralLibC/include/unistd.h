#ifndef __UNISTD__
#define __UNISTD__
#include <stddef.h>
#include <stdint.h>
#include <sys/types.h>
#define STDIN_FILENO 0
#define STDOUT_FILENO 1
#define STDERR_FILENO 2
#define F_OK 0
#define X_OK 1
#define W_OK 2
#define R_OK 4
ssize_t
read(int descriptor, void *buffer, size_t count);
ssize_t
write(int descriptor, const void *buffer, size_t count);
int
close(int descriptor);
off_t
lseek(int descriptor, off_t offset, int whence);
int
unlink(const char *path);
int
access(const char *path, int mode);
int
isatty(int descriptor);
int
pipe(int descriptors[2]);
int
dup(int descriptor);
int
dup2(int from, int to);
unsigned int
sleep(unsigned int seconds);
int
usleep(useconds_t microseconds);
pid_t
getpid();
pid_t
getppid();
pid_t
fork();
char *
getcwd(char *buffer, size_t size);
int
chdir(const char *path);
long
sysconf(int name);
#define _SC_NPROCESSORS_ONLN 84
#define _SC_NPROCESSORS_CONF 83
#define _SC_PAGESIZE 30

#endif
