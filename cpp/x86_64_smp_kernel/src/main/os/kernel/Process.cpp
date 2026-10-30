#include "kernel/Process.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "fs/Vfs.h"
#include "kernel/KernelHeap.h"
#include "kernel/Scheduler.h"
#include "lib/vitralKernel/Memory.h"
#include "syscalls/Syscalls.h"
#include "lib/vitralKernel/String.h"
#include "fs/OpenFile.h"
#include "lib/vitralKernel/LockGuard.h"
#include "kernel/ArgList.h"

char *Process::pooledStacks[Process::STACK_POOL];
size_t Process::pooledSizes[Process::STACK_POOL];

Process::Process()
    : pidValue(0), ppidValue(0), consoleValue(0), ownsConsoleValue(true), stateValue(State::Ready), cpuValue(-1), program(nullptr),
      stack(nullptr), stackSize(STACK_SIZE), threadEntry(nullptr), threadArgument(nullptr), threadData(nullptr),
      waitAddress(nullptr), frame(nullptr), wakeNs(0), cpuTicks(0), waitNext(nullptr), errorValue(0), exitCodeValue(0), waitable(false),
      exited(false), waitingFor(0), waitStatus(nullptr), resources(nullptr),
      nextInQueue(nullptr) {
    nameBuffer[0] = 0;
    for (void *&v : keyValues) {
        v = nullptr;
    }
    Fpu::initialize(fpu);
}

char *
Process::allocateStack(size_t size) {
    for (unsigned i = 0; i < STACK_POOL; i++) {
        if (pooledStacks[i] && pooledSizes[i] == size) {
            char *s = pooledStacks[i];
            pooledStacks[i] = nullptr;
            return s;
        }
    }
    return (char *)KernelHeap::tryAllocate(size);
}

Process::Process(uint32_t pid, uint32_t ppid, const Program &prog, const ArgList &a,
                 const CpuMask &affinity, unsigned console, bool ownsConsole, char *stackMemory) : Process() {
    pidValue = pid;
    consoleValue = console;
    ownsConsoleValue = ownsConsole;
    ppidValue = ppid;
    affinityMask = affinity;
    stack = stackMemory;
    resources = new Resources();
    resources->references = 1;
    for (OpenFile *&f : resources->files) {
        f = nullptr;
    }
    resources->cwd[0] = '/';
    resources->cwd[1] = 0;
    setProgram(prog, a);
}

Process::Process(uint32_t pid, Process *creator, void (*entry)(void *), void *argument, char *stackMemory, size_t size,
                 const CpuMask &affinity) : Process() {
    pidValue = pid;
    ppidValue = creator->pidValue;
    consoleValue = creator->consoleValue;
    ownsConsoleValue = false;
    affinityMask = affinity;
    stackSize = size;
    stack = stackMemory;
    resources = creator->resources;
    resources->references++;
    threadEntry = entry;
    threadArgument = argument;
    String::copy(nameBuffer, "thread", NAME_LENGTH);
    prepareFrame();
}

Process *
Process::makeIdle(unsigned cpu) {
    Process *p = new Process();
    p->cpuValue = (int)cpu;
    p->stateValue = State::Ready;
    p->affinityMask = CpuMask::only(cpu);
    String::copy(p->nameBuffer, "idle", NAME_LENGTH);
    return p;
}

Process::~Process() {
    if (resources && --resources->references == 0) {
        for (OpenFile *&f : resources->files) {
            Vfs::close(f);
            f = nullptr;
        }
        delete resources;
    }
    releaseStack();
}

void
Process::releaseStack() {
    if (!stack) {
        return;
    }
    char *memory = stack;
    stack = nullptr;
    for (unsigned i = 0; i < STACK_POOL; i++) {
        if (!pooledStacks[i]) {
            pooledStacks[i] = memory;
            pooledSizes[i] = stackSize;
            return;
        }
    }
    KernelHeap::release(memory);
}

int
Process::addFile(OpenFile *open) {
    LockGuard guard(resources->lock);
    for (unsigned i = FIRST_FILE; i < MAX_FILES; i++) {
        if (!resources->files[i]) {
            resources->files[i] = open;
            return (int)i;
        }
    }
    return -1;
}

OpenFile *
Process::takeFile(int fd) {
    LockGuard guard(resources->lock);
    OpenFile *f = file(fd);
    if (f) {
        resources->files[fd] = nullptr;
    }
    return f;
}

void
Process::setName(const char *name) { String::copy(nameBuffer, name, NAME_LENGTH); }

void
Process::setProgram(const Program &prog, const ArgList &a) {
    program = &prog;
    args = a;
    String::copy(nameBuffer, prog.name, NAME_LENGTH);
    prepareFrame();
}

void
Process::start() {
    Process *self = Scheduler::current();
    if (self->threadEntry) {
        self->threadEntry(self->threadArgument);
        Syscalls::exit(0);
    }
    if (self->ownsConsoleValue) {
        VirtualConsole::clear(self->console(), 0x07);
    }
    int code = self->program->main(self->args);
    Syscalls::exit(code);
}

const char *
Process::stateName() const {
    switch (stateValue) {
    case State::Ready: return "READY";
    case State::Running: return "RUN";
    case State::Sleeping: return "SLEEP";
    case State::Blocked: return "WAIT";
    default: return "ZOMBIE";
    }
}
