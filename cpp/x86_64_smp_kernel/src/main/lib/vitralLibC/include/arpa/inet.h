#ifndef __INET__
#define __INET__
#include <netinet/in.h>
in_addr_t
inet_addr(const char *text);
int
inet_pton(int family, const char *text, void *out);
const char *
inet_ntop(int family, const void *address, char *buffer, socklen_t size);
char *
inet_ntoa(in_addr address);

#endif
