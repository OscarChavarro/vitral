#include "kernel/InitScripts.h"
#include "drivers/char/uart/Uart.h"
#include "kernel/KernelLog.h"
#include "net/NetDevice.h"
#include "syscalls/Syscalls.h"
#include "net/NetDevices.h"
#include "kernel/ArgList.h"
#include "fs/OpenFlag.h"

const InitScripts::Step InitScripts::script[] = {
    {Need::Serial, nullptr, Mode::Respawn, "shell", "/dev/ttyS0"},
    {Need::Interface, "eth0", Mode::Run, "ifconfig eth0 192.168.1.199 netmask 255.255.255.0 up", nullptr},
    {Need::Interface, "eth0", Mode::WaitLink, "eth0", nullptr},
    {Need::Nic, nullptr, Mode::Background, "httpd", nullptr},
};
const unsigned InitScripts::scriptLength = sizeof(script) / sizeof(script[0]);

unsigned
InitScripts::run() {
    unsigned failures = 0;
    for (unsigned i = 0; i < scriptLength; i++) {
        const Step &step = script[i];
        if (!satisfied(step)) {
            continue;
        }
        bool ok = step.mode == Mode::WaitLink ? waitForLink(step.line) : command(step);
        if (!ok) {
            failures++;
        }
    }
    return failures;
}

bool
InitScripts::satisfied(const Step &step) {
    switch (step.need) {
    case Need::Nothing: return true;
    case Need::Serial: return Uart::ready();
    case Need::Interface: return NetDevices::find(step.interfaceName) != nullptr;
    case Need::Nic:
        if (hasNic()) {
            return true;
        }
        kprintf("init: %s: no network card (only the loopback), not started\n", step.line);
        return false;
    }
    return false;
}

// A network card is any interface but the loopback, so this is false when ifconfig only shows lo.
bool
InitScripts::hasNic() {
    for (unsigned i = 0; i < NetDevices::count(); i++) {
        if (!NetDevices::at(i)->isLoopback()) {
            return true;
        }
    }
    return false;
}

bool
InitScripts::parse(const char *line, char (&buffer)[ArgList::BUFFER_SIZE], const char *&name, ArgList &args) {
    unsigned length = 0;
    for (const char *c = line; *c && length < sizeof(buffer) - 1; c++) {
        buffer[length++] = *c;
    }
    buffer[length] = '\0';
    name = nullptr;
    bool tooMany = false;
    for (char *c = buffer; *c;) {
        while (*c == ' ') {
            *c++ = '\0';
        }
        if (!*c) {
            break;
        }
        char *word = c;
        while (*c && *c != ' ') {
            c++;
        }
        if (*c) {
            *c++ = '\0';
        }
        if (!name) {
            name = word;
        } else if (!args.add(word)) {
            tooMany = true;
        }
    }
    return name && !tooMany;
}

// Starts the step's command, on its terminal if it names one: its PID, or a negative error.
int64_t
InitScripts::start(const Step &step, bool background) {
    char buffer[ArgList::BUFFER_SIZE];
    const char *name;
    ArgList args;
    if (!parse(step.line, buffer, name, args)) {
        kprintf("init: bad command line\n");
        return -Syscalls::ERR_INVAL;
    }
    int terminal = -1;
    if (step.terminal) {
        int64_t fd = Syscalls::open(step.terminal, OpenFlag::RDWR);
        if (fd < 0) {
            kprintf("init: %s: cannot open %s\n", name, step.terminal);
            return fd;
        }
        terminal = (int)fd;
    }
    int64_t child = background ? Syscalls::spawn(name, args, terminal) : Syscalls::run(name, args, terminal);
    if (terminal >= 0) {
        Syscalls::close(terminal);
    }
    if (child < 0) {
        kprintf("init: %s: %s\n", name, child == -Syscalls::ERR_NOENT ? "command not found" : "cannot start");
    }
    return child;
}

// A thread of init that keeps the step's command running.
void
InitScripts::respawn(void *argument) {
    const Step &step = *(const Step *)argument;
    for (;;) {
        int64_t child = start(step, true);
        while (child >= 0 && Syscalls::consoleOf((uint32_t)child) >= 0) {
            Syscalls::sleep(RESPAWN_CHECK_MS);
        }
        Syscalls::sleep(RESPAWN_PAUSE_MS);
    }
}

bool
InitScripts::command(const Step &step) {
    kprintf("init: %s%s%s%s\n", step.line, step.terminal ? " < " : "", step.terminal ? step.terminal : "",
            step.mode == Mode::Background ? " &" : step.mode == Mode::Respawn ? " (respawn)" : "");
    if (step.mode == Mode::Respawn) {
        if (Syscalls::createThread(respawn, (void *)&step, RESPAWN_STACK_SIZE) < 0) {
            kprintf("init: %s: cannot start its respawn thread\n", step.line);
            return false;
        }
        return true;
    }
    int64_t child = start(step, step.mode == Mode::Background);
    if (child < 0) {
        return false;
    }
    if (step.mode == Mode::Background) {
        return true;
    }
    int status = 0;
    Syscalls::waitpid(child, &status);
    if (Syscalls::exitCode(status) != 0) {
        kprintf("init: %s failed with status %d\n", step.line, Syscalls::exitCode(status));
        return false;
    }
    return true;
}

bool
InitScripts::waitForLink(const char *interfaceName) {
    NetDevice *device = NetDevices::find(interfaceName);
    if (!device) {
        return false;
    }
    uint64_t start = Syscalls::uptimeMs();
    while (!device->hasLink()) {
        if (Syscalls::uptimeMs() - start >= LINK_TIMEOUT_MS) {
                kprintf("init: %s: no link after %u s, going on\n", interfaceName, LINK_TIMEOUT_MS / 1000);
            return false;
        }
        Syscalls::sleep(100);
    }
    kprintf("init: %s: link up after %u ms\n", interfaceName, (uint32_t)(Syscalls::uptimeMs() - start));
    return true;
}
