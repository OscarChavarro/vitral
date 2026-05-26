#ifndef JAVA_STRING__
#define JAVA_STRING__

#include <cstdarg>
#include <string>

namespace java {

class String {
  private:
    char *value;

    void
    assignText(const char *text);

  public:
    static constexpr int npos = -1;

    String();
    String(const String &other);
    String(const char *text);
    String(const std::string &text);
    ~String();

    void
    dispose();

    String &
    operator=(const String &other);

    String &
    operator=(const char *other);

    String &
    operator=(const std::string &other);

    const char *
    toCString() const;

    const char *
    c_str() const;

    const char *
    data() const;

    std::string
    toStdString() const;

    operator std::string() const;

    int
    length() const;

    int
    size() const;

    bool
    isEmpty() const;

    bool
    empty() const;

    char
    charAt(int index) const;

    bool
    equals(const String &other) const;

    bool
    equals(const char *other) const;

    String
    substring(int beginIndex) const;

    String
    substring(int beginIndex, int endIndex) const;

    String
    substr(int beginIndex) const;

    String
    substr(int beginIndex, int count) const;

    int
    indexOf(char token, int fromIndex = 0) const;

    bool
    startsWith(const char *prefix) const;

    int
    rfind(char token) const;

    int
    rfind(const char *token) const;

    int
    rfind(const char *token, int fromIndex) const;

    int
    rfind(const String &token) const;

    String
    concat(const String &other) const;

    String
    concat(const char *other) const;

    String
    operator+(const String &other) const;

    String
    operator+(const char *other) const;

    bool
    operator==(const String &other) const;

    bool
    operator==(const char *other) const;

    bool
    operator!=(const String &other) const;

    bool
    operator!=(const char *other) const;

    static String
    valueOf(long long value);

    static String
    valueOf(unsigned long long value);

    static String
    valueOf(long value);

    static String
    valueOf(unsigned long value);

    static String
    valueOf(int value);

    static String
    valueOf(unsigned int value);

    static String
    formatCStringToJavaString(const char *format, va_list arguments);
};

String operator+(const char *left, const String &right);

}

#endif
