#ifndef __TREE__
#define __TREE__
#include <cstddef>
#include <new>
#include <utility>
#include <iterator>
#include <functional>
#include <initializer_list>
namespace std {
template <typename T> struct __tree_node {
    T value;
    __tree_node *parent;
    __tree_node *left;
    __tree_node *right;
    __tree_node(const T &v, __tree_node *p) : value(v), parent(p), left(nullptr), right(nullptr) {}
    static __tree_node *first(__tree_node *n) {
        if (n) {
            while (n->left) {
                n = n->left;
            }
        }
        return n;
    }
    static __tree_node *last(__tree_node *n) {
        if (n) {
            while (n->right) {
                n = n->right;
            }
        }
        return n;
    }
    static __tree_node *next(__tree_node *n) {
        if (n->right) {
            return first(n->right);
        }
        while (n->parent && n == n->parent->right) {
            n = n->parent;
        }
        return n->parent;
    }
    static __tree_node *previous(__tree_node *n) {
        if (n->left) {
            return last(n->left);
        }
        while (n->parent && n == n->parent->left) {
            n = n->parent;
        }
        return n->parent;
    }
};
template <typename T, typename R, typename P> class __tree_iterator {
public:
    typedef bidirectional_iterator_tag iterator_category;
    typedef T value_type;
    typedef ptrdiff_t difference_type;
    typedef P pointer;
    typedef R reference;
    typedef __tree_node<T> Node;
    __tree_iterator() : node(nullptr), root(nullptr) {}
    __tree_iterator(Node *n, Node *const *r) : node(n), root(r) {}
    template <typename R2, typename P2> __tree_iterator(const __tree_iterator<T, R2, P2> &other)
        : node(other.node), root(other.root) {}
    R operator*() const { return node->value; }
    P operator->() const { return &node->value; }
    __tree_iterator &operator++() {
        node = Node::next(node);
        return *this;
    }
    __tree_iterator operator++(int) {
        __tree_iterator old(*this);
        ++*this;
        return old;
    }
    __tree_iterator &operator--() {
        node = node ? Node::previous(node) : Node::last(*root);
        return *this;
    }
    __tree_iterator operator--(int) {
        __tree_iterator old(*this);
        --*this;
        return old;
    }
    template <typename R2, typename P2> bool operator==(const __tree_iterator<T, R2, P2> &other) const {
        return node == other.node;
    }
    template <typename R2, typename P2> bool operator!=(const __tree_iterator<T, R2, P2> &other) const {
        return node != other.node;
    }
    Node *node;
    Node *const *root;
};
template <typename T, typename K, typename KeyOf, typename C> class __tree {
public:
    typedef __tree_node<T> Node;
    typedef __tree_iterator<T, T &, T *> iterator;
    typedef __tree_iterator<T, const T &, const T *> const_iterator;
    __tree() : root(nullptr), total(0), compare() {}
    explicit __tree(const C &c) : root(nullptr), total(0), compare(c) {}
    __tree(const __tree &other) : root(nullptr), total(0), compare(other.compare) { copy(other); }
    __tree(__tree &&other) noexcept : root(other.root), total(other.total), compare(other.compare) {
        other.root = nullptr;
        other.total = 0;
    }
    ~__tree() { clear(); }
    __tree &operator=(const __tree &other) {
        if (this != &other) {
            clear();
            compare = other.compare;
            copy(other);
        }
        return *this;
    }
    __tree &operator=(__tree &&other) noexcept {
        if (this != &other) {
            clear();
            swap(other);
        }
        return *this;
    }
    iterator begin() noexcept { return iterator(Node::first(root), &root); }
    iterator end() noexcept { return iterator(nullptr, &root); }
    const_iterator begin() const noexcept { return const_iterator(Node::first(root), &root); }
    const_iterator end() const noexcept { return const_iterator(nullptr, &root); }
    bool empty() const noexcept { return total == 0; }
    size_t size() const noexcept { return total; }
    C key_comp() const { return compare; }
    pair<iterator, bool> insert(const T &value) {
        Node *parent = nullptr;
        Node **link = &root;
        while (*link) {
            parent = *link;
            if (compare(KeyOf()(value), KeyOf()(parent->value))) {
                link = &parent->left;
            } else if (compare(KeyOf()(parent->value), KeyOf()(value))) {
                link = &parent->right;
            } else {
                return pair<iterator, bool>(iterator(parent, &root), false);
            }
        }
        *link = new Node(value, parent);
        total++;
        return pair<iterator, bool>(iterator(*link, &root), true);
    }
    iterator find(const K &key) { return iterator(lookup(key), &root); }
    const_iterator find(const K &key) const { return const_iterator(lookup(key), &root); }
    size_t count(const K &key) const { return lookup(key) ? 1 : 0; }
    iterator lower_bound(const K &key) { return iterator(bound(key, false), &root); }
    const_iterator lower_bound(const K &key) const { return const_iterator(bound(key, false), &root); }
    iterator upper_bound(const K &key) { return iterator(bound(key, true), &root); }
    const_iterator upper_bound(const K &key) const { return const_iterator(bound(key, true), &root); }
    iterator erase(const_iterator position) {
        Node *following = Node::next(position.node);
        unlink(position.node);
        return iterator(following, &root);
    }
    iterator erase(const_iterator from, const_iterator to) {
        while (from != to) {
            from = erase(from);
        }
        return iterator(to.node, &root);
    }
    size_t erase(const K &key) {
        Node *n = lookup(key);
        if (!n) {
            return 0;
        }
        unlink(n);
        return 1;
    }
    void clear() noexcept {
        destroy(root);
        root = nullptr;
        total = 0;
    }
    void swap(__tree &other) noexcept {
        std::swap(root, other.root);
        std::swap(total, other.total);
        std::swap(compare, other.compare);
    }
private:
    Node *root;
    size_t total;
    C compare;
    void copy(const __tree &other) {
        for (const_iterator i = other.begin(); i != other.end(); ++i) {
            insert(*i);
        }
    }
    Node *lookup(const K &key) const {
        Node *n = root;
        while (n) {
            if (compare(key, KeyOf()(n->value))) {
                n = n->left;
            } else if (compare(KeyOf()(n->value), key)) {
                n = n->right;
            } else {
                return n;
            }
        }
        return nullptr;
    }
    Node *bound(const K &key, bool upper) const {
        Node *n = root;
        Node *result = nullptr;
        while (n) {
            if (upper ? compare(key, KeyOf()(n->value)) : !compare(KeyOf()(n->value), key)) {
                result = n;
                n = n->left;
            } else {
                n = n->right;
            }
        }
        return result;
    }
    void replace(Node *n, Node *child) {
        if (!n->parent) {
            root = child;
        } else if (n == n->parent->left) {
            n->parent->left = child;
        } else {
            n->parent->right = child;
        }
        if (child) {
            child->parent = n->parent;
        }
    }
    void unlink(Node *n) {
        if (!n->left) {
            replace(n, n->right);
        } else if (!n->right) {
            replace(n, n->left);
        } else {
            Node *successor = Node::first(n->right);
            if (successor->parent != n) {
                replace(successor, successor->right);
                successor->right = n->right;
                successor->right->parent = successor;
            }
            replace(n, successor);
            successor->left = n->left;
            successor->left->parent = successor;
        }
        delete n;
        total--;
    }
    static void destroy(Node *n) {
        while (n) {
            destroy(n->right);
            Node *left = n->left;
            delete n;
            n = left;
        }
    }
};
}

#endif
