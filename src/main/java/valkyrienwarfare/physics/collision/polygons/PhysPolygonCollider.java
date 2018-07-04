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

import valkyrienwarfare.math.Vector;

public class PhysPolygonCollider {

    public Vector[] potentialSeperatingAxes;
    public boolean seperated;
    public PhysCollisionObject[] collisions;
    public int minDistanceIndex;
    public double minDistance;
    public Polygon entity;
    public Polygon block;

    public PhysPolygonCollider(Polygon movable, Polygon stationary, Vector[] axes) {
        this.potentialSeperatingAxes = axes;
        this.entity = movable;
        this.block = stationary;
        this.processData();
    }

    // TODO: Fix this, processes the penetration distances backwards from their reality
    public void processData() {
        this.collisions = new PhysCollisionObject[this.potentialSeperatingAxes.length];
        for (int i = 0; i < this.potentialSeperatingAxes.length && !this.seperated; i++) {
            this.collisions[i] = new PhysCollisionObject(this.entity, this.block, this.potentialSeperatingAxes[i]);
            this.seperated = this.collisions[i].seperated;
        }
        if (!this.seperated) {
            this.minDistance = 420;
            for (int i = 0; i < this.potentialSeperatingAxes.length; i++) {
                // Take the collision response closest to 0
                if (Math.abs(this.collisions[i].penetrationDistance) < this.minDistance) {
                    this.minDistanceIndex = i;
                    this.minDistance = Math.abs(this.collisions[i].penetrationDistance);
                }
            }
        }
    }

}