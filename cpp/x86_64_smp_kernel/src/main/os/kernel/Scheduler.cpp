#include "kernel/Scheduler.h"
#include "fs/Err.h"
#include "kernel/Clock.h"
#include "kernel/Consoles.h"
#include "drivers/tty/Tty.h"
#include "drivers/tty/VirtualConsoleTty.h"
#include "kernel/Idle.h"
#include "kernel/Interrupts.h"
#include "kernel/LocalApic.h"
#include "kernel/Panic.h"
#include "kernel/Smp.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "lib/vitralKernel/InterruptsOff.h"
#include "lib/vitralKernel/LockGuard.h"
#include "kernel/ArgList.h"

Scheduler::CpuState Scheduler::cpus[CpuMask::MAX];
unsigned Scheduler::cpuTotal = 0;
Process *Scheduler::table[Scheduler::MAX_PROCESSES];
uint32_t Scheduler::nextPid = 1;
bool Scheduler::running = false;
bool Scheduler::lapicTimers = false;
bool Scheduler::tickless = false;
SpinLock Scheduler::lock;
Process *Scheduler::futexBuckets[Scheduler::FUTEX_BUCKETS];
uint64_t Scheduler::earliestWake = 0;

unsigned
Scheduler::cpuIndex() { return Smp::currentIndex(); }

void
Scheduler::init(unsigned count) {
    cpuTotal = count < CpuMask::MAX ? count : CpuMask::MAX;
    for (unsigned i = 0; i < cpuTotal; i++) {
        cpus[i] = {nullptr, Process::makeIdle(i), nullptr, nullptr, 0, i == 0, nullptr, 0, 0, 0};
        cpus[i].current = cpus[i].idle;
        cpus[i].idle->setState(Process::State::Running);
    }
    for (auto &slot : table) {
        slot = nullptr;
    }
}

Process *
Scheduler::current() {
    InterruptsOff guard;
    return cpus[cpuIndex()].current;
}

unsigned
Scheduler::currentConsole() {
    return cpuTotal ? current()->console() : 0;
}

int
Scheduler::consoleOf(uint32_t pid) {
    LockGuard guard(lock);
    for (Process *p : table) {
        if (p && p->pid() == pid && p->state() != Process::State::Zombie) {
            return (int)p->console();
        }
    }
    return -1;
}

unsigned
Scheduler::chooseCpu(const Process &p) {
    for (int pass = 0; pass < 2; pass++) {
        unsigned best = 0;
        unsigned bestLoad = ~0u;
        for (unsigned i = 0; i < cpuTotal; i++) {
            if ((pass == 0 && !cpus[i].online) || !p.affinity().test(i)) {
                continue;
            }
            unsigned load = cpus[i].queued + (cpus[i].current != cpus[i].idle ? 1 : 0);
            if (load < bestLoad) { best = i; bestLoad = load; }
        }
        if (bestLoad != ~0u) {
            return best;
        }
    }
    Panic::halt("process affinity excludes every CPU");
}

void
Scheduler::enqueue(Process *p) {
    unsigned target = chooseCpu(*p);
    CpuState &cpu = cpus[target];
    p->setState(Process::State::Ready);
    p->scheduling.nextInQueue = nullptr;
    if (cpu.tail) {
        cpu.tail->scheduling.nextInQueue = p;
    } else {
        cpu.head = p;
    }
    cpu.tail = p;
    cpu.queued++;
    notifyIdle(target);
}

void
Scheduler::notifyIdle(unsigned target) {
    CpuState &cpu = cpus[target];
    if (!lapicTimers || !cpu.online || cpu.current != cpu.idle || target == cpuIndex()) {
        return;
    }
    LocalApic::sendVector(Smp::cpu(target).apicId, Interrupts::RESCHEDULE);
}

Process *
Scheduler::steal(unsigned thief) {
    for (;;) {
        unsigned victim = thief;
        unsigned longest = 0;
        for (unsigned i = 0; i < cpuTotal; i++) {
            if (i != thief && cpus[i].queued > longest) {
                victim = i;
                longest = cpus[i].queued;
            }
        }
        if (victim == thief) {
            return nullptr;
        }
        CpuState &v = cpus[victim];
        Process *before = nullptr;
        for (Process *p = v.head; p; before = p, p = p->scheduling.nextInQueue) {
            if (!p->affinity().test(thief)) {
                continue;
            }
            if (before) {
                before->scheduling.nextInQueue = p->scheduling.nextInQueue;
            } else {
                v.head = p->scheduling.nextInQueue;
            }
            if (v.tail == p) {
                v.tail = before;
            }
            p->scheduling.nextInQueue = nullptr;
            v.queued--;
            return p;
        }
        return nullptr;
    }
}

bool
Scheduler::setAffinity(uint32_t pid, const unsigned long *bits) {
    CpuMask mask = CpuMask::from(bits);
    bool usable = false;
    for (unsigned i = 0; i < cpuTotal; i++) {
        usable = usable || mask.test(i);
    }
    if (!usable) {
        return false;
    }
    LockGuard guard(lock);
    for (Process *p : table) {
        if (p && p->pid() == pid && p->state() != Process::State::Zombie) {
            p->setAffinity(mask);
            return true;
        }
    }
    return false;
}

unsigned
Scheduler::onlineCount() {
    unsigned n = 0;
    for (unsigned i = 0; i < cpuTotal; i++) {
        n += cpus[i].online ? 1 : 0;
    }
    return n ? n : 1;
}

unsigned
Scheduler::bucketOf(const int *address) {
    return (unsigned)((((uintptr_t)address >> 2) * 2654435761u) >> 8) % FUTEX_BUCKETS;
}

void
Scheduler::futexLink(Process *p) {
    Process **at = &futexBuckets[bucketOf(p->scheduling.waitAddress)];
    while (*at) {
        at = &(*at)->scheduling.waitNext;
    }
    p->scheduling.waitNext = nullptr;
    *at = p;
}

void
Scheduler::futexUnlink(Process *p) {
    Process **at = &futexBuckets[bucketOf(p->scheduling.waitAddress)];
    while (*at && *at != p) {
        at = &(*at)->scheduling.waitNext;
    }
    if (*at) {
        *at = p->scheduling.waitNext;
    }
    p->scheduling.waitNext = nullptr;
    p->scheduling.waitAddress = nullptr;
}

void
Scheduler::setDeadline(Process *p, uint64_t nanoseconds) {
    if (!nanoseconds) {
        p->scheduling.wakeNs = 0;
        return;
    }
    p->scheduling.wakeNs = Clock::nanoseconds() + nanoseconds;
    if (!earliestWake || p->scheduling.wakeNs < earliestWake) {
        earliestWake = p->scheduling.wakeNs;
    }
}

Process *
Scheduler::dequeue(CpuState &cpu) {
    Process *p = cpu.head;
    if (!p) {
        return nullptr;
    }
    cpu.head = p->scheduling.nextInQueue;
    if (!cpu.head) {
        cpu.tail = nullptr;
    }
    p->scheduling.nextInQueue = nullptr;
    cpu.queued--;
    return p;
}

Process *
Scheduler::spawn(const Program &program, const ArgList &args, uint32_t ppid,
                 const CpuMask &affinity, int inheritConsole, bool waitable, Tty *terminal) {
    LockGuard guard(lock);
    unsigned slot = 0;
    while (slot < MAX_PROCESSES && table[slot]) {
        slot++;
    }
    if (slot == MAX_PROCESSES) {
        return nullptr;
    }
    int console = inheritConsole >= 0 ? inheritConsole : Consoles::allocate(nextPid);
    if (console < 0) {
        return nullptr;
    }
    char *stack = Process::allocateStack(Process::STACK_SIZE);
    if (!stack) {
        if (inheritConsole < 0) {
            Consoles::release((unsigned)console);
        }
        return nullptr;
    }
    if (!terminal) {
        terminal = VirtualConsoleTty::of((unsigned)console);
    }
    Process *p = new Process(nextPid++, ppid, program, args, affinity, (unsigned)console, inheritConsole < 0, terminal,
                             stack);
    p->scheduling.waitable = waitable;
    table[slot] = p;
    enqueue(p);
    return p;
}

Process *
Scheduler::spawnThread(void (*entry)(void *), void *argument, size_t stackSize, Process *creator,
                       const CpuMask &affinity) {
    LockGuard guard(lock);
    unsigned slot = 0;
    while (slot < MAX_PROCESSES && table[slot]) {
        slot++;
    }
    if (slot == MAX_PROCESSES) {
        return nullptr;
    }
    char *stack = Process::allocateStack(stackSize);
    if (!stack) {
        return nullptr;
    }
    Process *p = new Process(nextPid++, creator, entry, argument, stack, stackSize, affinity);
    table[slot] = p;
    enqueue(p);
    return p;
}

TrapFrame *
Scheduler::switchFrom(unsigned index, TrapFrame *frame) {
    CpuState &cpu = cpus[index];
    reapZombie(cpu);
    Process *prev = cpu.current;
    uint64_t now = Clock::cycles();
    if (cpu.since && now > cpu.since) {
        (prev == cpu.idle ? cpu.idleCycles : cpu.busyCycles) += now - cpu.since;
    }
    cpu.since = now;
    prev->setTrapFrame(frame);
    Fpu::save(prev->fpuImage());
    if (prev->state() == Process::State::Running && prev != cpu.idle) {
        enqueue(prev);
    } else if (prev == cpu.idle) {
        prev->setState(Process::State::Ready);
    }

    if (prev->state() == Process::State::Zombie) {
        cpu.zombie = prev;
    }
    Process *next = dequeue(cpu);
    if (!next && lapicTimers) {
        next = steal(index);
    }
    if (!next) {
        next = cpu.idle;
    }
    next->setState(Process::State::Running);
    next->setCpu((int)index);
    cpu.current = next;
    Fpu::restore(next->fpuImage());
    if (lapicTimers) {
        armTimer(index, next);
    }
    return next->trapFrame();
}

void
Scheduler::armTimer(unsigned index, const Process *next) {
    const uint64_t sliceNs = 1000000000000ull / Clock::HZ_MILLI / TICKS_PER_PIT_TICK;
    if (!tickless || next != cpus[index].idle || cpus[index].zombie) {
        LocalApic::armOneShot(sliceNs);
    } else if (earliestWake) {
        uint64_t now = Clock::nanoseconds();
        LocalApic::armOneShot(earliestWake > now ? earliestWake - now : 1);
    } else {
        LocalApic::stopTimer();
    }
}

void
Scheduler::wakeSleepers(uint64_t now) {
    if (!earliestWake || now < earliestWake) {
        return;
    }
    uint64_t next = 0;
    for (Process *p : table) {
        if (!p) {
            continue;
        }
        bool sleeping = p->state() == Process::State::Sleeping;
        bool timed = p->state() == Process::State::Blocked && p->scheduling.wakeNs;
        if (!sleeping && !timed) {
            continue;
        }
        if (p->scheduling.wakeNs <= now) {
            if (timed) {
                p->trapFrame()->setResult((uint64_t)-Syscalls::ERR_TIMEDOUT);
                futexUnlink(p);
            }
            p->scheduling.wakeNs = 0;
            enqueue(p);
        } else if (!next || p->scheduling.wakeNs < next) {
            next = p->scheduling.wakeNs;
        }
    }
    earliestWake = next;
}

void
Scheduler::reapZombie(CpuState &cpu) {
    Process *z = cpu.zombie;
    if (!z) {
        return;
    }
    cpu.zombie = nullptr;
    if (z->scheduling.waitable) {
        z->releaseStack();
        z->scheduling.exited = true;
        childEnded(z);
        return;
    }
    for (Process *&slot : table) {
        if (slot == z) {
            slot = nullptr;
            break;
        }
    }
    delete z;
}

TrapFrame *
Scheduler::tick(TrapFrame *frame) {
    LockGuard guard(lock);
    unsigned index = cpuIndex();
    uint64_t now = Clock::nanoseconds();
    cpus[index].current->addTick();
    wakeSleepers(now);
    return switchFrom(index, frame);
}

TrapFrame *
Scheduler::yield(TrapFrame *frame) {
    LockGuard guard(lock);
    return switchFrom(cpuIndex(), frame);
}

TrapFrame *
Scheduler::sleep(TrapFrame *frame, uint64_t nanoseconds) {
    LockGuard guard(lock);
    unsigned index = cpuIndex();
    Process *p = cpus[index].current;
    setDeadline(p, nanoseconds ? nanoseconds : 1);
    p->setState(Process::State::Sleeping);
    return switchFrom(index, frame);
}

TrapFrame *
Scheduler::futexWait(TrapFrame *frame, int *address, int expected, uint64_t timeoutNs) {
    LockGuard guard(lock);
    if (__atomic_load_n(address, __ATOMIC_SEQ_CST) != expected) {
        frame->setResult((uint64_t)-Syscalls::ERR_AGAIN);
        return frame;
    }
    unsigned index = cpuIndex();
    Process *p = cpus[index].current;
    frame->setResult(0);
    p->scheduling.waitAddress = address;
    futexLink(p);
    setDeadline(p, timeoutNs);
    p->setState(Process::State::Blocked);
    return switchFrom(index, frame);
}

int
Scheduler::futexWake(int *address, int count) {
    LockGuard guard(lock);
    int woken = 0;
    Process *p = futexBuckets[bucketOf(address)];
    while (p && woken < count) {
        Process *next = p->scheduling.waitNext;
        if (p->scheduling.waitAddress == address) {
            futexUnlink(p);
            p->scheduling.wakeNs = 0;
            enqueue(p);
            woken++;
        }
        p = next;
    }
    return woken;
}

bool
Scheduler::collect(Process *child, Process *parent, int *status) {
    (void)parent;
    if (status) {
        *status = (child->exitCode() & 0xFF) << 8;
    }
    for (Process *&slot : table) {
        if (slot == child) {
            slot = nullptr;
            break;
        }
    }
    delete child;
    return true;
}

void
Scheduler::childEnded(Process *child) {
    for (Process *parent : table) {
        if (!parent || parent->pid() != child->ppid()) {
            continue;
        }
        if (parent->state() == Process::State::Blocked && parent->scheduling.waitingFor &&
            (parent->scheduling.waitingFor == Process::WAIT_ANY || parent->scheduling.waitingFor == child->pid())) {
            uint32_t pid = child->pid();
            collect(child, parent, parent->scheduling.waitStatus);
            parent->scheduling.waitingFor = 0;
            parent->scheduling.waitStatus = nullptr;
            parent->trapFrame()->setResult(pid);
            enqueue(parent);
        }
        return;
    }
}

TrapFrame *
Scheduler::wait(TrapFrame *frame, int64_t pid, int *status, bool noHang) {
    LockGuard guard(lock);
    unsigned index = cpuIndex();
    Process *self = cpus[index].current;
    bool candidates = false;
    for (Process *child : table) {
        if (!child || child->ppid() != self->pid() || !child->scheduling.waitable) {
            continue;
        }
        if (pid >= 0 && child->pid() != (uint32_t)pid) {
            continue;
        }
        candidates = true;
        if (child->scheduling.exited) {
            uint32_t done = child->pid();
            collect(child, self, status);
            frame->setResult(done);
            return frame;
        }
    }
    if (!candidates) {
        frame->setResult((uint64_t)-Err::CHILD);
        return frame;
    }
    if (noHang) {
        frame->setResult(0);
        return frame;
    }
    self->scheduling.waitingFor = pid >= 0 ? (uint32_t)pid : Process::WAIT_ANY;
    self->scheduling.waitStatus = status;
    self->scheduling.waitAddress = nullptr;
    self->scheduling.wakeNs = 0;
    self->setState(Process::State::Blocked);
    return switchFrom(index, frame);
}

TrapFrame *
Scheduler::exit(TrapFrame *frame, int code) {
    LockGuard guard(lock);
    unsigned index = cpuIndex();
    Process *p = cpus[index].current;
    if (p->pid() == 1) {
        Panic::halt("init (PID 1) exited");
    }
    p->setState(Process::State::Zombie);
    p->setExitCode(code);
    bool parentAlive = false;
    for (Process *&slot : table) {
        if (!slot) {
            continue;
        }
        if (slot->pid() == p->ppid() && slot->state() != Process::State::Zombie) {
            parentAlive = true;
        }
        if (slot->ppid() == p->pid() && slot->scheduling.waitable) {
            slot->scheduling.waitable = false;
            if (slot->scheduling.exited) {
                Process *dead = slot;
                slot = nullptr;
                delete dead;
            }
        }
    }
    if (!parentAlive) {
        p->scheduling.waitable = false;
    }
    p->returnScreen();
    if (p->ownsConsole() && code != 0) {
        Consoles::retire(p->console(), p->name(), code);
    } else if (p->ownsConsole()) {
        bool wasVisible = VirtualConsole::active() == p->console();
        Consoles::release(p->console());
        if (wasVisible) {
            for (Process *parent : table) {
                if (parent && parent->pid() == p->ppid() && parent->state() != Process::State::Zombie) {
                    Consoles::switchTo(parent->console());
                    break;
                }
            }
        }
    }
    return switchFrom(index, frame);
}

TrapFrame *
Scheduler::exec(TrapFrame *, const Program &program, const ArgList &args) {
    LockGuard guard(lock);
    Process *p = cpus[cpuIndex()].current;
    ArgList copy = args;
    p->setProgram(program, copy);
    p->setState(Process::State::Running);
    return p->trapFrame();
}

unsigned
Scheduler::snapshot(Syscalls::ProcessInfo *out, unsigned max) {
    LockGuard guard(lock);
    unsigned n = 0;
    for (Process *p : table) {
        if (!p || n >= max) {
            continue;
        }
        Syscalls::ProcessInfo &info = out[n++];
        info.pid = p->pid();
        info.ppid = p->ppid();
        String::copy(info.name, p->name(), sizeof(info.name));
        info.state = (uint8_t)p->state();
        info.cpu = p->cpu();
        info.console = (uint8_t)p->console();
        String::copy(info.terminal, p->terminal() ? p->terminal()->name() : "?", sizeof(info.terminal));
        info.ticks = lapicTimers ? p->ticks() / TICKS_PER_PIT_TICK : p->ticks();
        info.affinity = p->affinity().low64();
    }
    return n;
}

unsigned
Scheduler::cpuLoad(Syscalls::CpuLoadInfo *out, unsigned max) {
    LockGuard guard(lock);
    uint64_t now = Clock::cycles();
    for (unsigned i = 0; i < cpuTotal && i < max; i++) {
        const CpuState &cpu = cpus[i];
        out[i].busyCycles = cpu.busyCycles;
        out[i].idleCycles = cpu.idleCycles;
        if (cpu.since && now > cpu.since) {
            (cpu.current == cpu.idle ? out[i].idleCycles : out[i].busyCycles) += now - cpu.since;
        }
        out[i].online = cpu.online;
    }
    return cpuTotal;
}

void
Scheduler::useLapicTimers() {
    lapicTimers = true;
    tickless = Idle::canSleep();
    if (!Interrupts::registerHandler(Interrupts::LAPIC_TIMER, onLapicTimer)) {
        Panic::halt("local APIC timer vector already taken");
    }
    if (!Interrupts::registerHandler(Interrupts::RESCHEDULE, onReschedule)) {
        Panic::halt("reschedule IPI vector already taken");
    }
}

TrapFrame *
Scheduler::onReschedule(TrapFrame *frame) {
    LocalApic::eoi();
    LockGuard guard(lock);
    unsigned index = cpuIndex();
    if (cpus[index].current != cpus[index].idle) {
        return frame;
    }
    return switchFrom(index, frame);
}

TrapFrame *
Scheduler::onLapicTimer(TrapFrame *frame) {
    LocalApic::eoi();
    return tick(frame);
}

void
Scheduler::start() {
    cpus[cpuIndex()].since = Clock::cycles();
    running = true;
    Smp::releaseProcessors();
    if (lapicTimers) {
        LocalApic::initTimer();
        LocalApic::armOneShot(1000000000000ull / Clock::HZ_MILLI / TICKS_PER_PIT_TICK);
    }
    Idle::run();
}

void
Scheduler::startCpu() {
    cpus[cpuIndex()].online = true;
    cpus[cpuIndex()].since = Clock::cycles();
    LocalApic::initTimer();
    LocalApic::armOneShot(1000000000000ull / Clock::HZ_MILLI / TICKS_PER_PIT_TICK);
    Idle::run();
}
