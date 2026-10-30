#include <chrono>
#include <thread>
#include <time.h>
#include <exception>
#include <unistd.h>
#include "kernel/Clock.h"
#include "syscalls/Syscalls.h"

namespace std {
namespace chrono {
long long
__nanoseconds(int clock) {
    timespec now;
    clock_gettime(clock == 0 ? CLOCK_REALTIME : CLOCK_MONOTONIC, &now);
    return (long long)now.tv_sec * 1000000000ll + now.tv_nsec;
}
}

namespace this_thread {
void
yield() noexcept { Syscalls::yield(); }

void
__sleep(long long nanoseconds) {
    if (nanoseconds <= 0) {
        Syscalls::yield();
        return;
    }
    Syscalls::sleep((uint32_t)((nanoseconds + 999999) / 1000000));
}
}
static void *
threadEntry(void *argument) {
    __thread_task *task = (__thread_task *)argument;
    task->run();
    delete task;
    return nullptr;
}

void
thread::__start(__thread_task *task) {
    if (pthread_create(&handle, nullptr, threadEntry, task) != 0) {
        delete task;
        handle = 0;
        terminate();
    }
}

thread &
thread::operator=(thread &&other) noexcept {
    if (joinable()) {
        terminate();
    }
    handle = other.handle;
    other.handle = 0;
    return *this;
}

thread::~thread() {
    if (joinable()) {
        terminate();
    }
}

void
thread::join() {
    pthread_join(handle, nullptr);
    handle = 0;
}

void
thread::detach() {
    pthread_detach(handle);
    handle = 0;
}

unsigned
thread::hardware_concurrency() noexcept {
    long n = sysconf(_SC_NPROCESSORS_ONLN);
    return n > 0 ? (unsigned)n : 0;
}
}
