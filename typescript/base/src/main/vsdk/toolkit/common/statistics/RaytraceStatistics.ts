type StatisticsFlags = { readonly raytrace?: boolean; readonly polyhedral?: boolean };
const statisticsFlags = (globalThis as { __vitralStatistics?: StatisticsFlags }).__vitralStatistics;

/** Opt-in browser-safe counterpart of the Java system-property ray-tracing counters. */
export class RaytraceStatistics {
  private static readonly enabled = statisticsFlags?.raytrace === true;
  private static primaryRays = 0n; private static shadowRays = 0n; private static reflectionRays = 0n;
  private static sceneTraversals = 0n; private static objectIntersectionTests = 0n; private static rayWithTCalls = 0n;
  private static rayHitInstances = 0n; private static hitInfoClones = 0n; private static geometryDetailComputations = 0n;
  public static isEnabled(): boolean { return this.enabled; }
  public static recordPrimaryRay(): void { if (this.enabled) this.primaryRays++; } public static recordShadowRay(): void { if (this.enabled) this.shadowRays++; } public static recordReflectionRay(): void { if (this.enabled) this.reflectionRays++; }
  public static recordSceneTraversal(): void { if (this.enabled) this.sceneTraversals++; } public static recordObjectIntersectionTest(): void { if (this.enabled) this.objectIntersectionTests++; } public static recordRayWithT(): void { if (this.enabled) this.rayWithTCalls++; }
  public static recordRayHitInstance(): void { if (this.enabled) this.rayHitInstances++; } public static recordHitInfoClone(): void { if (this.enabled) this.hitInfoClones++; } public static recordGeometryDetailComputation(): void { if (this.enabled) this.geometryDetailComputations++; }
  public static printSummary(): void { if (!this.enabled) return; const total = this.primaryRays + this.shadowRays + this.reflectionRays; console.log(`Ray statistics:\n  Primary rays: ${this.primaryRays}\n  Shadow rays: ${this.shadowRays}\n  Reflection rays: ${this.reflectionRays}\n  Total rays cast: ${total}\n  Scene traversals: ${this.sceneTraversals}\n  Object intersection tests: ${this.objectIntersectionTests}\n  Ray.withT calls: ${this.rayWithTCalls}\n  RayHit instances: ${this.rayHitInstances}\n  Hit info clones: ${this.hitInfoClones}\n  Geometry detail computations: ${this.geometryDetailComputations}${this.sceneTraversals > 0n ? `\n  Avg. object tests / traversal: ${Number(this.objectIntersectionTests) / Number(this.sceneTraversals)}` : ""}`); }
}
