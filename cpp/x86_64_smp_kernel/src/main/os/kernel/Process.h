#ifndef __PROCESS__
#define __PROCESS__
#include <stdint.h>
#include "kernel/Fpu.h"
#include "kernel/Program.h"
#include "kernel/TrapFrame.h"
#include "lib/vitralKernel/CpuMask.h"
#include "lib/vitralKernel/SpinLock.h"
#include "kernel/ArgList.h"
#include "fs/OpenFile.h"

class OpenFile;

class Process {
public:
    enum class State : uint8_t { Ready, Running, Sleeping, Zombie, Blocked };

    static constexpr unsigned NAME_LENGTH = 16;
    static constexpr size_t STACK_SIZE = 64 * 1024;
    static constexpr size_t THREAD_STACK_SIZE = 256 * 1024;
    static constexpr unsigned KEY_COUNT = 16;
    static constexpr unsigned MAX_FILES = 32;
    static constexpr unsigned FIRST_FILE = 3;
    static constexpr unsigned CWD_LENGTH = 256;
    static constexpr uint32_t WAIT_ANY = 0xFFFFFFFFu;

    Process(uint32_t pid, uint32_t ppid, const Program &program, const ArgList &args,
            const CpuMask &affinity, unsigned console, bool ownsConsole, char *stack);
    Process(uint32_t pid, Process *creator, void (*entry)(void *), void *argument, char *stack, size_t stackSize,
            const CpuMask &affinity);
    static char *allocateStack(size_t size);
    static Process *makeIdle(unsigned cpu);
    ~Process();
    Process(const Process &) = delete;
    Process &operator=(const Process &) = delete;

    uint32_t pid() const { return pidValue; }
    uint32_t ppid() const { return ppidValue; }
    const char *name() const { return nameBuffer; }
    State state() const { return stateValue; }
    const char *stateName() const;
    unsigned console() const { return consoleValue; }
    bool ownsConsole() const { return ownsConsoleValue; }
    int cpu() const { return cpuValue; }
    uint64_t ticks() const { return cpuTicks; }
    int *errorNumber() { return &errorValue; }
    void **specific() { return keyValues; }
    void *threadRecord() const { return threadData; }
    void setThreadRecord(void *record) { threadData = record; }
    const CpuMask &affinity() const { return affinityMask; }
    void setAffinity(const CpuMask &m) { affinityMask = m; }
    void setName(const char *name);
    int exitCode() const { return exitCodeValue; }

    OpenFile *file(int fd) const { return fd >= 0 && (unsigned)fd < MAX_FILES ? resources->files[fd] : nullptr; }
    int addFile(OpenFile *open);
    OpenFile *takeFile(int fd);
    char *cwd() { return resources->cwd; }

private:
    friend class Scheduler;

    Process();
    void setProgram(const Program &program, const ArgList &args);
    void releaseStack();
    // Builds the first TrapFrame of the process, which resumes in start() (kernel/<arch>/ProcessFrame.cpp).
    void prepareFrame();
    [[noreturn]] static void start();

    uint32_t pidValue;
    uint32_t ppidValue;
    unsigned consoleValue;
    bool ownsConsoleValue;
    char nameBuffer[NAME_LENGTH];
    State stateValue;
    int cpuValue;
    CpuMask affinityMask;
    const Program *program;
    ArgList args;
    class Resources {
    public:
        OpenFile *files[MAX_FILES];
        char cwd[CWD_LENGTH];
        unsigned references;
        SpinLock lock;
    };
    static constexpr unsigned STACK_POOL = 32;
    static char *pooledStacks[STACK_POOL];
    static size_t pooledSizes[STACK_POOL];

    char *stack;
    size_t stackSize;
    void (*threadEntry)(void *);
    void *threadArgument;
    void *threadData;
    int *waitAddress;
    Fpu::Image fpu;
    TrapFrame *frame;
    uint64_t wakeNs;
    uint64_t cpuTicks;
    Process *waitNext;
    int errorValue;
    int exitCodeValue;
    bool waitable;
    bool exited;
    uint32_t waitingFor;
    int *waitStatus;
    void *keyValues[KEY_COUNT];
    Resources *resources;
    Process *nextInQueue;
};

#endif
