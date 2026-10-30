#ifndef __SEMAPHORE__
#define __SEMAPHORE__
#include <time.h>
struct sem_t {
    int value;
    int waiters;
};
#define SEM_VALUE_MAX 2147483647
int
sem_init(sem_t *semaphore, int shared, unsigned value);
int
sem_destroy(sem_t *semaphore);
int
sem_wait(sem_t *semaphore);
int
sem_trywait(sem_t *semaphore);
int
sem_timedwait(sem_t *semaphore, const timespec *deadline);
int
sem_post(sem_t *semaphore);
int
sem_getvalue(sem_t *semaphore, int *value);

#endif
