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

Port of `vsdk.toolkit.io.geometry.stepCad.reader._StepEntity`. Java's
`toString` prints the `List<String>` through `AbstractCollection.toString`,
which is `[a, b, c]`; the same text is built here.
*/
export class _StepEntity {
    public static readonly COMPLEX_NAME = "**COMPLEX**";

    public constructor(
        public readonly id: number,
        public readonly name: string,
        public readonly params: readonly string[],
    ) {}

    public toString(): string {
        return "#" + this.id + "=" + this.name + "[" + this.params.join(", ") + "]";
    }
}
