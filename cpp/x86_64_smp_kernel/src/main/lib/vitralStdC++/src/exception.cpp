#include <exception>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

namespace std {
exception::exception() noexcept {}
exception::exception(const exception &) noexcept {}
exception &
exception::operator=(const exception &) noexcept { return *this; }
exception::~exception() noexcept {}
const char *
exception::what() const noexcept { return "std::exception"; }

bad_exception::bad_exception() noexcept {}
bad_exception::~bad_exception() noexcept {}
const char *
bad_exception::what() const noexcept { return "std::bad_exception"; }

static void
defaultTerminate() {
    fputs("terminate called\n", stderr);
    abort();
}

static terminate_handler currentTerminate = defaultTerminate;

terminate_handler
set_terminate(terminate_handler handler) noexcept {
    terminate_handler old = currentTerminate;
    currentTerminate = handler ? handler : defaultTerminate;
    return old;
}

terminate_handler
get_terminate() noexcept { return currentTerminate; }

void
terminate() noexcept {
    currentTerminate();
    abort();
}

bool
uncaught_exception() noexcept { return false; }

}
