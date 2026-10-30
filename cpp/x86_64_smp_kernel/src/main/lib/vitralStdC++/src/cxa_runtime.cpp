#include <exception>
#include <typeinfo>
#include <stdlib.h>
#include <stdio.h>
#include <limits.h>
#include "syscalls/Syscalls.h"

namespace __cxxabiv1 {
void *
derivedFrom(const std::type_info &type, const void *object, const std::type_info &target);
}

static const size_t EXCEPTION_HEADER = 32;

void *
allocateException(size_t size) asm("__cxa_allocate_exception");
void
freeException(void *object) asm("__cxa_free_exception");
void
throwException(void *object, std::type_info *type, void (*destructor)(void *)) asm("__cxa_throw");
void *
beginCatch(void *object) asm("__cxa_begin_catch");
void
endCatch() asm("__cxa_end_catch");
void
rethrowException() asm("__cxa_rethrow");
void *
exceptionPointer(void *object) asm("__cxa_get_exception_ptr");
int
personality(int, int, unsigned long, void *, void *) asm("__gxx_personality_v0");
void
resumeUnwind(void *) asm("_Unwind_Resume");
void
pureVirtual() asm("__cxa_pure_virtual");
int
registerAtExit(void (*function)(void *), void *argument, void *handle) asm("__cxa_atexit");
int
guardAcquire(long long *guard) asm("__cxa_guard_acquire");
void
guardRelease(long long *guard) asm("__cxa_guard_release");
void
guardAbort(long long *guard) asm("__cxa_guard_abort");
void
badCast() asm("__cxa_bad_cast");
void
badTypeid() asm("__cxa_bad_typeid");
void *dsoHandle asm("__dso_handle") = nullptr;

void *
allocateException(size_t size) {
    void *block = malloc(size + EXCEPTION_HEADER);
    if (!block) {
        fputs("terminate called: out of memory throwing an exception\n", stderr);
        abort();
    }
    return (char *)block + EXCEPTION_HEADER;
}

void
freeException(void *object) { free((char *)object - EXCEPTION_HEADER); }

void
throwException(void *object, std::type_info *type, void (*)(void *)) {
    fprintf(stderr, "terminate called after throwing an instance of '%s'\n", type->name());
    const std::exception *base = (const std::exception *)__cxxabiv1::derivedFrom(*type, object, typeid(std::exception));
    if (base) {
        fprintf(stderr, "  what():  %s\n", base->what());
    }
    abort();
}

void *
beginCatch(void *object) { return object; }
void
endCatch() {}

void
rethrowException() {
    fputs("terminate called: rethrow\n", stderr);
    abort();
}

void *
exceptionPointer(void *object) { return object; }
int
personality(int, int, unsigned long, void *, void *) { return 0; }

void
resumeUnwind(void *) {
    fputs("terminate called: unwinding is not supported\n", stderr);
    abort();
}

void
pureVirtual() {
    fputs("pure virtual method called\n", stderr);
    abort();
}

int
registerAtExit(void (*)(void *), void *, void *) { return 0; }

int
guardAcquire(long long *guard) {
    char *state = (char *)guard;
    int *word = (int *)(state + 4);
    for (;;) {
        if (__atomic_load_n(state, __ATOMIC_ACQUIRE)) {
            return 0;
        }
        int expected = 0;
        if (__atomic_compare_exchange_n(word, &expected, 1, false, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST)) {
            return 1;
        }
        Syscalls::futexWait(word, 1, 0);
    }
}

void
guardRelease(long long *guard) {
    char *state = (char *)guard;
    int *word = (int *)(state + 4);
    __atomic_store_n(state, 1, __ATOMIC_RELEASE);
    __atomic_store_n(word, 0, __ATOMIC_SEQ_CST);
    Syscalls::futexWake(word, INT_MAX);
}

void
guardAbort(long long *guard) {
    int *word = (int *)((char *)guard + 4);
    __atomic_store_n(word, 0, __ATOMIC_SEQ_CST);
    Syscalls::futexWake(word, INT_MAX);
}

void
badCast() { throw std::bad_cast(); }
void
badTypeid() { throw std::bad_typeid(); }
