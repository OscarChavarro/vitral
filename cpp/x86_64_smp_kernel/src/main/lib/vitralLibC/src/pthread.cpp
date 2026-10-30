#include <pthread.h>
#include <semaphore.h>
#include <errno.h>
#include <limits.h>
#include <stdlib.h>
#include "kernel/Clock.h"
#include "kernel/Scheduler.h"
#include "syscalls/Syscalls.h"
#include "lib/vitralKernel/LockGuard.h"

struct ThreadRecord {
    Process *process;
    void *(*entry)(void *);
    void *argument;
    void *result;
    int finished;
    int detached;
};

static SpinLock threadLock;

static bool
compareAndSwap(int *word, int expected, int desired) {
    return __atomic_compare_exchange_n(word, &expected, desired, false, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
}

static void (*keyDestructors[Process::KEY_COUNT])(void *);

static void
runKeyDestructors() {
    void **values = Scheduler::current()->specific();
    for (int round = 0; round < 4; round++) {
        bool any = false;
        for (unsigned i = 0; i < Process::KEY_COUNT; i++) {
            void *value = values[i];
            if (value && keyDestructors[i]) {
                values[i] = nullptr;
                keyDestructors[i](value);
                any = true;
            }
        }
        if (!any) {
            return;
        }
    }
}

static void
finishThread(ThreadRecord *record, void *result) {
    runKeyDestructors();
    bool release = false;
    {
        LockGuard guard(threadLock);
        record->result = result;
        if (record->detached) {
            release = true;
        } else {
            __atomic_store_n(&record->finished, 1, __ATOMIC_SEQ_CST);
        }
    }
    if (release) {
        free(record);
    } else {
        Syscalls::futexWake(&record->finished, INT_MAX);
    }
}

static void
threadMain(void *argument) {
    ThreadRecord *record = (ThreadRecord *)argument;
    record->process = Scheduler::current();
    Scheduler::current()->setThreadRecord(record);
    finishThread(record, record->entry(record->argument));
}

int
pthread_create(pthread_t *thread, const pthread_attr_t *attributes, void *(*entry)(void *), void *argument) {
    ThreadRecord *record = (ThreadRecord *)malloc(sizeof(ThreadRecord));
    if (!record) {
        return EAGAIN;
    }
    record->process = nullptr;
    record->entry = entry;
    record->argument = argument;
    record->result = nullptr;
    record->finished = 0;
    record->detached = attributes && attributes->detachState == PTHREAD_CREATE_DETACHED;
    size_t stackSize = attributes && attributes->stackSize ? attributes->stackSize : 0;
    *thread = (pthread_t)record;
    int64_t pid = Syscalls::createThread(threadMain, record, stackSize);
    if (pid < 0) {
        free(record);
        return EAGAIN;
    }
    if (attributes && attributes->hasAffinity) {
        Scheduler::setAffinity((uint32_t)pid, attributes->affinity.bits);
    }
    return 0;
}

int
pthread_join(pthread_t thread, void **result) {
    ThreadRecord *record = (ThreadRecord *)thread;
    if (!record) {
        return ESRCH;
    }
    while (__atomic_load_n(&record->finished, __ATOMIC_SEQ_CST) == 0) {
        Syscalls::futexWait(&record->finished, 0, 0);
    }
    if (result) {
        *result = record->result;
    }
    free(record);
    return 0;
}

int
pthread_detach(pthread_t thread) {
    ThreadRecord *record = (ThreadRecord *)thread;
    bool release = false;
    {
        LockGuard guard(threadLock);
        if (__atomic_load_n(&record->finished, __ATOMIC_SEQ_CST)) {
            release = true;
        } else {
            record->detached = 1;
        }
    }
    if (release) {
        free(record);
    }
    return 0;
}

void
pthread_exit(void *result) {
    ThreadRecord *record = (ThreadRecord *)Scheduler::current()->threadRecord();
    if (record) {
        finishThread(record, result);
    }
    Syscalls::exit(0);
}

pthread_t
pthread_self() {
    Process *self = Scheduler::current();
    void *record = self->threadRecord();
    return record ? (pthread_t)record : (pthread_t)self->pid();
}

int
pthread_equal(pthread_t a, pthread_t b) { return a == b; }
int
pthread_yield() {
    Syscalls::yield();
    return 0;
}
int
sched_yield() {
    Syscalls::yield();
    return 0;
}

int
pthread_attr_init(pthread_attr_t *attributes) {
    attributes->stackSize = 0;
    attributes->detachState = PTHREAD_CREATE_JOINABLE;
    attributes->hasAffinity = 0;
    return 0;
}

int
pthread_attr_destroy(pthread_attr_t *) { return 0; }

int
pthread_attr_setstacksize(pthread_attr_t *attributes, size_t size) {
    if (size < 16384) {
        return EINVAL;
    }
    attributes->stackSize = size;
    return 0;
}

int
pthread_attr_getstacksize(const pthread_attr_t *attributes, size_t *size) {
    *size = attributes->stackSize ? attributes->stackSize : Process::THREAD_STACK_SIZE;
    return 0;
}

int
pthread_attr_setdetachstate(pthread_attr_t *attributes, int state) {
    attributes->detachState = state;
    return 0;
}

int
pthread_attr_getdetachstate(const pthread_attr_t *attributes, int *state) {
    *state = attributes->detachState;
    return 0;
}

int
pthread_mutexattr_init(pthread_mutexattr_t *attributes) {
    attributes->type = PTHREAD_MUTEX_NORMAL;
    return 0;
}

int
pthread_mutexattr_destroy(pthread_mutexattr_t *) { return 0; }

int
pthread_mutexattr_settype(pthread_mutexattr_t *attributes, int type) {
    attributes->type = type;
    return 0;
}

int
pthread_mutexattr_gettype(const pthread_mutexattr_t *attributes, int *type) {
    *type = attributes->type;
    return 0;
}

int
pthread_mutex_init(pthread_mutex_t *mutex, const pthread_mutexattr_t *attributes) {
    mutex->state = 0;
    mutex->type = attributes ? attributes->type : PTHREAD_MUTEX_NORMAL;
    mutex->count = 0;
    mutex->owner = 0;
    return 0;
}

int
pthread_mutex_destroy(pthread_mutex_t *) { return 0; }

static void
acquire(pthread_mutex_t *mutex) {
    if (compareAndSwap(&mutex->state, 0, 1)) {
        return;
    }
    if (Scheduler::onlineCount() > 1) {
        for (int i = 0; i < 200; i++) {
            if (__atomic_load_n(&mutex->state, __ATOMIC_RELAXED) == 0 && compareAndSwap(&mutex->state, 0, 1)) {
                return;
            }
            asm volatile("pause");
        }
    }
    while (__atomic_exchange_n(&mutex->state, 2, __ATOMIC_SEQ_CST) != 0) {
        Syscalls::futexWait(&mutex->state, 2, 0);
    }
}

int
pthread_mutex_lock(pthread_mutex_t *mutex) {
    pthread_t self = pthread_self();
    if (mutex->type == PTHREAD_MUTEX_RECURSIVE && mutex->owner == self && mutex->count > 0) {
        mutex->count++;
        return 0;
    }
    if (mutex->type == PTHREAD_MUTEX_ERRORCHECK && mutex->owner == self && mutex->count > 0) {
        return EDEADLK;
    }
    acquire(mutex);
    mutex->owner = self;
    mutex->count = 1;
    return 0;
}

int
pthread_mutex_trylock(pthread_mutex_t *mutex) {
    pthread_t self = pthread_self();
    if (mutex->type == PTHREAD_MUTEX_RECURSIVE && mutex->owner == self && mutex->count > 0) {
        mutex->count++;
        return 0;
    }
    if (!compareAndSwap(&mutex->state, 0, 1)) {
        return EBUSY;
    }
    mutex->owner = self;
    mutex->count = 1;
    return 0;
}

int
pthread_mutex_unlock(pthread_mutex_t *mutex) {
    if (mutex->type != PTHREAD_MUTEX_NORMAL && (mutex->owner != pthread_self() || mutex->count == 0)) {
        return EPERM;
    }
    if (--mutex->count > 0) {
        return 0;
    }
    mutex->owner = 0;
    if (__atomic_exchange_n(&mutex->state, 0, __ATOMIC_SEQ_CST) == 2) {
        Syscalls::futexWake(&mutex->state, 1);
    }
    return 0;
}

static long long
millisecondsUntil(const timespec *deadline) {
    timespec now;
    clock_gettime(CLOCK_REALTIME, &now);
    long long remaining = ((long long)deadline->tv_sec - now.tv_sec) * 1000000000ll + (deadline->tv_nsec - now.tv_nsec);
    return remaining <= 0 ? 0 : (remaining + 999999) / 1000000;
}

int
pthread_mutex_timedlock(pthread_mutex_t *mutex, const timespec *deadline) {
    pthread_t self = pthread_self();
    if (mutex->type == PTHREAD_MUTEX_RECURSIVE && mutex->owner == self && mutex->count > 0) {
        mutex->count++;
        return 0;
    }
    for (;;) {
        if (compareAndSwap(&mutex->state, 0, 1) || __atomic_exchange_n(&mutex->state, 2, __ATOMIC_SEQ_CST) == 0) {
            break;
        }
        long long ms = millisecondsUntil(deadline);
        if (ms == 0) {
            return ETIMEDOUT;
        }
        Syscalls::futexWait(&mutex->state, 2, ms > 0x7fffffff ? 0x7fffffff : (uint32_t)ms);
    }
    mutex->owner = self;
    mutex->count = 1;
    return 0;
}

int
pthread_condattr_init(pthread_condattr_t *attributes) {
    attributes->clock = CLOCK_REALTIME;
    return 0;
}

int
pthread_condattr_destroy(pthread_condattr_t *) { return 0; }

int
pthread_cond_init(pthread_cond_t *condition, const pthread_condattr_t *) {
    condition->sequence = 0;
    condition->waiters = 0;
    return 0;
}

int
pthread_cond_destroy(pthread_cond_t *) { return 0; }

static int
waitOn(pthread_cond_t *condition, pthread_mutex_t *mutex, uint32_t timeoutMs) {
    int sequence = __atomic_load_n(&condition->sequence, __ATOMIC_SEQ_CST);
    int count = mutex->count;
    mutex->count = 1;
    pthread_mutex_unlock(mutex);
    int result = Syscalls::futexWait(&condition->sequence, sequence, timeoutMs);
    pthread_mutex_lock(mutex);
    mutex->count = count;
    return result == -(int)Syscalls::ERR_TIMEDOUT ? ETIMEDOUT : 0;
}

int
pthread_cond_wait(pthread_cond_t *condition, pthread_mutex_t *mutex) { return waitOn(condition, mutex, 0); }

int
pthread_cond_timedwait(pthread_cond_t *condition, pthread_mutex_t *mutex, const timespec *deadline) {
    timespec now;
    clock_gettime(CLOCK_REALTIME, &now);
    long long remaining = ((long long)deadline->tv_sec - now.tv_sec) * 1000000000ll + (deadline->tv_nsec - now.tv_nsec);
    if (remaining <= 0) {
        return ETIMEDOUT;
    }
    long long ms = (remaining + 999999) / 1000000;
    return waitOn(condition, mutex, ms > 0x7fffffff ? 0x7fffffff : (uint32_t)ms);
}

int
pthread_cond_signal(pthread_cond_t *condition) {
    __atomic_add_fetch(&condition->sequence, 1, __ATOMIC_SEQ_CST);
    Syscalls::futexWake(&condition->sequence, 1);
    return 0;
}

int
pthread_cond_broadcast(pthread_cond_t *condition) {
    __atomic_add_fetch(&condition->sequence, 1, __ATOMIC_SEQ_CST);
    Syscalls::futexWake(&condition->sequence, INT_MAX);
    return 0;
}

int
pthread_once(pthread_once_t *once, void (*routine)()) {
    if (__atomic_load_n(&once->state, __ATOMIC_SEQ_CST) == 2) {
        return 0;
    }
    if (compareAndSwap(&once->state, 0, 1)) {
        routine();
        __atomic_store_n(&once->state, 2, __ATOMIC_SEQ_CST);
        Syscalls::futexWake(&once->state, INT_MAX);
        return 0;
    }
    while (__atomic_load_n(&once->state, __ATOMIC_SEQ_CST) != 2) {
        Syscalls::futexWait(&once->state, 1, 0);
    }
    return 0;
}

static bool keyUsed[Process::KEY_COUNT];

int
pthread_key_create(pthread_key_t *key, void (*destructor)(void *)) {
    LockGuard guard(threadLock);
    for (unsigned i = 0; i < Process::KEY_COUNT; i++) {
        if (!keyUsed[i]) {
            keyUsed[i] = true;
            keyDestructors[i] = destructor;
            *key = i;
            return 0;
        }
    }
    return EAGAIN;
}

int
pthread_key_delete(pthread_key_t key) {
    if (key >= Process::KEY_COUNT) {
        return EINVAL;
    }
    keyUsed[key] = false;
    keyDestructors[key] = nullptr;
    return 0;
}

void *
pthread_getspecific(pthread_key_t key) {
    return key < Process::KEY_COUNT ? Scheduler::current()->specific()[key] : nullptr;
}

int
pthread_setspecific(pthread_key_t key, const void *value) {
    if (key >= Process::KEY_COUNT) {
        return EINVAL;
    }
    Scheduler::current()->specific()[key] = (void *)value;
    return 0;
}

int
pthread_rwlock_init(pthread_rwlock_t *lock, const pthread_rwlockattr_t *) {
    lock->state = 0;
    lock->writersWaiting = 0;
    return 0;
}

int
pthread_rwlock_destroy(pthread_rwlock_t *) { return 0; }

int
pthread_rwlock_tryrdlock(pthread_rwlock_t *lock) {
    int s = __atomic_load_n(&lock->state, __ATOMIC_SEQ_CST);
    if (s < 0 || __atomic_load_n(&lock->writersWaiting, __ATOMIC_SEQ_CST) > 0) {
        return EBUSY;
    }
    return compareAndSwap(&lock->state, s, s + 1) ? 0 : EBUSY;
}

int
pthread_rwlock_rdlock(pthread_rwlock_t *lock) {
    for (;;) {
        int s = __atomic_load_n(&lock->state, __ATOMIC_SEQ_CST);
        if (s >= 0 && __atomic_load_n(&lock->writersWaiting, __ATOMIC_SEQ_CST) == 0) {
            if (compareAndSwap(&lock->state, s, s + 1)) {
                return 0;
            }
        } else {
            Syscalls::futexWait(&lock->state, s, 0);
        }
    }
}

int
pthread_rwlock_trywrlock(pthread_rwlock_t *lock) { return compareAndSwap(&lock->state, 0, -1) ? 0 : EBUSY; }

int
pthread_rwlock_wrlock(pthread_rwlock_t *lock) {
    if (compareAndSwap(&lock->state, 0, -1)) {
        return 0;
    }
    __atomic_add_fetch(&lock->writersWaiting, 1, __ATOMIC_SEQ_CST);
    for (;;) {
        int s = __atomic_load_n(&lock->state, __ATOMIC_SEQ_CST);
        if (s == 0 && compareAndSwap(&lock->state, 0, -1)) {
            break;
        }
        if (s != 0) {
            Syscalls::futexWait(&lock->state, s, 0);
        }
    }
    __atomic_sub_fetch(&lock->writersWaiting, 1, __ATOMIC_SEQ_CST);
    return 0;
}

int
pthread_rwlock_unlock(pthread_rwlock_t *lock) {
    int s = __atomic_load_n(&lock->state, __ATOMIC_SEQ_CST);
    if (s == 0) {
        return EPERM;
    }
    if (s < 0) {
        __atomic_store_n(&lock->state, 0, __ATOMIC_SEQ_CST);
        Syscalls::futexWake(&lock->state, INT_MAX);
    } else if (__atomic_sub_fetch(&lock->state, 1, __ATOMIC_SEQ_CST) == 0) {
        Syscalls::futexWake(&lock->state, INT_MAX);
    }
    return 0;
}

int
pthread_barrier_init(pthread_barrier_t *barrier, const pthread_barrierattr_t *, unsigned count) {
    if (count == 0) {
        return EINVAL;
    }
    barrier->count = (int)count;
    barrier->arrived = 0;
    barrier->generation = 0;
    return 0;
}

int
pthread_barrier_destroy(pthread_barrier_t *) { return 0; }

int
pthread_barrier_wait(pthread_barrier_t *barrier) {
    int generation = __atomic_load_n(&barrier->generation, __ATOMIC_SEQ_CST);
    if (__atomic_add_fetch(&barrier->arrived, 1, __ATOMIC_SEQ_CST) == barrier->count) {
        __atomic_store_n(&barrier->arrived, 0, __ATOMIC_SEQ_CST);
        __atomic_add_fetch(&barrier->generation, 1, __ATOMIC_SEQ_CST);
        Syscalls::futexWake(&barrier->generation, INT_MAX);
        return PTHREAD_BARRIER_SERIAL_THREAD;
    }
    while (__atomic_load_n(&barrier->generation, __ATOMIC_SEQ_CST) == generation) {
        Syscalls::futexWait(&barrier->generation, generation, 0);
    }
    return 0;
}

int
pthread_spin_init(pthread_spinlock_t *lock, int) {
    *lock = 0;
    return 0;
}

int
pthread_spin_destroy(pthread_spinlock_t *) { return 0; }

int
pthread_spin_trylock(pthread_spinlock_t *lock) {
    return __atomic_exchange_n(lock, 1, __ATOMIC_ACQUIRE) == 0 ? 0 : EBUSY;
}

int
pthread_spin_lock(pthread_spinlock_t *lock) {
    for (unsigned spins = 0; __atomic_exchange_n(lock, 1, __ATOMIC_ACQUIRE) != 0;) {
        while (__atomic_load_n(lock, __ATOMIC_RELAXED)) {
            asm volatile("pause");
            if (++spins % 2000 == 0) {
                Syscalls::yield();
            }
        }
    }
    return 0;
}

int
pthread_spin_unlock(pthread_spinlock_t *lock) {
    __atomic_store_n(lock, 0, __ATOMIC_RELEASE);
    return 0;
}

int
sem_init(sem_t *semaphore, int, unsigned value) {
    if (value > (unsigned)SEM_VALUE_MAX) {
        errno = EINVAL;
        return -1;
    }
    semaphore->value = (int)value;
    semaphore->waiters = 0;
    return 0;
}

int
sem_destroy(sem_t *) { return 0; }

int
sem_trywait(sem_t *semaphore) {
    int v = __atomic_load_n(&semaphore->value, __ATOMIC_SEQ_CST);
    while (v > 0) {
        if (__atomic_compare_exchange_n(&semaphore->value, &v, v - 1, false, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST)) {
            return 0;
        }
    }
    errno = EAGAIN;
    return -1;
}

static int
semaphoreWait(sem_t *semaphore, const timespec *deadline) {
    for (;;) {
        int v = __atomic_load_n(&semaphore->value, __ATOMIC_SEQ_CST);
        if (v > 0) {
            if (__atomic_compare_exchange_n(&semaphore->value, &v, v - 1, false, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST)) {
                return 0;
            }
            continue;
        }
        uint32_t timeout = 0;
        if (deadline) {
            long long ms = millisecondsUntil(deadline);
            if (ms == 0) {
                errno = ETIMEDOUT;
                return -1;
            }
            timeout = ms > 0x7fffffff ? 0x7fffffff : (uint32_t)ms;
        }
        __atomic_add_fetch(&semaphore->waiters, 1, __ATOMIC_SEQ_CST);
        Syscalls::futexWait(&semaphore->value, 0, timeout);
        __atomic_sub_fetch(&semaphore->waiters, 1, __ATOMIC_SEQ_CST);
    }
}

int
sem_wait(sem_t *semaphore) { return semaphoreWait(semaphore, nullptr); }
int
sem_timedwait(sem_t *semaphore, const timespec *deadline) { return semaphoreWait(semaphore, deadline); }

int
sem_post(sem_t *semaphore) {
    __atomic_add_fetch(&semaphore->value, 1, __ATOMIC_SEQ_CST);
    if (__atomic_load_n(&semaphore->waiters, __ATOMIC_SEQ_CST) > 0) {
        Syscalls::futexWake(&semaphore->value, 1);
    }
    return 0;
}

int
sem_getvalue(sem_t *semaphore, int *value) {
    *value = __atomic_load_n(&semaphore->value, __ATOMIC_SEQ_CST);
    return 0;
}

int
sched_getcpu() { return Scheduler::current()->cpu(); }

int
pthread_attr_setaffinity_np(pthread_attr_t *attributes, size_t size, const cpu_set_t *set) {
    if (size < sizeof(cpu_set_t)) {
        return EINVAL;
    }
    attributes->affinity = *set;
    attributes->hasAffinity = 1;
    return 0;
}

static Process *
processOf(pthread_t thread) {
    ThreadRecord *record = (ThreadRecord *)thread;
    return thread < 65536 || !record ? Scheduler::current() : record->process;
}

int
pthread_setaffinity_np(pthread_t thread, size_t size, const cpu_set_t *set) {
    Process *p = processOf(thread);
    if (size < sizeof(cpu_set_t) || !p) {
        return EINVAL;
    }
    return Scheduler::setAffinity(p->pid(), set->bits) ? 0 : EINVAL;
}

int
pthread_getaffinity_np(pthread_t thread, size_t size, cpu_set_t *set) {
    Process *p = processOf(thread);
    if (size < sizeof(cpu_set_t) || !p) {
        return EINVAL;
    }
    p->affinity().copyTo(set->bits);
    return 0;
}

int
pthread_setname_np(pthread_t thread, const char *name) {
    Process *p = processOf(thread);
    if (!p) {
        return ESRCH;
    }
    p->setName(name);
    return 0;
}
