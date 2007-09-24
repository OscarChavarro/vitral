<map version="0.7.1">
<node TEXT="1D Curves in 3D space">
<node TEXT="1D Curves in 3D space" POSITION="left">
<node TEXT="Explicit: y=f(x), z = g(x)"/>
<node TEXT="Implicit: canonic equation = 0"/>
<node TEXT="Parametric">
<node TEXT="N-order (N &gt; 3)"/>
<node TEXT="Cubic (N = 3)">
<node TEXT="Hermite [FOLE1992].11.2.1"/>
<node TEXT="Bezier [FOLE1992].11.2.2"/>
<node TEXT="Spline">
<node TEXT="Natural cubic splines [FOLE1992].11.2.3"/>
<node TEXT="B Uniform non rational [FOLE1992].11.2.3"/>
<node TEXT="B Nonuniform non rational [FOLE1992].11.2.4"/>
<node TEXT="Nonuniform rational cubic polinomial curve segments [FOLE1992].11.2.5"/>
<node TEXT="Catmull-Rom (Overhauser) [FOLE1992].11.2.6"/>
<node TEXT="Uniform shaped beta [FOLE1992].11.2.6">
<node TEXT="Continuous shaped"/>
<node TEXT="Discretely shaped"/>
</node>
<node TEXT="Kochanek/Bartels Hermite variant [FOLE1992].11.2.6"/>
</node>
</node>
<node TEXT="Quadratic (N = 2)"/>
<node TEXT="Straight lines (N = 1)"/>
</node>
</node>
<node TEXT="2D Surfaces in 3D space" POSITION="right">
<node TEXT="Implicit"/>
<node TEXT="Polygon meshes"/>
<node TEXT="Patches">
<node TEXT="Hermite [FOLE1992].11.3.1"/>
<node TEXT="Coons/Ferguson [FOLE1992].11.3.1"/>
<node TEXT="Bezier [FOLE1992].11.3.2"/>
<node TEXT="B-spline surfaces [FOLE1992].11.3.3"/>
</node>
<node TEXT="Quadrics [FOLE1992].11.4"/>
</node>
<node TEXT="Comments" POSITION="right">
<node TEXT="Splnes are">
<node TEXT="Natural"/>
<node TEXT="B (Basis)"/>
</node>
<node TEXT="Splines are">
<node TEXT="Uniform with respect to distances in ti, ti+1, ..."/>
<node TEXT="Non uniform"/>
</node>
<node TEXT="Splines are">
<node TEXT="Non-rational (simple polynomials)">
<node TEXT="Invariant under">
<node TEXT="Rotation"/>
<node TEXT="Scaling"/>
<node TEXT="Translation"/>
</node>
</node>
<node TEXT="Rational (ratios of polynomials)">
<node TEXT="Invariant under">
<node TEXT="Rotation"/>
<node TEXT="Scaling"/>
<node TEXT="Translation"/>
<node TEXT="Perspective"/>
</node>
<node TEXT="Can represent any conic section"/>
</node>
</node>
<node TEXT="Splines propierties">
<node TEXT="Local control">
<node TEXT="YES">
<node TEXT="Catmul-rom"/>
<node TEXT="Uniformly shaped beta when continously or discretely shaped"/>
</node>
<node TEXT="NO">
<node TEXT="Natural splines"/>
<node TEXT="Uniformly shaped beta"/>
</node>
</node>
<node TEXT="Convex hull">
<node TEXT="YES">
<node TEXT="Hermite"/>
<node TEXT="Bezier"/>
<node TEXT="Uniformly shaped beta"/>
</node>
<node TEXT="NO"/>
</node>
<node TEXT="Passing by knots">
<node TEXT="YES">
<node TEXT="Hermite"/>
<node TEXT="Bezier"/>
<node TEXT="Catmull-rom"/>
<node TEXT="Natural splines"/>
</node>
<node TEXT="NO">
<node TEXT="B-Splines"/>
</node>
</node>
<node TEXT="Ease of subdivision"/>
<node TEXT="Continuity"/>
</node>
</node>
</node>
</map>
