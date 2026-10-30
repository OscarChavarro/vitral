#include "programs/httpd/Feeds.h"
#include "kernel/Clock.h"
#include "kernel/Idle.h"
#include "kernel/KernelLog.h"
#include "kernel/Smp.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "programs/httpd/MessageWriter.h"

class Feeds::SystemFeed {
public:
    SystemFeed() : list(new Syscalls::ProcessInfo[MAX_ROWS]), previous(new Syscalls::ProcessInfo[MAX_ROWS]),
                   announced(new uint32_t[MAX_ROWS]), count(0), previousCount(0), announcedCount(0), previousTicks(0) {}
    ~SystemFeed() {
        delete[] list;
        delete[] previous;
        delete[] announced;
    }

    bool hello(WebSocket &socket) {
        MessageWriter m;
        m.reset(SYSTEM);
        m.put((uint8_t)Smp::onlineCount());
        m.put((uint8_t)Smp::logicalCount());
        m.put((uint8_t)Smp::coreCount());
        m.put((uint8_t)Smp::threadsPerCore());
        const char *name = Idle::name();
        size_t n = String::length(name);
        if (n > 40) {
            n = 40;
        }
        m.put((uint8_t)n);
        for (size_t i = 0; i < n; i++) {
            m.put((uint8_t)name[i]);
        }
        return m.send(socket);
    }

    bool update(WebSocket &socket) {
        uint64_t now = Clock::ticks();
        count = Syscalls::listProcesses(list, MAX_ROWS);
        uint64_t elapsed = now - previousTicks;
        if (elapsed == 0) {
            elapsed = 1;
        }

        if (!sendNewProcesses(socket)) {
            return false;
        }

        MessageWriter m;
        uint32_t uptime = (uint32_t)Syscalls::uptime();
        bool first = true;
        unsigned i = 0;
        while (first || i < count) {
            m.reset(PROCESS_STATE);
            size_t flagsAt = m.size();
            m.put(first ? 1 : 0);
            m.put32(uptime);
            first = false;
            while (i < count && m.fits(MessageWriter::varSize(list[i].pid) + 3)) {
                const Syscalls::ProcessInfo &p = list[i];
                unsigned percent = 0;
                for (unsigned j = 0; j < previousCount; j++) {
                    if (previous[j].pid == p.pid) {
                        if (p.ticks >= previous[j].ticks) {
                            percent = (unsigned)((p.ticks - previous[j].ticks) * 100 / elapsed);
                        }
                        break;
                    }
                }
                m.putVar(p.pid);
                m.put(p.state);
                m.put(p.cpu < 0 ? 255 : (uint8_t)p.cpu);
                m.put(percent > 255 ? 255 : (uint8_t)percent);
                i++;
            }
            if (i >= count) {
                m.data()[flagsAt] |= 2;
            }
            if (!m.send(socket)) {
                return false;
            }
        }
        Memory::copy(previous, list, count * sizeof(Syscalls::ProcessInfo));
        previousCount = count;
        previousTicks = now;
        return true;
    }

    bool sendNewProcesses(WebSocket &socket) {
        MessageWriter m;
        m.reset(PROCESS_INFO);
        bool any = false;
        for (unsigned i = 0; i < count; i++) {
            const Syscalls::ProcessInfo &p = list[i];
            bool known = false;
            for (unsigned j = 0; j < announcedCount && !known; j++) {
                known = announced[j] == p.pid;
            }
            if (known) {
                continue;
            }
            size_t nameLength = String::length(p.name);
            if (!m.fits(MessageWriter::varSize(p.pid) + MessageWriter::varSize(p.ppid) + 1 +
                        MessageWriter::varSize((uint32_t)p.affinity) + 1 + nameLength)) {
                if (!m.send(socket)) {
                    return false;
                }
                m.reset(PROCESS_INFO);
            }
            m.putVar(p.pid);
            m.putVar(p.ppid);
            m.put(p.console);
            m.putVar((uint32_t)p.affinity);
            m.put((uint8_t)nameLength);
            for (size_t k = 0; k < nameLength; k++) {
                m.put((uint8_t)p.name[k]);
            }
            any = true;
            if (announcedCount < MAX_ROWS) {
                announced[announcedCount++] = p.pid;
            }
        }
        unsigned kept = 0;
        for (unsigned j = 0; j < announcedCount; j++) {
            for (unsigned i = 0; i < count; i++) {
                if (list[i].pid == announced[j]) {
                    announced[kept++] = announced[j];
                    break;
                }
            }
        }
        announcedCount = kept;
        return any ? m.send(socket) : true;
    }

private:
    static constexpr unsigned MAX_ROWS = 256;
    Syscalls::ProcessInfo *list;
    Syscalls::ProcessInfo *previous;
    uint32_t *announced;
    unsigned count;
    unsigned previousCount;
    unsigned announcedCount;
    uint64_t previousTicks;
};

class Feeds::CpuFeed {
public:
    CpuFeed() : previous(new Syscalls::CpuLoadInfo[MAX_CPUS]) { Memory::set(previous, 0, MAX_CPUS * sizeof(Syscalls::CpuLoadInfo)); }
    ~CpuFeed() { delete[] previous; }

    bool update(WebSocket &socket) {
        Syscalls::CpuLoadInfo now[MAX_CPUS];
        unsigned count = Syscalls::cpuLoad(now, MAX_CPUS);
        if (count > MAX_CPUS) {
            count = MAX_CPUS;
        }
        MessageWriter m;
        m.reset(CPU_LOAD);
        m.put32((uint32_t)Syscalls::uptime());
        m.put((uint8_t)count);
        for (unsigned i = 0; i < count; i++) {
            uint64_t busy = now[i].busyCycles - previous[i].busyCycles;
            uint64_t idle = now[i].idleCycles - previous[i].idleCycles;
            uint64_t total = busy + idle;
            m.put(!now[i].online ? 255 : total == 0 ? 0 : (uint8_t)((busy * 100 + total / 2) / total));
            previous[i] = now[i];
        }
        return m.send(socket);
    }

private:
    static constexpr unsigned MAX_CPUS = 64;
    Syscalls::CpuLoadInfo *previous;
};

class Feeds::LogFeed {
public:
    LogFeed() : position(0) {}

    bool update(WebSocket &socket) {
        for (;;) {
            MessageWriter m;
            m.reset(LOG);
            m.put32((uint32_t)position);
            size_t room = WebSocket::SEND_MAX - m.size();
            size_t n = KernelLog::read(position, (char *)m.data() + m.size(), room);
            if (n == 0) {
                return true;
            }
            uint32_t first = (uint32_t)(position - n);
            for (int i = 0; i < 4; i++) {
                m.data()[1 + i] = (uint8_t)(first >> (8 * i));
            }
            if (!socket.sendBinary(m.data(), 5 + n)) {
                return false;
            }
        }
    }

private:
    uint64_t position;
};

bool
Feeds::knowsTopic(const char *topic) {
    return String::equals(topic, "system") || String::equals(topic, "log") || String::equals(topic, "cpu");
}

void
Feeds::serve(WebSocket &socket, const char *topic) {
    bool isLog = String::equals(topic, "log");
    bool isCpu = String::equals(topic, "cpu");
    SystemFeed system;
    LogFeed log;
    CpuFeed cpu;
    uint32_t interval = isLog ? LOG_INTERVAL_MS : DEFAULT_INTERVAL_MS;
    if (!isLog && !isCpu && !system.hello(socket)) {
        return;
    }
    uint64_t lastPing = Syscalls::uptimeMs();
    uint64_t next = Syscalls::uptimeMs();
    for (;;) {
        uint64_t now = Syscalls::uptimeMs();
        if (now >= next) {
            if (!(isLog ? log.update(socket) : isCpu ? cpu.update(socket)
                                                     : system.update(socket))) {
                return;
            }
            next = now + interval;
            if (isLog && now - lastPing >= PING_INTERVAL_MS) {
                if (!socket.sendPing()) {
                    return;
                }
                lastPing = now;
            }
            now = Syscalls::uptimeMs();
        }
        uint8_t message[WebSocket::PAYLOAD_MAX];
        size_t length = 0;
        WebSocket::Event event = socket.poll(next > now ? (uint32_t)(next - now) : 1, message, sizeof(message), length);
        if (event == WebSocket::Event::Closed) {
            return;
        }
        if (event == WebSocket::Event::Message && !isLog && length == 3 && message[0] == 0x01) {
            uint32_t ms = message[1] | (uint32_t)message[2] << 8;
            interval = ms < 100 ? 100 : ms > 5000 ? 5000 : ms;
            next = Syscalls::uptimeMs();
        }
    }
}
