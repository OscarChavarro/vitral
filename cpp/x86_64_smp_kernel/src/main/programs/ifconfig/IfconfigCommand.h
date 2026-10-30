#ifndef __IFCONFIG_COMMAND__
#define __IFCONFIG_COMMAND__
#include "net/NetInterfaceInfo.h"
#include "kernel/ArgList.h"

class IfconfigCommand {
public:
    static int main(const ArgList &args);

private:
    static void show(const NetInterfaceInfo &info);
    static void showBytes(uint64_t bytes);
    static int find(const char *name, NetInterfaceInfo &info);
    static bool address(const char *text, uint8_t out[4]);
};

#endif
