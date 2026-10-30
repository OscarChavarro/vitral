#ifndef __VITRAL_LIBC_SYS_SOCKET__
#define __VITRAL_LIBC_SYS_SOCKET__
#include <sys/types.h>
#include <sys/select.h>
#define AF_UNSPEC 0
#define AF_INET 2
#define AF_INET6 10
#define PF_INET AF_INET
#define SOCK_STREAM 1
#define SOCK_DGRAM 2
#define SOL_SOCKET 1
#define SO_REUSEADDR 2
#define SO_KEEPALIVE 9
#define SO_RCVTIMEO 20
#define SO_SNDTIMEO 21
#define SHUT_RD 0
#define SHUT_WR 1
#define SHUT_RDWR 2
#define SOMAXCONN 128
#define MSG_NOSIGNAL 0x4000
typedef unsigned int socklen_t;
typedef unsigned short sa_family_t;
struct sockaddr {
    sa_family_t sa_family;
    char sa_data[14];
};
int
socket(int domain, int type, int protocol);
int
bind(int descriptor, const sockaddr *address, socklen_t length);
int
listen(int descriptor, int backlog);
int
accept(int descriptor, sockaddr *address, socklen_t *length);
int
connect(int descriptor, const sockaddr *address, socklen_t length);
ssize_t
send(int descriptor, const void *buffer, size_t count, int flags);
ssize_t
recv(int descriptor, void *buffer, size_t count, int flags);
int
shutdown(int descriptor, int how);
int
setsockopt(int descriptor, int level, int name, const void *value, socklen_t length);
int
getsockopt(int descriptor, int level, int name, void *value, socklen_t *length);

#endif
