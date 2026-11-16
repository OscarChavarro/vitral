#ifndef __STEP_ENTITY__
#define __STEP_ENTITY__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"
#include "java/util/HashMapIterationOrder.h"

/**
Immutable value type representing one parsed ISO 10303-21 entity instance
from the DATA section of a STEP file.

The `name` field holds the uppercase entity type name
(e.g. "MANIFOLD_SOLID_BREP", "CARTESIAN_POINT"). For complex (ANDOR)
entity instances — those encoded as `( NAME1(...) NAME2(...) )` — the
name is "**COMPLEX**" and `params` contains a single element with the
raw body text; the topology builder ignores them during traversal.

The `params` list holds the raw top-level parameter strings as they
appear in the entity's parameter list, with no further interpretation.
Each entry may be a reference (`#id`), an aggregate (`(#1,#2,...)`),
a STEP string (`'...'`), an enumeration (`.T.`), a number, or a typed
value (`LENGTH_MEASURE(1.5E3)`).

This is an internal collaborator of `StepReader`.
*/
class _StepEntity {
public:
    static const char* const COMPLEX_NAME;

    const int id;
    const java::String name;
    const java::ArrayList<java::String> params;

    _StepEntity(int id, const java::String& name,
                const java::ArrayList<java::String>& params)
        : id(id), name(name), params(params)
    {
    }

    /**
    @return `#id=NAME[p1, p2, ...]`, as Java's `toString` (a `List` prints
    its elements between brackets, separated by ", ")
    */
    java::String toString() const
    {
        java::String out = java::String("#") + java::String::valueOf(id) +
            "=" + name + "[";
        long int i;
        for ( i = 0; i < params.size(); i++ ) {
            if ( i > 0 ) {
                out += ", ";
            }
            out += params.get(i);
        }
        out += "]";
        return out;
    }
};

/**
C++ port of the `Map<Integer, _StepEntity>` that `_StepTokenizer.parse`
returns (a Java `HashMap`). It owns its entities. Since `java::HashMap`
cannot be iterated, the keys are also kept in insertion order, and
`keysInJavaOrder` gives them in the order Java iterates its `HashMap`
(see `java::HashMapIterationOrder`), so `values()` loops visit the
entities as Java does.
*/
class _StepEntityMap {
public:
    _StepEntityMap() {}

    ~_StepEntityMap()
    {
        long int i;
        for ( i = 0; i < keysInInsertionOrder.size(); i++ ) {
            _StepEntity** e = map.get(keysInInsertionOrder.get(i));
            if ( e != nullptr ) {
                delete *e;
            }
        }
    }

    /**
    As Java's `put`: an entity with an existing id replaces the previous
    one, which keeps its place in the iteration order.
    @param entity entity to store, owned by the map from now on
    */
    void put(int id, _StepEntity* entity)
    {
        _StepEntity** previous = map.get(id);
        if ( previous != nullptr ) {
            delete *previous;
        }
        else {
            keysInInsertionOrder.add(id);
        }
        map.put(id, entity);
    }

    /**
    @return the entity with the given id, or null
    */
    _StepEntity* get(int id) const
    {
        const _StepEntity* const* e = map.get(id);
        return e != nullptr ? const_cast<_StepEntity*>(*e) : nullptr;
    }

    long int size() const
    {
        return keysInInsertionOrder.size();
    }

    /**
    @return the ids in the iteration order of a Java `HashMap`
    */
    java::ArrayList<int> keysInJavaOrder() const
    {
        return java::HashMapIterationOrder::integerKeys(keysInInsertionOrder);
    }

private:
    java::HashMap<int, _StepEntity*> map;
    java::ArrayList<int> keysInInsertionOrder;

    _StepEntityMap(const _StepEntityMap&);
    _StepEntityMap& operator=(const _StepEntityMap&);
};

#endif
