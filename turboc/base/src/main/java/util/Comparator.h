#ifndef __COMPARATOR__
#define __COMPARATOR__

/**
Emulation of `java.util.Comparator`: a total order over the elements of a
collection, used by `Collections::sort`.
*/
template <class T>
class Comparator {
  public:
    virtual ~Comparator() {}

    /**
    @return a negative number, zero or a positive number when `a` goes
    before, is equivalent to or goes after `b`
    */
    virtual int compare(const T &a, const T &b) const = 0;
};

/**
Emulation of `java.util.Comparator.naturalOrder()`: the order of the
`operator<` of the elements.
*/
template <class T>
class NaturalOrderComparator : public Comparator<T> {
  public:
    virtual int compare(const T &a, const T &b) const
    {
        if ( a < b ) {
            return -1;
        }
        if ( b < a ) {
            return 1;
        }
        return 0;
    }
};

#endif
