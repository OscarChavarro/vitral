#ifndef __NETDB__
#define __NETDB__
#include <netinet/in.h>
#define AI_PASSIVE 1
#define NI_MAXHOST 1025
struct addrinfo {
    int ai_flags;
    int ai_family;
    int ai_socktype;
    int ai_protocol;
    socklen_t ai_addrlen;
    sockaddr *ai_addr;
    char *ai_canonname;
    addrinfo *ai_next;
};
struct hostent {
    char *h_name;
    char **h_aliases;
    int h_addrtype;
    int h_length;
    char **h_addr_list;
};
#define h_addr h_addr_list[0]
int
getaddrinfo(const char *node, const char *service, const addrinfo *hints, addrinfo **result);
void
freeaddrinfo(addrinfo *result);
const char *
gai_strerror(int code);
hostent *
gethostbyname(const char *name);

#endif
