#ifndef __SCHED__
#define __SCHED__
typedef struct {
    unsigned long bits[4];
} cpu_set_t;
#define CPU_SETSIZE 256
#define CPU_ZERO(set)                                             \
    do {                                                          \
        for (int cpuSetIndex = 0; cpuSetIndex < 4; cpuSetIndex++) \
            (set)->bits[cpuSetIndex] = 0;                         \
    } while (0)
#define CPU_SET(cpu, set) ((set)->bits[(cpu) / 64] |= 1ul << ((cpu) % 64))
#define CPU_CLR(cpu, set) ((set)->bits[(cpu) / 64] &= ~(1ul << ((cpu) % 64)))
#define CPU_ISSET(cpu, set) (((set)->bits[(cpu) / 64] >> ((cpu) % 64)) & 1ul)
int
sched_yield();
int
sched_getcpu();

#endif
