#ifndef __POLL__
#define __POLL__
#define POLLIN 1
#define POLLPRI 2
#define POLLOUT 4
#define POLLERR 8
#define POLLHUP 16
#define POLLNVAL 32
typedef unsigned long nfds_t;
struct pollfd {
    int fd;
    short events;
    short revents;
};
int
poll(pollfd *descriptors, nfds_t count, int timeout);

#endif
