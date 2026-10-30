#include <exception>
#include <functional>
#include <stdexcept>
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

bad_function_call::bad_function_call() noexcept {}
bad_function_call::~bad_function_call() noexcept {}
const char *
bad_function_call::what() const noexcept { return "std::bad_function_call"; }

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

static char *
copyMessage(const char *message) { return strdup(message ? message : ""); }

logic_error::logic_error(const char *message) : text(copyMessage(message)) {}
logic_error::logic_error(const string &message) : text(copyMessage(message.c_str())) {}
logic_error::logic_error(const logic_error &other) noexcept : exception(other), text(copyMessage(other.text)) {}

logic_error &
logic_error::operator=(const logic_error &other) noexcept {
    if (this != &other) {
        free(text);
        text = copyMessage(other.text);
    }
    return *this;
}

logic_error::~logic_error() noexcept { free(text); }
const char *
logic_error::what() const noexcept { return text; }

runtime_error::runtime_error(const char *message) : text(copyMessage(message)) {}
runtime_error::runtime_error(const string &message) : text(copyMessage(message.c_str())) {}
runtime_error::runtime_error(const runtime_error &other) noexcept : exception(other), text(copyMessage(other.text)) {}

runtime_error &
runtime_error::operator=(const runtime_error &other) noexcept {
    if (this != &other) {
        free(text);
        text = copyMessage(other.text);
    }
    return *this;
}

runtime_error::~runtime_error() noexcept { free(text); }
const char *
runtime_error::what() const noexcept { return text; }

domain_error::~domain_error() noexcept {}
invalid_argument::~invalid_argument() noexcept {}
length_error::~length_error() noexcept {}
out_of_range::~out_of_range() noexcept {}
range_error::~range_error() noexcept {}
overflow_error::~overflow_error() noexcept {}
underflow_error::~underflow_error() noexcept {}
}
