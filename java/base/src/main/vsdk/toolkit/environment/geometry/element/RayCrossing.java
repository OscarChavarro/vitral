package vsdk.toolkit.environment.geometry.element;

import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.material.Material;
public class RayCrossing {
    public double t;
    public Geometry geometry;
    public RayCrossingKind kind;
    public SimpleBody body;
    public Material material;
    public RayCrossing(double t, Geometry geometry, RayCrossingKind kind) { this.t = t; this.geometry = geometry; this.kind = kind; }
}
