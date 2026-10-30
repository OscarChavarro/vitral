#include "syscalls/Syscalls.h"
#include "fs/Err.h"
#include "fs/Vfs.h"
#include "kernel/Clock.h"
#include "kernel/Consoles.h"
#include "kernel/Interrupts.h"
#include "kernel/Panic.h"
#include "kernel/Scheduler.h"
#include "kernel/WallClock.h"
#include "net/Net.h"
#include "net/Tcp.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "fs/PollEvent.h"
#include "fs/PollRecord.h"
#include "fs/FileStat.h"
#include "fs/MountInfo.h"
#include "net/NetInterfaceInfo.h"
#include "net/NetConfig.h"
#include "fs/OpenFile.h"
#include "kernel/ArgList.h"
#include "kernel/ProgramRegistry.h"

void
Syscalls::sleep(uint32_t milliseconds) {
    TimeSpec ts = {milliseconds / 1000, (int64_t)(milliseconds % 1000) * 1000000};
    call(SYS_NANOSLEEP, (uint64_t)(uintptr_t)&ts);
}

void
Syscalls::exit(int code) {
    call(SYS_EXIT, (uint64_t)(int64_t)code);
    for (;;) {
        asm volatile("hlt");
    }
}

int64_t
Syscalls::waitpid(int64_t pid, int *status, int options) {
    return call(SYS_WAIT4, (uint64_t)pid, (uint64_t)(uintptr_t)status, (uint64_t)(int64_t)options);
}

int64_t
Syscalls::exec(const char *name, const ArgList &args) {
    return call(SYS_EXECVE, (uint64_t)(uintptr_t)name, (uint64_t)(uintptr_t)&args);
}

int64_t
Syscalls::spawn(const char *name, const ArgList &args) {
    return call(SYS_SPAWN, (uint64_t)(uintptr_t)name, (uint64_t)(uintptr_t)&args);
}

int64_t
Syscalls::run(const char *name, const ArgList &args) {
    return call(SYS_RUN, (uint64_t)(uintptr_t)name, (uint64_t)(uintptr_t)&args);
}

int64_t
Syscalls::createThread(void (*entry)(void *), void *argument, size_t stackSize) {
    return call(SYS_THREAD_CREATE, (uint64_t)(uintptr_t)entry, (uint64_t)(uintptr_t)argument, stackSize);
}

int
Syscalls::futexWait(int *address, int expected, uint32_t timeoutMs) {
    return (int)call(SYS_FUTEX_WAIT, (uint64_t)(uintptr_t)address, (uint64_t)(int64_t)expected, timeoutMs);
}

int
Syscalls::futexWake(int *address, int count) {
    return (int)call(SYS_FUTEX_WAKE, (uint64_t)(uintptr_t)address, (uint64_t)(int64_t)count);
}

unsigned
Syscalls::listProcesses(ProcessInfo *out, unsigned max) {
    return (unsigned)call(SYS_PROCESS_LIST, (uint64_t)(uintptr_t)out, max);
}

unsigned
Syscalls::cpuLoad(CpuLoadInfo *out, unsigned max) { return (unsigned)call(SYS_CPU_LOAD, (uint64_t)(uintptr_t)out, max); }

#define ARG(p) ((uint64_t)(uintptr_t)(p))

int64_t
Syscalls::clockGetTime(int clock, TimeSpec *out) { return call(SYS_CLOCK_GETTIME, (uint64_t)(int64_t)clock, ARG(out)); }

int64_t
Syscalls::open(const char *path, int flags) { return call(SYS_OPEN, ARG(path), (uint64_t)(int64_t)flags); }
int64_t
Syscalls::close(int fd) { return call(SYS_CLOSE, (uint64_t)(int64_t)fd); }
int64_t
Syscalls::read(int fd, void *buffer, size_t count) { return call(SYS_READ, (uint64_t)(int64_t)fd, ARG(buffer), count); }
int64_t
Syscalls::write(int fd, const void *buffer, size_t count) { return call(SYS_WRITE, (uint64_t)(int64_t)fd, ARG(buffer), count); }
int64_t
Syscalls::pread(int fd, void *buffer, size_t count, uint64_t offset) {
    return call(SYS_PREAD, (uint64_t)(int64_t)fd, ARG(buffer), count, offset);
}
int64_t
Syscalls::pwrite(int fd, const void *buffer, size_t count, uint64_t offset) {
    return call(SYS_PWRITE, (uint64_t)(int64_t)fd, ARG(buffer), count, offset);
}
int64_t
Syscalls::lseek(int fd, int64_t offset, int whence) {
    return call(SYS_LSEEK, (uint64_t)(int64_t)fd, (uint64_t)offset, (uint64_t)(int64_t)whence);
}
int64_t
Syscalls::ftruncate(int fd, uint64_t size) { return call(SYS_FTRUNCATE, (uint64_t)(int64_t)fd, size); }
int64_t
Syscalls::fsync(int fd) { return call(SYS_FSYNC, (uint64_t)(int64_t)fd); }
int64_t
Syscalls::ioctl(int fd, uint64_t request, void *argument) { return call(SYS_IOCTL, (uint64_t)(int64_t)fd, request, ARG(argument)); }
int64_t
Syscalls::poll(PollRecord *records, unsigned count, int timeoutMs) {
    uint64_t start = uptimeMs();
    for (;;) {
        int64_t ready = call(SYS_POLL, ARG(records), count, 0);
        if (ready != 0 || timeoutMs == 0) {
            return ready;
        }
        if (timeoutMs > 0 && uptimeMs() - start >= (uint64_t)timeoutMs) {
            return 0;
        }
        sleep(1);
    }
}
int64_t
Syscalls::stat(const char *path, FileStat *out) { return call(SYS_STAT, ARG(path), ARG(out)); }
int64_t
Syscalls::fstat(int fd, FileStat *out) { return call(SYS_FSTAT, (uint64_t)(int64_t)fd, ARG(out)); }
int64_t
Syscalls::getdents(int fd, void *buffer, size_t size) { return call(SYS_GETDENTS, (uint64_t)(int64_t)fd, ARG(buffer), size); }
int64_t
Syscalls::mkdir(const char *path) { return call(SYS_MKDIR, ARG(path)); }
int64_t
Syscalls::rmdir(const char *path) { return call(SYS_RMDIR, ARG(path)); }
int64_t
Syscalls::unlink(const char *path) { return call(SYS_UNLINK, ARG(path)); }
int64_t
Syscalls::chdir(const char *path) { return call(SYS_CHDIR, ARG(path)); }
int64_t
Syscalls::getcwd(char *buffer, size_t size) { return call(SYS_GETCWD, ARG(buffer), size); }
int64_t
Syscalls::mount(const char *source, const char *target, const char *type) {
    return call(SYS_MOUNT, ARG(source), ARG(target), ARG(type));
}
int64_t
Syscalls::umount(const char *target) { return call(SYS_UMOUNT, ARG(target)); }
unsigned
Syscalls::mountList(MountInfo *out, unsigned max) { return (unsigned)call(SYS_MOUNT_LIST, ARG(out), max); }
int64_t
Syscalls::netInterface(unsigned index, NetInterfaceInfo *out) { return call(SYS_NET_INTERFACE, index, ARG(out)); }
int64_t
Syscalls::netConfigure(const NetConfig *config) { return call(SYS_NET_CONFIGURE, ARG(config)); }

int64_t
Syscalls::tcpWait(uint64_t number, int64_t socket, void *buffer, size_t size, uint32_t timeoutMs) {
    int64_t address = call(SYS_TCP_EVENT, (uint64_t)socket);
    if (address <= 0) {
        return -Err::BADF;
    }
    int *word = (int *)address;
    uint64_t deadline = timeoutMs ? uptimeMs() + timeoutMs : 0;
    for (;;) {
        int seen = __atomic_load_n(word, __ATOMIC_SEQ_CST);
        int64_t result = call(number, (uint64_t)socket, ARG(buffer), size);
        if (result != -ERR_AGAIN) {
            return result;
        }
        uint32_t wait = 0;
        if (deadline) {
            uint64_t now = uptimeMs();
            if (now >= deadline) {
                return -ERR_TIMEDOUT;
            }
            wait = (uint32_t)(deadline - now);
        }
        futexWait(word, seen, wait);
    }
}

int64_t
Syscalls::tcpListen(uint16_t port) { return call(SYS_TCP_LISTEN, port); }
int64_t
Syscalls::tcpAccept(int64_t listener, uint32_t timeoutMs) { return tcpWait(SYS_TCP_ACCEPT, listener, nullptr, 0, timeoutMs); }
int64_t
Syscalls::tcpReceive(int64_t socket, void *buffer, size_t size, uint32_t timeoutMs) {
    return tcpWait(SYS_TCP_RECV, socket, buffer, size, timeoutMs);
}
int64_t
Syscalls::tcpSend(int64_t socket, const void *buffer, size_t size) {
    const uint8_t *bytes = (const uint8_t *)buffer;
    size_t done = 0;
    while (done < size) {
        int64_t n = tcpWait(SYS_TCP_SEND, socket, (void *)(bytes + done), size - done, 0);
        if (n < 0) {
            return n;
        }
        done += (size_t)n;
    }
    return (int64_t)done;
}
int64_t
Syscalls::tcpClose(int64_t socket) { return call(SYS_TCP_CLOSE, (uint64_t)socket); }
int64_t
Syscalls::tcpPeer(int64_t socket, uint8_t address[4], uint16_t *port) {
    return call(SYS_TCP_PEER, (uint64_t)socket, ARG(address), ARG(port));
}

void
Syscalls::init() {
    if (!Interrupts::registerHandler(Interrupts::SYSCALL, handle)) {
        Panic::halt("syscall vector already taken");
    }
}

TrapFrame *
Syscalls::handle(TrapFrame *f) {
    const uint64_t arg0 = f->argument(0);
    const uint64_t arg1 = f->argument(1);
    const uint64_t arg2 = f->argument(2);
    switch (f->number()) {
    case SYS_SCHED_YIELD:
        f->setResult(0);
        return Scheduler::yield(f);
    case SYS_GETPID:
        f->setResult(Scheduler::current()->pid());
        return f;
    case SYS_GETPPID:
        f->setResult(Scheduler::current()->ppid());
        return f;
    case SYS_NANOSLEEP: {
        const TimeSpec *ts = (const TimeSpec *)arg0;
        if (!ts || ts->seconds < 0 || ts->nanoseconds < 0 || ts->nanoseconds >= 1000000000) {
            f->setResult((uint64_t)-ERR_INVAL);
            return f;
        }
        f->setResult(0);
        return Scheduler::sleep(f, (uint64_t)ts->seconds * 1000000000ull + (uint64_t)ts->nanoseconds);
    }
    case SYS_EXIT:
        return Scheduler::exit(f, (int)arg0);
    case SYS_WAIT4:
        return Scheduler::wait(f, (int64_t)arg0, (int *)arg1, arg2 & WNOHANG);
    case SYS_EXECVE: {
        const char *name = (const char *)arg0;
        const ArgList *args = (const ArgList *)arg1;
        const Program *program = name ? ProgramRegistry::find(name) : nullptr;
        if (!program || !args) {
            f->setResult((uint64_t)-ERR_NOENT);
            return f;
        }
        return Scheduler::exec(f, *program, *args);
    }
    case SYS_PROCESS_LIST:
        f->setResult(Scheduler::snapshot((ProcessInfo *)arg0, (unsigned)arg1));
        return f;
    case SYS_CPU_LOAD:
        f->setResult(Scheduler::cpuLoad((CpuLoadInfo *)arg0, (unsigned)arg1));
        return f;
    case SYS_GETCONSOLE: {
        int64_t console = arg0 ? (int64_t)Scheduler::consoleOf((uint32_t)arg0) : (int64_t)Scheduler::current()->console();
        f->setResult(console < 0 ? (uint64_t)-ERR_SRCH : (uint64_t)console);
        return f;
    }
    case SYS_SWITCH_CONSOLE:
        Consoles::switchTo((unsigned)arg0);
        f->setResult(0);
        return f;
    case SYS_SPAWN:
    case SYS_RUN: {
        const char *name = (const char *)arg0;
        const ArgList *args = (const ArgList *)arg1;
        const Program *program = name ? ProgramRegistry::find(name) : nullptr;
        if (!program || !args) {
            f->setResult((uint64_t)-ERR_NOENT);
            return f;
        }
        Process *child = Scheduler::spawn(
            *program, *args, Scheduler::current()->pid(), CpuMask::all(),
            f->number() == SYS_RUN ? (int)Scheduler::current()->console() : -1, f->number() == SYS_RUN);
        f->setResult(child ? child->pid() : (uint64_t)-ERR_AGAIN);
        return f;
    }
    case SYS_THREAD_CREATE: {
        Process *self = Scheduler::current();
        Process *thread = Scheduler::spawnThread((void (*)(void *))arg0, (void *)arg1,
                                                 arg2 ? (size_t)arg2 : Process::THREAD_STACK_SIZE, self,
                                                 CpuMask::all());
        f->setResult(thread ? thread->pid() : (uint64_t)-ERR_AGAIN);
        return f;
    }
    case SYS_FUTEX_WAIT:
        return Scheduler::futexWait(f, (int *)arg0, (int)arg1, arg2 * 1000000ull);
    case SYS_FUTEX_WAKE:
        f->setResult((uint64_t)(int64_t)Scheduler::futexWake((int *)arg0, (int)arg1));
        return f;
    case SYS_UPTIME_MS:
        f->setResult(Clock::uptimeMs());
        return f;
    case SYS_UPTIME:
        if (arg0) {
            *(int64_t *)arg0 = WallClock::startSeconds();
        }
        f->setResult(WallClock::uptimeSeconds());
        return f;
    case SYS_TIME:
        f->setResult((uint64_t)WallClock::seconds());
        return f;
    case SYS_CLOCK_GETTIME: {
        TimeSpec *out = (TimeSpec *)arg1;
        if (!out) { f->setResult((uint64_t)-Err::FAULT); return f; }
        if (arg0 != CLOCK_WALL && arg0 != CLOCK_UPTIME) { f->setResult((uint64_t)-Err::INVAL); return f; }
        uint64_t ns = arg0 == CLOCK_WALL ? WallClock::nanoseconds() : Clock::nanoseconds();
        out->seconds = (int64_t)(ns / 1000000000ull);
        out->nanoseconds = (int64_t)(ns % 1000000000ull);
        f->setResult(0);
        return f;
    }
    case SYS_MOUNT_LIST:
        f->setResult(arg0 ? Vfs::mounts((MountInfo *)arg0, (unsigned)arg1) : (uint64_t)-Err::FAULT);
        return f;
    case SYS_NET_INTERFACE:
        f->setResult(arg1 ? (uint64_t)Net::describe((unsigned)arg0, *(NetInterfaceInfo *)arg1) : (uint64_t)-Err::FAULT);
        return f;
    case SYS_NET_CONFIGURE:
        f->setResult(arg0 ? (uint64_t)Net::configure(*(const NetConfig *)arg0) : (uint64_t)-Err::FAULT);
        return f;
    case SYS_TCP_LISTEN:
        f->setResult((uint64_t)Tcp::listen((uint16_t)arg0));
        return f;
    case SYS_TCP_ACCEPT:
        f->setResult((uint64_t)Tcp::accept((int64_t)arg0));
        return f;
    case SYS_TCP_RECV:
        f->setResult(arg1 ? (uint64_t)Tcp::receive((int64_t)arg0, (void *)arg1, arg2) : (uint64_t)-Err::FAULT);
        return f;
    case SYS_TCP_SEND:
        f->setResult(arg1 ? (uint64_t)Tcp::send((int64_t)arg0, (const void *)arg1, arg2) : (uint64_t)-Err::FAULT);
        return f;
    case SYS_TCP_CLOSE:
        f->setResult((uint64_t)Tcp::close((int64_t)arg0));
        return f;
    case SYS_TCP_EVENT: {
        int *word = Tcp::eventWord((int64_t)arg0);
        f->setResult(word ? (uint64_t)(uintptr_t)word : (uint64_t)-Err::BADF);
        return f;
    }
    case SYS_TCP_PEER: {
        Ipv4Address address;
        uint16_t port;
        if (!arg1 || !arg2 || !Tcp::peer((int64_t)arg0, address, port)) {
            f->setResult((uint64_t)-Err::BADF);
            return f;
        }
        Memory::copy((void *)arg1, address.data(), 4);
        *(uint16_t *)arg2 = port;
        f->setResult(0);
        return f;
    }
    default:
        f->setResult((uint64_t)fileCall(f));
        return f;
    }
}

int64_t
Syscalls::fileCall(TrapFrame *f) {
    const uint64_t arg0 = f->argument(0);
    const uint64_t arg1 = f->argument(1);
    const uint64_t arg2 = f->argument(2);
    const uint64_t arg3 = f->argument(3);
    Process *self = Scheduler::current();
    OpenFile *file = nullptr;
    switch (f->number()) {
    case SYS_READ: case SYS_WRITE: case SYS_PREAD: case SYS_PWRITE: case SYS_LSEEK: case SYS_IOCTL:
    case SYS_FSTAT: case SYS_FSYNC: case SYS_FDATASYNC: case SYS_FTRUNCATE: case SYS_GETDENTS:
        file = self->file((int)arg0);
        if (!file) {
            return -Err::BADF;
        }
        break;
    default:
        break;
    }
    switch (f->number()) {
    case SYS_POLL: {
        PollRecord *records = (PollRecord *)arg0;
        if (!records && arg1) {
            return -Err::FAULT;
        }
        int64_t ready = 0;
        for (uint64_t i = 0; i < arg1; i++) {
            OpenFile *polled = records[i].fd >= 0 ? self->file(records[i].fd) : nullptr;
            records[i].revents = records[i].fd < 0 ? 0
                                 : polled          ? (int16_t)Vfs::poll(polled, records[i].events)
                                                   : PollEvent::NVAL;
            if (records[i].revents) {
                ready++;
            }
        }
        return ready;
    }
    case SYS_OPEN: {
        if (!arg0) {
            return -Err::FAULT;
        }
        OpenFile *opened = nullptr;
        int64_t r = Vfs::open(self->cwd(), (const char *)arg0, (int)arg1, opened);
        if (r < 0) {
            return r;
        }
        int fd = self->addFile(opened);
        if (fd < 0) {
            Vfs::close(opened);
            return -Err::MFILE;
        }
        return fd;
    }
    case SYS_CLOSE: {
        OpenFile *taken = self->takeFile((int)arg0);
        if (!taken) {
            return -Err::BADF;
        }
        Vfs::close(taken);
        return 0;
    }
    case SYS_READ:
        return arg1 || !arg2 ? Vfs::read(file, (void *)arg1, arg2) : -Err::FAULT;
    case SYS_WRITE:
        return arg1 || !arg2 ? Vfs::write(file, (const void *)arg1, arg2) : -Err::FAULT;
    case SYS_PREAD:
        return arg1 || !arg2 ? Vfs::pread(file, (void *)arg1, arg2, arg3) : -Err::FAULT;
    case SYS_PWRITE:
        return arg1 || !arg2 ? Vfs::pwrite(file, (const void *)arg1, arg2, arg3) : -Err::FAULT;
    case SYS_LSEEK:
        return Vfs::lseek(file, (int64_t)arg1, (int)arg2);
    case SYS_IOCTL:
        return Vfs::ioctl(file, arg1, (void *)arg2);
    case SYS_FSTAT:
        return arg1 ? Vfs::fstat(file, *(FileStat *)arg1) : -Err::FAULT;
    case SYS_FSYNC: case SYS_FDATASYNC:
        return Vfs::sync(file);
    case SYS_FTRUNCATE:
        return Vfs::truncate(file, arg1);
    case SYS_GETDENTS:
        return arg1 ? Vfs::getdents(file, (void *)arg1, arg2) : -Err::FAULT;
    case SYS_STAT:
        return arg0 && arg1 ? Vfs::stat(self->cwd(), (const char *)arg0, *(FileStat *)arg1) : -Err::FAULT;
    case SYS_MKDIR:
        return arg0 ? Vfs::mkdir(self->cwd(), (const char *)arg0) : -Err::FAULT;
    case SYS_RMDIR:
        return arg0 ? Vfs::rmdir(self->cwd(), (const char *)arg0) : -Err::FAULT;
    case SYS_UNLINK:
        return arg0 ? Vfs::unlink(self->cwd(), (const char *)arg0) : -Err::FAULT;
    case SYS_CHDIR: {
        if (!arg0) {
            return -Err::FAULT;
        }
        char canonical[Vfs::PATH_MAX];
        int64_t r = Vfs::chdir(self->cwd(), (const char *)arg0, canonical);
        if (r == 0) {
            String::copy(self->cwd(), canonical, Process::CWD_LENGTH);
        }
        return r;
    }
    case SYS_GETCWD: {
        if (!arg0) {
            return -Err::FAULT;
        }
        size_t length = String::length(self->cwd()) + 1;
        if (arg1 < length) {
            return -Err::INVAL;
        }
        Memory::copy((void *)arg0, self->cwd(), length);
        return (int64_t)length;
    }
    case SYS_MOUNT:
        return Vfs::mount(self->cwd(), (const char *)arg0, (const char *)arg1, (const char *)arg2);
    case SYS_UMOUNT:
        return Vfs::umount(self->cwd(), (const char *)arg0);
    default:
        return -Err::NOSYS;
    }
}
