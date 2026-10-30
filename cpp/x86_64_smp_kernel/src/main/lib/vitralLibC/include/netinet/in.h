#ifndef __IN__
#define __IN__
#include <stdint.h>
#include <sys/socket.h>
#define INADDR_ANY ((in_addr_t)0)
#define INADDR_LOOPBACK ((in_addr_t)0x7f000001)
#define IPPROTO_TCP 6
#define IPPROTO_UDP 17
typedef uint16_t in_port_t;
typedef uint32_t in_addr_t;
struct in_addr {
    in_addr_t s_addr;
};
struct sockaddr_in {
    sa_family_t sin_family;
    in_port_t sin_port;
    in_addr sin_addr;
    unsigned char sin_zero[8];
};
uint16_t
htons(uint16_t value);
uint16_t
ntohs(uint16_t value);
uint32_t
htonl(uint32_t value);
uint32_t
ntohl(uint32_t value);

#endif
