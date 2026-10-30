#include "programs/ifconfig/IfconfigCommand.h"
#include "fs/Err.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/Printf.h"
#include "net/Ipv4Address.h"
#include "net/MacAddress.h"
#include "syscalls/Syscalls.h"
#include "lib/vitralKernel/String.h"
#include "net/NetFlag.h"
#include "net/NetInterfaceInfo.h"
#include "net/NetConfig.h"
#include "kernel/ArgList.h"

bool
IfconfigCommand::address(const char *text, uint8_t out[4]) {
    Ipv4Address parsed;
    if (!Ipv4Address::parse(text, parsed)) {
        return false;
    }
    Memory::copy(out, parsed.data(), 4);
    return true;
}

int
IfconfigCommand::find(const char *name, NetInterfaceInfo &info) {
    for (unsigned i = 0; Syscalls::netInterface(i, &info) == 0; i++) {
        if (String::equals(info.name, name) || String::equals(info.driver, name)) {
            return (int)i;
        }
    }
    return -1;
}

void
IfconfigCommand::showBytes(uint64_t bytes) {
    static const char *const UNITS[] = {"B", "KiB", "MiB", "GiB"};
    unsigned unit = 0;
    uint64_t tenths = bytes * 10;
    while (tenths >= 10240 && unit < 3) {
        tenths /= 1024;
        unit++;
    }
    printf64("%u (%u.%u %s)", (uint32_t)bytes, (uint32_t)(tenths / 10), (uint32_t)(tenths % 10), UNITS[unit]);
}

void
IfconfigCommand::show(const NetInterfaceInfo &info) {
    printf64("%s: flags=%u<", info.name, info.flags);
    const char *separator = "";
    static const class {
    public:
        uint32_t bit;
        const char *name;
    } FLAGS[] = {
        {NetFlag::UP, "UP"}, {NetFlag::BROADCAST, "BROADCAST"}, {NetFlag::LOOPBACK, "LOOPBACK"},
        {NetFlag::RUNNING, "RUNNING"}, {NetFlag::MULTICAST, "MULTICAST"}};
    for (const auto &flag : FLAGS) {
        if (info.flags & flag.bit) {
            printf64("%s%s", separator, flag.name);
            separator = ",";
        }
    }
    printf64(">  mtu %u\n", info.mtu);

    Ipv4Address ip(info.address[0], info.address[1], info.address[2], info.address[3]);
    if (!ip.isZero()) {
        char text[Ipv4Address::TEXT_SIZE];
        ip.format(text);
        printf64("        inet %s", text);
        Ipv4Address(info.netmask[0], info.netmask[1], info.netmask[2], info.netmask[3]).format(text);
        printf64("  netmask %s", text);
        Ipv4Address broadcast(info.broadcast[0], info.broadcast[1], info.broadcast[2], info.broadcast[3]);
        if (!broadcast.isZero()) {
            broadcast.format(text);
            printf64("  broadcast %s", text);
        }
        printf64("\n");
    }
    if (info.flags & NetFlag::LOOPBACK) {
        printf64("        loop  (Local Loopback)\n");
    } else {
        char text[MacAddress::TEXT_SIZE];
        MacAddress::from(info.mac).format(text);
        printf64("        ether %s  (Ethernet)\n", text);
    }
    printf64("        RX packets %u  bytes ", (uint32_t)info.rxPackets);
    showBytes(info.rxBytes);
    printf64("\n        RX errors %u  dropped %u\n", (uint32_t)info.rxErrors, (uint32_t)info.rxDropped);
    printf64("        TX packets %u  bytes ", (uint32_t)info.txPackets);
    showBytes(info.txBytes);
    printf64("\n        TX errors %u  dropped %u\n\n", (uint32_t)info.txErrors, (uint32_t)info.txDropped);
}

int
IfconfigCommand::main(const ArgList &args) {
    NetInterfaceInfo info;
    unsigned count = args.count();

    if (count == 0 || String::equals(args.at(0), "-a")) {
        bool all = count > 0;
        for (unsigned i = 0; Syscalls::netInterface(i, &info) == 0; i++) {
            if (all || (info.flags & NetFlag::UP)) {
                show(info);
            }
        }
        return 0;
    }

    int index = find(args.at(0), info);
    if (index < 0) {
        printf64("%s: error fetching interface information: Device not found\n", args.at(0));
        return 1;
    }
    if (count == 1) {
        show(info);
        return 0;
    }

    NetConfig config = {};
    config.index = (uint32_t)index;
    for (unsigned i = 1; i < count; i++) {
        const char *word = args.at(i);
        if (String::equals(word, "up")) {
            config.actions |= NetConfig::BRING_UP;
        } else if (String::equals(word, "down")) {
            config.actions |= NetConfig::BRING_DOWN;
        } else if (String::equals(word, "inet")) {
            continue;
        } else if (String::equals(word, "netmask") || String::equals(word, "broadcast")) {
            bool netmask = word[0] == 'n';
            const char *value = i + 1 < count ? args.at(i + 1) : nullptr;
            if (!value) {
                printf64("ifconfig: `%s' requires an argument\n", word);
                return 1;
            }
            if (!address(value, netmask ? config.netmask : config.broadcast)) {
                printf64("ifconfig: bad address `%s'\n", value);
                return 1;
            }
            config.actions |= netmask ? NetConfig::SET_NETMASK : NetConfig::SET_BROADCAST;
            i++;
        } else if (!(config.actions & NetConfig::SET_ADDRESS) && word[0] >= '0' && word[0] <= '9') {
            if (!address(word, config.address)) {
                printf64("ifconfig: bad address `%s'\n", word);
                return 1;
            }
            config.actions |= NetConfig::SET_ADDRESS;
        } else {
            printf64("ifconfig: unsupported argument `%s'\n", word);
            return 1;
        }
    }

    int64_t status = Syscalls::netConfigure(&config);
    if (status < 0) {
        printf64("ifconfig: cannot configure %s: %s\n", info.name,
                 status == -Err::INVAL ? "invalid argument" : status == -Err::NODEV ? "no such device" : "failed");
        return 1;
    }
    return 0;
}
