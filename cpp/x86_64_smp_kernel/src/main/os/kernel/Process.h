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
class Tty;

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
            const CpuMask &affinity, unsigned console, bool ownsConsole, Tty *terminal, char *stack);
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
    // Its standard input, output and error. The console is still where its graphics and keyboard are (svgalib), and
    // the terminal is that console's unless the process was started on another one, such as /dev/ttyS0.
    Tty *terminal() const { return terminalValue; }
    // When its terminal is not its console's (it was started on another one, such as /dev/ttyS0), its console is not
    // necessarily on the screen: this puts it there, for graphics, until the process (with its threads) exits, and
    // then the screen shows again the console it showed before. Nothing when the terminal is the console's.
    void showConsole();
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

    // Run queue and futex bucket links, sleeps and waits: the state that the Scheduler keeps of each process (only
    // the Scheduler changes it). Value initialized: no links, no wait.
    struct Scheduling {
        Process *nextInQueue;
        Process *waitNext;
        int *waitAddress;
        uint64_t wakeNs;
        bool waitable;
        bool exited;
        uint32_t waitingFor;
        int *waitStatus;
    };
    // Changed by the Scheduler
    // Frees the stack (back to the pool) of a process that has exited
    void releaseStack();
    // exec: replaces the program and arguments of the process
    void setProgram(const Program &program, const ArgList &args);
    // On exit: gives the screen back if showConsole() took it and nothing has changed it since.
    void returnScreen();
    void setState(State s) { stateValue = s; }
    void setCpu(int index) { cpuValue = index; }
    void addTick() { cpuTicks++; }
    void setExitCode(int code) { exitCodeValue = code; }
    TrapFrame *trapFrame() const { return frame; }
    void setTrapFrame(TrapFrame *f) { frame = f; }
    Fpu::Image &fpuImage() { return fpu; }

private:

    Process();
    // Builds the first TrapFrame of the process, which resumes in start() (kernel/<arch>/ProcessFrame.cpp).
    void prepareFrame();
    [[noreturn]] static void start();

    uint32_t pidValue;
    uint32_t ppidValue;
    unsigned consoleValue;
    bool ownsConsoleValue;
    Tty *terminalValue;
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
        // The console showConsole() took the screen from, or -1.
        int screenReturn;
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
    Fpu::Image fpu;
    TrapFrame *frame;
    uint64_t cpuTicks;
    int errorValue;
    int exitCodeValue;
    void *keyValues[KEY_COUNT];
    Resources *resources;

public:
    Scheduling scheduling;
};

#endif
