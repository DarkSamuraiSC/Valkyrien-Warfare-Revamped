/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2015-2018 the Valkyrien Warfare team
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income unless it is to be used as a part of a larger project (IE: "modpacks"), nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from the Valkyrien Warfare team.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: The Valkyrien Warfare team), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package valkyrienwarfare.physics.collision.polygons;

import valkyrienwarfare.math.VWMath;
import valkyrienwarfare.math.Vector;

public class PhysCollisionObject {

    public final Vector collision_normal;
    public final Polygon movable, fixed;
    public double penetrationDistance;
    public boolean seperated;
    private double[] blockMinMax;
    private double movMaxFixMin;
    private double movMinFixMax;

    public PhysCollisionObject(Polygon movable_, Polygon stationary, Vector axes) {
        this.collision_normal = axes;
        this.movable = movable_;
        this.fixed = stationary;
        this.generateCollision();
    }

    public void generateCollision() {
        double[] playerMinMax = VWMath.getMinMaxOfArray(this.movable.getProjectionOnVector(this.collision_normal));
        this.blockMinMax = VWMath.getMinMaxOfArray(this.fixed.getProjectionOnVector(this.collision_normal));
        this.movMaxFixMin = playerMinMax[0] - this.blockMinMax[1];
        this.movMinFixMax = playerMinMax[1] - this.blockMinMax[0];
        if (this.movMaxFixMin > 0 || this.movMinFixMax < 0) {
            this.seperated = true;
            this.penetrationDistance = 0.0D;
            return;
        }
        // Set the penetration to be the smaller distance
        Vector firstContactPoint;
        if (Math.abs(this.movMaxFixMin) > Math.abs(this.movMinFixMax)) {
            this.penetrationDistance = this.movMinFixMax;
            for (Vector v : this.movable.getVertices()) {
                if (v.dot(this.collision_normal) == playerMinMax[1]) {
                    firstContactPoint = v;
                }
            }
        } else {
            this.penetrationDistance = this.movMaxFixMin;
            for (Vector v : this.movable.getVertices()) {
                if (v.dot(this.collision_normal) == playerMinMax[0]) {
                    firstContactPoint = v;
                }
            }
        }
        this.seperated = false;
    }

    public Vector getSecondContactPoint() {
        if (Math.abs(this.movMaxFixMin) > Math.abs(this.movMinFixMax)) {
            for (Vector v : this.fixed.getVertices()) {
                if (v.dot(this.collision_normal) == this.blockMinMax[0]) {
                    return v;
                }
            }
        } else {
            for (Vector v : this.fixed.getVertices()) {
                if (v.dot(this.collision_normal) == this.blockMinMax[1]) {
                    return v;
                }
            }
        }
        return null;
    }

    public Vector getResponse() {
        return this.collision_normal.getProduct(this.penetrationDistance);
    }

    public void setResponse(Vector v) {
        v.X = this.collision_normal.X * this.penetrationDistance;
        v.Y = this.collision_normal.Y * this.penetrationDistance;
        v.Z = this.collision_normal.Z * this.penetrationDistance;
    }
}