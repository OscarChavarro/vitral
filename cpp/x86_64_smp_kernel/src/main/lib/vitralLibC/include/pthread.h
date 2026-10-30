#ifndef __PTHREAD__
#define __PTHREAD__
#include <stddef.h>
#include <stdint.h>
#include <sched.h>
#include <time.h>
typedef unsigned long pthread_t;
typedef unsigned int pthread_key_t;
struct pthread_attr_t {
    size_t stackSize;
    int detachState;
    cpu_set_t affinity;
    int hasAffinity;
};
struct pthread_mutexattr_t {
    int type;
};
struct pthread_condattr_t {
    int clock;
};
struct pthread_mutex_t {
    int state;
    int type;
    int count;
    unsigned long owner;
};
struct pthread_cond_t {
    int sequence;
    int waiters;
};
struct pthread_once_t {
    int state;
};
struct pthread_rwlock_t {
    int state;
    int writersWaiting;
};
struct pthread_rwlockattr_t {
    int unused;
};
struct pthread_barrier_t {
    int count;
    int arrived;
    int generation;
};
struct pthread_barrierattr_t {
    int unused;
};
typedef int pthread_spinlock_t;
#define PTHREAD_MUTEX_INITIALIZER {0, 0, 0, 0}
#define PTHREAD_COND_INITIALIZER {0, 0}
#define PTHREAD_ONCE_INIT {0}
#define PTHREAD_RWLOCK_INITIALIZER {0, 0}
#define PTHREAD_BARRIER_SERIAL_THREAD (-1)
#define PTHREAD_PROCESS_PRIVATE 0
#define PTHREAD_PROCESS_SHARED 1
#define PTHREAD_CREATE_JOINABLE 0
#define PTHREAD_CREATE_DETACHED 1
#define PTHREAD_MUTEX_NORMAL 0
#define PTHREAD_MUTEX_RECURSIVE 1
#define PTHREAD_MUTEX_ERRORCHECK 2
#define PTHREAD_MUTEX_DEFAULT PTHREAD_MUTEX_NORMAL
int
pthread_create(pthread_t *thread, const pthread_attr_t *attributes, void *(*entry)(void *),
               void *argument);
int
pthread_join(pthread_t thread, void **result);
int
pthread_detach(pthread_t thread);
[[noreturn]] void
pthread_exit(void *result);
pthread_t
pthread_self();
int
pthread_equal(pthread_t a, pthread_t b);
int
pthread_yield();
int
pthread_attr_init(pthread_attr_t *attributes);
int
pthread_attr_destroy(pthread_attr_t *attributes);
int
pthread_attr_setstacksize(pthread_attr_t *attributes, size_t size);
int
pthread_attr_getstacksize(const pthread_attr_t *attributes, size_t *size);
int
pthread_attr_setdetachstate(pthread_attr_t *attributes, int state);
int
pthread_attr_getdetachstate(const pthread_attr_t *attributes, int *state);
int
pthread_mutexattr_init(pthread_mutexattr_t *attributes);
int
pthread_mutexattr_destroy(pthread_mutexattr_t *attributes);
int
pthread_mutexattr_settype(pthread_mutexattr_t *attributes, int type);
int
pthread_mutexattr_gettype(const pthread_mutexattr_t *attributes, int *type);
int
pthread_mutex_init(pthread_mutex_t *mutex, const pthread_mutexattr_t *attributes);
int
pthread_mutex_destroy(pthread_mutex_t *mutex);
int
pthread_mutex_lock(pthread_mutex_t *mutex);
int
pthread_mutex_trylock(pthread_mutex_t *mutex);
int
pthread_mutex_unlock(pthread_mutex_t *mutex);
int
pthread_mutex_timedlock(pthread_mutex_t *mutex, const timespec *deadline);
int
pthread_condattr_init(pthread_condattr_t *attributes);
int
pthread_condattr_destroy(pthread_condattr_t *attributes);
int
pthread_cond_init(pthread_cond_t *condition, const pthread_condattr_t *attributes);
int
pthread_cond_destroy(pthread_cond_t *condition);
int
pthread_cond_wait(pthread_cond_t *condition, pthread_mutex_t *mutex);
int
pthread_cond_timedwait(pthread_cond_t *condition, pthread_mutex_t *mutex,
                       const timespec *deadline);
int
pthread_cond_signal(pthread_cond_t *condition);
int
pthread_cond_broadcast(pthread_cond_t *condition);
int
pthread_rwlock_init(pthread_rwlock_t *lock, const pthread_rwlockattr_t *attributes);
int
pthread_rwlock_destroy(pthread_rwlock_t *lock);
int
pthread_rwlock_rdlock(pthread_rwlock_t *lock);
int
pthread_rwlock_tryrdlock(pthread_rwlock_t *lock);
int
pthread_rwlock_wrlock(pthread_rwlock_t *lock);
int
pthread_rwlock_trywrlock(pthread_rwlock_t *lock);
int
pthread_rwlock_unlock(pthread_rwlock_t *lock);
int
pthread_barrier_init(pthread_barrier_t *barrier, const pthread_barrierattr_t *attributes, unsigned count);
int
pthread_barrier_destroy(pthread_barrier_t *barrier);
int
pthread_barrier_wait(pthread_barrier_t *barrier);
int
pthread_spin_init(pthread_spinlock_t *lock, int shared);
int
pthread_spin_destroy(pthread_spinlock_t *lock);
int
pthread_spin_lock(pthread_spinlock_t *lock);
int
pthread_spin_trylock(pthread_spinlock_t *lock);
int
pthread_spin_unlock(pthread_spinlock_t *lock);
int
pthread_attr_setaffinity_np(pthread_attr_t *attributes, size_t size, const cpu_set_t *set);
int
pthread_setaffinity_np(pthread_t thread, size_t size, const cpu_set_t *set);
int
pthread_getaffinity_np(pthread_t thread, size_t size, cpu_set_t *set);
int
pthread_setname_np(pthread_t thread, const char *name);
int
pthread_once(pthread_once_t *once, void (*routine)());
int
pthread_key_create(pthread_key_t *key, void (*destructor)(void *));
int
pthread_key_delete(pthread_key_t key);
void *
pthread_getspecific(pthread_key_t key);
int
pthread_setspecific(pthread_key_t key, const void *value);

#endif
