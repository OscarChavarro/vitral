/**
Java's `LightGizmoStyle` is an `enum`. A string enum is used here so that a
style keeps its identity across a structured clone, as the rest of the
TypeScript edition does for the Java enums that cross a worker boundary.
*/
export enum LightGizmoStyle {
    CROSS = "CROSS",
    OMNI_BILLBOARD = "OMNI_BILLBOARD",
}
