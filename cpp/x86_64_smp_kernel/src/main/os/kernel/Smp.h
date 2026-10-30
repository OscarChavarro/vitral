#ifndef __SMP__
#define __SMP__
#include <stdint.h>
#include <stddef.h>

class Smp {
public:
    static constexpr unsigned MAX_CPUS = 256;

    enum class State : uint8_t {
        Detected,
        Online,
    };

    class LogicalCpu {
    public:
        uint32_t apicId;
        uint16_t package;
        uint16_t core;
        uint16_t thread;
        State state;
        bool bsp;
        void *process;
    };

    static void detect();
    static void report();

    static void startProcessors();
    static void releaseProcessors() { released = true; }
    static unsigned onlineCount();

    static bool isSmp() { return count > 1; }
    static unsigned logicalCount() { return count; }
    static unsigned coreCount() { return cores; }
    static unsigned threadsPerCore() { return count / cores; }
    static LogicalCpu &cpu(unsigned i) { return table[i]; }
    static unsigned currentIndex();

private:
    static constexpr uintptr_t TRAMPOLINE = 0x8000;
    static constexpr size_t AP_STACK_SIZE = 16 * 1024;

    static void apMain();
    static bool startProcessor(unsigned index);
    static void wait(uint64_t ticks);

    static volatile bool apReady;
    static volatile bool released;
    static uint8_t indexByApicId[256];
    static LogicalCpu table[MAX_CPUS];
    static unsigned count;
    static unsigned cores;
    static bool topologyKnown;
};

#endif
