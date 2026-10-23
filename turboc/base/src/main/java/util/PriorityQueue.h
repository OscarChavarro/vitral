#ifndef __PRIORITY_QUEUE__
#define __PRIORITY_QUEUE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Object.h"
#include "java/util/function/Consumer.h"
#include "java/util/function/Predicate.h"

template <class T>
class PriorityQueueAccess;

template <class T>
class PriorityQueue : public Object {
  private:
    enum { DEFAULT_CAPACITY = 11 };

    int currentSize;
    int maxSize;
    int activeLimit;
    T *data;

    void init(int initialCapacity);
    void allocateStorage(int capacity);
    void releaseStorage();
    void ensureCapacity(int requiredCapacity);
    void siftUp(int index);
    void siftDown(int index);
    BOOLEAN lessThan(const T& a, const T& b) const;

    friend class PriorityQueueAccess<T>;

  public:
    // Iterator: JDK-style unordered traversal of the internal heap array [1..currentSize]
    class Iterator {
      private:
        const PriorityQueue<T>* owner;
        int cursor;

        friend class PriorityQueue<T>;
        Iterator(const PriorityQueue<T>* q, int start) : owner(q), cursor(start) {}

      public:
        BOOLEAN hasNext() const
        {
            return cursor <= owner->currentSize;
        }

        T next()
        {
            return owner->data[cursor++];
        }
    };

    PriorityQueue();
    PriorityQueue(int initialCapacity);
    virtual ~PriorityQueue();

    BOOLEAN add(const T& elem);
    BOOLEAN offer(const T& elem);
    T peek();
    T poll();
    int size() const;
    void clear();

    Iterator iterator() const
    {
        return Iterator(this, 1);
    }

    BOOLEAN removeIf(Predicate<T>& p);
    void forEach(Consumer<T>& action) const;

    BOOLEAN contains(const T& elem) const;
    BOOLEAN remove(const T& elem);
    T* toArray() const;
    T* toArray(T* target) const;

    // C++ ergonomic helpers for range-for loops (not in JDK API; expose iterator to C++ idioms)
    const T* begin() const;
    const T* end() const;
    T* begin();
    T* end();
};

template <class T>
class PriorityQueueAccess {
  public:
    static inline void setActiveLimit(PriorityQueue<T> &queue, int limit)
    {
        queue.activeLimit = limit;
    }
};


#endif
