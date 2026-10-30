#include <signal.h>
#include <errno.h>

static bool
valid(int number) { return number > 0 && number < NSIG && number != SIGKILL; }

sighandler_t
signal(int number, sighandler_t) {
    if (!valid(number)) {
        errno = EINVAL;
        return SIG_ERR;
    }
    return SIG_DFL;
}

int
raise(int number) {
    errno = valid(number) ? ENOSYS : EINVAL;
    return -1;
}
