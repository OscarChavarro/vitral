#ifndef __ETHERNET_HEADER__
#define __ETHERNET_HEADER__
#include "net/Be16.h"
#include "net/MacAddress.h"

class EthernetHeader {
public:
    MacAddress destination;
    MacAddress source;
    Be16 type;
} __attribute__((packed));

#endif
