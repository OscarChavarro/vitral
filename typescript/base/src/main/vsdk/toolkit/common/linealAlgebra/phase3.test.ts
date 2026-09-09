import { describe, expect, it } from "vitest";
import { Matrix4x4d, Matrix4x4f, MatrixNxM, Quaterniond, Quaternionf, Vector2Dd, Vector2Df, Vector3Dd, Vector3Df, Vector4Dd, Vector4Df } from "../../../../index.js";

describe("ported Java linear-algebra test inventory", () => {
  it("Vector2DTest", () => { const a=new Vector2Dd(1.5,-2),b=new Vector2Dd(-.5,3);expect(a.add(b).epsilonEquals(new Vector2Dd(1,1),1e-9)).toBe(true);expect(()=>a.epsilonEquals(b,-1)).toThrow(); });
  it("Vector2DfTest", () => { const a=new Vector2Df(1.5,-2),b=new Vector2Df(-.5,3);expect(a.add(b).epsilonEquals(new Vector2Df(1,1),1e-6)).toBe(true); });
  it("Vector3DTest", () => { const a=new Vector3Dd(1,0,0),b=new Vector3Dd(0,1,0);expect(a.crossProduct(b).epsilonEquals(new Vector3Dd(0,0,1),1e-12)).toBe(true);expect(Vector3Dd.fromSpherical(3,a.obtainSphericalThetaAngle(),a.obtainSphericalPhiAngle()).length()).toBeCloseTo(3); });
  it("Vector3DfTest", () => { const a=new Vector3Df(1,0,0),b=new Vector3Df(0,1,0);expect(a.crossProduct(b).epsilonEquals(new Vector3Df(0,0,1),1e-6)).toBe(true); });
  it("Vector4DTest", () => { expect(new Vector4Dd(4,6,8,2).dividedByW().epsilonEquals(new Vector4Dd(2,3,4,1),1e-12)).toBe(true); });
  it("Vector4DfTest", () => { expect(new Vector4Df(4,6,8,2).dividedByW().epsilonEquals(new Vector4Df(2,3,4,1),1e-6)).toBe(true); });
  it("QuaternionTest", () => { const q=new Quaterniond(new Vector3Dd(0,0,Math.sin(Math.PI/4)),Math.cos(Math.PI/4));expect(q.rotate(new Vector3Dd(1,0,0)).epsilonEquals(new Vector3Dd(0,1,0),1e-8)).toBe(true); });
  it("QuaternionfTest", () => { const q=new Quaternionf(new Vector3Df(0,0,Math.fround(Math.sin(Math.PI/4))),Math.fround(Math.cos(Math.PI/4)));expect(q.rotate(new Vector3Df(1,0,0)).epsilonEquals(new Vector3Df(0,1,0),1e-5)).toBe(true); });
  it("MatrixNxMTest", () => { const m=new MatrixNxM(2,2).withVal(0,0,4).withVal(0,1,7).withVal(1,0,2).withVal(1,1,6);expect(m.determinant()).toBeCloseTo(10);expect(m.multiply(m.inverse()).epsilonEquals(new MatrixNxM(2,2),1e-8)).toBe(true); });
  it("Matrix4x4Test", () => { const m=new Matrix4x4d().translation(5,-2,1.5);expect(m.multiply(new Vector3Dd(1,2,3)).epsilonEquals(new Vector3Dd(6,0,4.5),1e-9)).toBe(true);expect(new Matrix4x4d().scale(2,3,4).determinant()).toBeCloseTo(24); });
  it("Matrix4x4fTest", () => { const m=new Matrix4x4f().translation(5,-2,1.5);expect(m.multiply(new Vector3Df(1,2,3)).epsilonEquals(new Vector3Df(6,0,4.5),1e-5)).toBe(true); });
});
