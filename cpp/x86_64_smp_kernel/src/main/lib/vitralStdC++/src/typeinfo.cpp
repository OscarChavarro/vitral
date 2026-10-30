#include <typeinfo>
#include <string.h>
#include <stddef.h>

namespace __cxxabiv1 {
struct SearchState {
    const std::type_info *target;
    const void *found[8];
    unsigned count;
};

static void
record(SearchState &state, const void *pointer) {
    for (unsigned i = 0; i < state.count; i++) {
        if (state.found[i] == pointer) {
            return;
        }
    }
    if (state.count < 8) {
        state.found[state.count++] = pointer;
    }
}

class __class_type_info : public std::type_info {
public:
    explicit __class_type_info(const char *name) : std::type_info(name) {}
    ~__class_type_info() override;
    virtual void search(const void *pointer, SearchState &state) const;
};

class __si_class_type_info : public __class_type_info {
public:
    explicit __si_class_type_info(const char *name) : __class_type_info(name), base(nullptr) {}
    ~__si_class_type_info() override;
    void search(const void *pointer, SearchState &state) const override;
    const __class_type_info *base;
};

struct __base_class_type_info {
    const __class_type_info *base;
    long flags;
};

class __vmi_class_type_info : public __class_type_info {
public:
    explicit __vmi_class_type_info(const char *name) : __class_type_info(name), flags(0), count(0) {}
    ~__vmi_class_type_info() override;
    void search(const void *pointer, SearchState &state) const override;
    unsigned flags;
    unsigned count;
    __base_class_type_info bases[1];
};

class __fundamental_type_info : public std::type_info {
public:
    explicit __fundamental_type_info(const char *name) : std::type_info(name) {}
    ~__fundamental_type_info() override;
};

class __pointer_type_info : public std::type_info {
public:
    explicit __pointer_type_info(const char *name) : std::type_info(name), flags(0), pointee(nullptr) {}
    ~__pointer_type_info() override;
    unsigned flags;
    const std::type_info *pointee;
};

class __array_type_info : public std::type_info {
public:
    explicit __array_type_info(const char *name) : std::type_info(name) {}
    ~__array_type_info() override;
};

class __function_type_info : public std::type_info {
public:
    explicit __function_type_info(const char *name) : std::type_info(name) {}
    ~__function_type_info() override;
};

class __enum_type_info : public std::type_info {
public:
    explicit __enum_type_info(const char *name) : std::type_info(name) {}
    ~__enum_type_info() override;
};

class __pbase_type_info : public std::type_info {
public:
    explicit __pbase_type_info(const char *name) : std::type_info(name), flags(0), pointee(nullptr) {}
    ~__pbase_type_info() override;
    unsigned flags;
    const std::type_info *pointee;
};

class __pointer_to_member_type_info : public __pbase_type_info {
public:
    explicit __pointer_to_member_type_info(const char *name) : __pbase_type_info(name), context(nullptr) {}
    ~__pointer_to_member_type_info() override;
    const __class_type_info *context;
};

__class_type_info::~__class_type_info() {}
__si_class_type_info::~__si_class_type_info() {}
__vmi_class_type_info::~__vmi_class_type_info() {}
__fundamental_type_info::~__fundamental_type_info() {}
__pointer_type_info::~__pointer_type_info() {}
__array_type_info::~__array_type_info() {}
__function_type_info::~__function_type_info() {}
__enum_type_info::~__enum_type_info() {}
__pbase_type_info::~__pbase_type_info() {}
__pointer_to_member_type_info::~__pointer_to_member_type_info() {}

void
__class_type_info::search(const void *pointer, SearchState &state) const {
    if (*this == *state.target) {
        record(state, pointer);
    }
}

void
__si_class_type_info::search(const void *pointer, SearchState &state) const {
    if (*this == *state.target) {
        record(state, pointer);
    } else {
        base->search(pointer, state);
    }
}

void
__vmi_class_type_info::search(const void *pointer, SearchState &state) const {
    if (*this == *state.target) {
        record(state, pointer);
        return;
    }
    for (unsigned i = 0; i < count; i++) {
        long offset = bases[i].flags >> 8;
        if (!(bases[i].flags & 2)) {
            continue;
        }
        const char *adjusted = (const char *)pointer;
        if (bases[i].flags & 1) {
            const char *table = *(const char *const *)pointer;
            offset = *(const ptrdiff_t *)(table + offset);
        }
        bases[i].base->search(adjusted + offset, state);
    }
}

static void *
dynamicCast(const void *source, const __class_type_info *sourceType,
            const __class_type_info *destinationType, ptrdiff_t) {
    const char *const *table = *(const char *const *const *)source;
    ptrdiff_t top = ((const ptrdiff_t *)table)[-2];
    const __class_type_info *wholeType = ((const __class_type_info *const *)table)[-1];
    const void *whole = (const char *)source + top;
    SearchState state = {destinationType, {nullptr}, 0};
    wholeType->search(whole, state);
    if (state.count == 1) {
        return (void *)state.found[0];
    }
    void *chosen = nullptr;
    unsigned candidates = 0;
    for (unsigned i = 0; i < state.count; i++) {
        SearchState inner = {sourceType, {nullptr}, 0};
        destinationType->search(state.found[i], inner);
        for (unsigned j = 0; j < inner.count; j++) {
            if (inner.found[j] == source) {
                chosen = (void *)state.found[i];
                candidates++;
                break;
            }
        }
    }
    return candidates == 1 ? chosen : nullptr;
}

void *
derivedFrom(const std::type_info &type, const void *object, const std::type_info &target) {
    const __class_type_info *start = dynamic_cast<const __class_type_info *>(&type);
    if (!start) {
        return nullptr;
    }
    SearchState state = {&target, {nullptr}, 0};
    start->search(object, state);
    return state.count ? (void *)state.found[0] : nullptr;
}
}

void *
dynamicCastEntry(const void *source, const __cxxabiv1::__class_type_info *sourceType,
                 const __cxxabiv1::__class_type_info *destinationType, ptrdiff_t hint) asm("__dynamic_cast");

void *
dynamicCastEntry(const void *source, const __cxxabiv1::__class_type_info *sourceType,
                 const __cxxabiv1::__class_type_info *destinationType, ptrdiff_t hint) {
    return __cxxabiv1::dynamicCast(source, sourceType, destinationType, hint);
}

namespace std {
type_info::~type_info() {}

bool
type_info::before(const type_info &other) const noexcept {
    if (typeName[0] == '*' || other.typeName[0] == '*') {
        return typeName < other.typeName;
    }
    return strcmp(typeName, other.typeName) < 0;
}

bool
type_info::operator==(const type_info &other) const noexcept {
    return typeName == other.typeName || (typeName[0] != '*' && other.typeName[0] != '*' && strcmp(typeName, other.typeName) == 0);
}

bad_cast::bad_cast() noexcept {}
bad_cast::~bad_cast() noexcept {}
const char *
bad_cast::what() const noexcept { return "std::bad_cast"; }
bad_typeid::bad_typeid() noexcept {}
bad_typeid::~bad_typeid() noexcept {}
const char *
bad_typeid::what() const noexcept { return "std::bad_typeid"; }
}
