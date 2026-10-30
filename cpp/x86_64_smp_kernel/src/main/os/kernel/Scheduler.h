#ifndef __SCHEDULER__
#define __SCHEDULER__
#include <stdint.h>
#include "kernel/Process.h"
#include "kernel/Program.h"
#include "lib/vitralKernel/SpinLock.h"
#include "syscalls/Syscalls.h"
#include "kernel/ArgList.h"

class Scheduler {
public:
    static constexpr unsigned MAX_PROCESSES = 256;

    static void init(unsigned cpus);
    static Process *spawn(const Program &program, const ArgList &args, uint32_t ppid,
                          const CpuMask &affinity, int inheritConsole = -1, bool waitable = false);
    static Process *spawnThread(void (*entry)(void *), void *argument, size_t stackSize, Process *creator,
                                const CpuMask &affinity);
    [[noreturn]] static void start();
    [[noreturn]] static void startCpu();
    static void useLapicTimers();
    static bool usesLapicTimers() { return lapicTimers; }

    static Process *current();
    static int consoleOf(uint32_t pid);
    static unsigned currentConsole();
    static unsigned cpuCount() { return cpuTotal; }
    static bool setAffinity(uint32_t pid, const unsigned long *bits);
    static unsigned onlineCount();
    static constexpr unsigned TICKS_PER_PIT_TICK = 4;
    static bool isRunning() { return running; }

    static TrapFrame *tick(TrapFrame *frame);
    static TrapFrame *yield(TrapFrame *frame);
    static TrapFrame *sleep(TrapFrame *frame, uint64_t nanoseconds);
    static TrapFrame *exit(TrapFrame *frame, int code);
    static TrapFrame *wait(TrapFrame *frame, int64_t pid, int *status, bool noHang);
    static TrapFrame *futexWait(TrapFrame *frame, int *address, int expected, uint64_t timeoutNs);
    static int futexWake(int *address, int count);
    static TrapFrame *exec(TrapFrame *frame, const Program &program, const ArgList &args);

    static unsigned snapshot(Syscalls::ProcessInfo *out, unsigned max);
    static unsigned cpuLoad(Syscalls::CpuLoadInfo *out, unsigned max);

private:
    class CpuState {
    public:
        Process *current;
        Process *idle;
        Process *head;
        Process *tail;
        unsigned queued;
        bool online;
        Process *zombie;
        uint64_t busyCycles;
        uint64_t idleCycles;
        uint64_t since;
    };

    static constexpr unsigned FUTEX_BUCKETS = 64;

    static CpuState cpus[CpuMask::MAX];
    static Process *futexBuckets[FUTEX_BUCKETS];
    static uint64_t earliestWake;
    static unsigned cpuTotal;
    static Process *table[MAX_PROCESSES];
    static uint32_t nextPid;
    static bool running;
    static bool lapicTimers;
    static bool tickless;
    static SpinLock lock;

    static unsigned cpuIndex();
    static unsigned chooseCpu(const Process &p);
    static void enqueue(Process *p);
    static bool collect(Process *child, Process *parent, int *status);
    static void childEnded(Process *child);
    static void armTimer(unsigned cpu, const Process *next);
    static void notifyIdle(unsigned cpu);
    static Process *steal(unsigned thief);
    static unsigned bucketOf(const int *address);
    static void futexLink(Process *p);
    static void futexUnlink(Process *p);
    static void setDeadline(Process *p, uint64_t nanoseconds);
    static TrapFrame *onReschedule(TrapFrame *frame);
    static Process *dequeue(CpuState &cpu);
    static TrapFrame *switchFrom(unsigned index, TrapFrame *frame);
    static void wakeSleepers(uint64_t now);
    static void reapZombie(CpuState &cpu);
    static TrapFrame *onLapicTimer(TrapFrame *frame);
};

#endif
