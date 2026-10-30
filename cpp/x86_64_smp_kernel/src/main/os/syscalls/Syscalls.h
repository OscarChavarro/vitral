#ifndef __SYSCALLS__
#define __SYSCALLS__
#include <stdint.h>
#include "kernel/Program.h"
#include "kernel/TrapFrame.h"
#include "fs/PollRecord.h"
#include "fs/FileStat.h"
#include "fs/MountInfo.h"
#include "net/NetInterfaceInfo.h"
#include "net/NetConfig.h"
#include "kernel/ArgList.h"

class Syscalls {
public:
    static constexpr uint64_t SYS_SCHED_YIELD = 24;
    static constexpr uint64_t SYS_NANOSLEEP = 35;
    static constexpr uint64_t SYS_GETPID = 39;
    static constexpr uint64_t SYS_EXECVE = 59;
    static constexpr uint64_t SYS_EXIT = 60;
    static constexpr uint64_t SYS_WAIT4 = 61;
    static constexpr uint64_t SYS_GETPPID = 110;
    static constexpr uint64_t SYS_TIME = 201;
    static constexpr uint64_t SYS_CLOCK_GETTIME = 228;
    static constexpr uint64_t SYS_PROCESS_LIST = 0x1000;
    static constexpr uint64_t SYS_UPTIME_MS = 0x1001;
    static constexpr uint64_t SYS_GETCONSOLE = 0x1002;
    static constexpr uint64_t SYS_SPAWN = 0x1003;
    static constexpr uint64_t SYS_RUN = 0x1004;
    static constexpr uint64_t SYS_SWITCH_CONSOLE = 0x1005;
    static constexpr uint64_t SYS_THREAD_CREATE = 0x1006;
    static constexpr uint64_t SYS_FUTEX_WAIT = 0x1007;
    static constexpr uint64_t SYS_FUTEX_WAKE = 0x1008;
    static constexpr uint64_t SYS_MOUNT_LIST = 0x1009;
    static constexpr uint64_t SYS_NET_INTERFACE = 0x100A;
    static constexpr uint64_t SYS_NET_CONFIGURE = 0x100B;
    static constexpr uint64_t SYS_TCP_LISTEN = 0x100C;
    static constexpr uint64_t SYS_TCP_ACCEPT = 0x100D;
    static constexpr uint64_t SYS_TCP_RECV = 0x100E;
    static constexpr uint64_t SYS_TCP_SEND = 0x100F;
    static constexpr uint64_t SYS_TCP_CLOSE = 0x1010;
    static constexpr uint64_t SYS_TCP_EVENT = 0x1011;
    static constexpr uint64_t SYS_TCP_PEER = 0x1012;
    static constexpr uint64_t SYS_UPTIME = 0x1013;
    static constexpr uint64_t SYS_CPU_LOAD = 0x1014;
    static constexpr uint64_t SYS_READ = 0;
    static constexpr uint64_t SYS_WRITE = 1;
    static constexpr uint64_t SYS_OPEN = 2;
    static constexpr uint64_t SYS_CLOSE = 3;
    static constexpr uint64_t SYS_STAT = 4;
    static constexpr uint64_t SYS_FSTAT = 5;
    static constexpr uint64_t SYS_POLL = 7;
    static constexpr uint64_t SYS_LSEEK = 8;
    static constexpr uint64_t SYS_IOCTL = 16;
    static constexpr uint64_t SYS_PREAD = 17;
    static constexpr uint64_t SYS_PWRITE = 18;
    static constexpr uint64_t SYS_FSYNC = 74;
    static constexpr uint64_t SYS_FDATASYNC = 75;
    static constexpr uint64_t SYS_FTRUNCATE = 77;
    static constexpr uint64_t SYS_GETCWD = 79;
    static constexpr uint64_t SYS_CHDIR = 80;
    static constexpr uint64_t SYS_MKDIR = 83;
    static constexpr uint64_t SYS_RMDIR = 84;
    static constexpr uint64_t SYS_UNLINK = 87;
    static constexpr uint64_t SYS_MOUNT = 165;
    static constexpr uint64_t SYS_UMOUNT = 166;
    static constexpr uint64_t SYS_GETDENTS = 217;
    static constexpr int64_t ERR_NOENT = 2;
    static constexpr int64_t ERR_AGAIN = 11;
    static constexpr int64_t ERR_INVAL = 22;
    static constexpr int64_t ERR_SRCH = 3;
    static constexpr int64_t ERR_TIMEDOUT = 110;

    class TimeSpec {
    public:
        int64_t seconds;
        int64_t nanoseconds;
    };
    static constexpr int WNOHANG = 1;
    static constexpr int CLOCK_WALL = 0;
    static constexpr int CLOCK_UPTIME = 1;

    class ProcessInfo {
    public:
        uint32_t pid;
        uint32_t ppid;
        char name[16];
        uint8_t state;
        int32_t cpu;
        uint8_t console;
        uint64_t ticks;
        uint64_t affinity;
    };

    class CpuLoadInfo {
    public:
        uint64_t busyCycles;
        uint64_t idleCycles;
        uint8_t online;
    };

    static int64_t call(uint64_t number, uint64_t a = 0, uint64_t b = 0, uint64_t c = 0, uint64_t d = 0);
    static void yield() { call(SYS_SCHED_YIELD); }
    static void sleep(uint32_t milliseconds);
    static uint32_t getpid() { return (uint32_t)call(SYS_GETPID); }
    static uint32_t getppid() { return (uint32_t)call(SYS_GETPPID); }
    [[noreturn]] static void exit(int code);
    static int64_t exec(const char *name, const ArgList &args);
    static unsigned listProcesses(ProcessInfo *out, unsigned max);
    static unsigned cpuLoad(CpuLoadInfo *out, unsigned max);
    static unsigned console() { return (unsigned)call(SYS_GETCONSOLE); }
    static int64_t consoleOf(uint32_t pid) { return call(SYS_GETCONSOLE, pid); }
    static void switchConsole(unsigned console) { call(SYS_SWITCH_CONSOLE, console); }
    static int64_t spawn(const char *name, const ArgList &args);
    static int64_t run(const char *name, const ArgList &args);
    static int64_t waitpid(int64_t pid, int *status, int options = 0);
    static int exitCode(int status) { return (status >> 8) & 0xFF; }
    static int64_t createThread(void (*entry)(void *), void *argument, size_t stackSize);
    static int futexWait(int *address, int expected, uint32_t timeoutMs);
    static int futexWake(int *address, int count);
    static int64_t clockGetTime(int clock, TimeSpec *out);
    static int64_t time() { return call(SYS_TIME, 0); }
    static uint64_t uptime(int64_t *startedAt = nullptr) { return (uint64_t)call(SYS_UPTIME, (uint64_t)startedAt); }
    static uint64_t uptimeMs() { return (uint64_t)call(SYS_UPTIME_MS); }

    static int64_t open(const char *path, int flags);
    static int64_t close(int fd);
    static int64_t read(int fd, void *buffer, size_t count);
    static int64_t write(int fd, const void *buffer, size_t count);
    static int64_t pread(int fd, void *buffer, size_t count, uint64_t offset);
    static int64_t pwrite(int fd, const void *buffer, size_t count, uint64_t offset);
    static int64_t lseek(int fd, int64_t offset, int whence);
    static int64_t ftruncate(int fd, uint64_t size);
    static int64_t fsync(int fd);
    static int64_t ioctl(int fd, uint64_t request, void *argument = nullptr);
    static int64_t poll(PollRecord *records, unsigned count, int timeoutMs);
    static int64_t stat(const char *path, FileStat *out);
    static int64_t fstat(int fd, FileStat *out);
    static int64_t getdents(int fd, void *buffer, size_t size);
    static int64_t mkdir(const char *path);
    static int64_t rmdir(const char *path);
    static int64_t unlink(const char *path);
    static int64_t chdir(const char *path);
    static int64_t getcwd(char *buffer, size_t size);
    static int64_t mount(const char *source, const char *target, const char *type = "vfat");
    static int64_t umount(const char *target);
    static unsigned mountList(MountInfo *out, unsigned max);

    static int64_t netInterface(unsigned index, NetInterfaceInfo *out);
    static int64_t netConfigure(const NetConfig *config);

    static int64_t tcpListen(uint16_t port);
    static int64_t tcpAccept(int64_t listener, uint32_t timeoutMs = 0);
    static int64_t tcpReceive(int64_t socket, void *buffer, size_t size, uint32_t timeoutMs = 0);
    static int64_t tcpSend(int64_t socket, const void *buffer, size_t size);
    static int64_t tcpClose(int64_t socket);
    static int64_t tcpPeer(int64_t socket, uint8_t address[4], uint16_t *port);

    static void init();
    static TrapFrame *handle(TrapFrame *frame);

private:
    static int64_t fileCall(TrapFrame *frame);
    static int64_t tcpWait(uint64_t number, int64_t socket, void *buffer, size_t size, uint32_t timeoutMs);
};

#endif
