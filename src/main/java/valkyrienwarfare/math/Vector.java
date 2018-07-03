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

package valkyrienwarfare.math;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import valkyrienwarfare.mod.coordinates.VectorImmutable;

/**
 * Custom Vector class used by Valkyrien Warfare
 *
 * @author thebest108
 */
public class Vector {

    public double X;
    public double Y;
    public double Z;

    public Vector(double x, double y, double z) {
        this.X = x;
        this.Y = y;
        this.Z = z;
    }

    public Vector() {
    }

    public Vector(double x, double y, double z, double[] rotationMatrix) {
        this(x, y, z);
        this.transform(rotationMatrix);
    }

    public Vector(Vector v) {
        this(v.X, v.Y, v.Z);
    }

    public Vector(Vector v, double scale) {
        this(v);
        this.multiply(scale);
    }

    public Vector(Vec3d vec3) {
        this(vec3.x, vec3.y, vec3.z);
    }

    public Vector(Entity entity) {
        this(entity.posX, entity.posY, entity.posZ);
    }

    public Vector(ByteBuf toRead) {
        this(toRead.readDouble(), toRead.readDouble(), toRead.readDouble());
    }

    public Vector(Vector theNormal, double[] matrixTransform) {
        this(theNormal.X, theNormal.Y, theNormal.Z, matrixTransform);
    }

    public Vector(EnumFacing facing) {
        switch (facing) {
            case DOWN:
                this.Y = 1d;
                break;
            case UP:
                this.Y = -1d;
                break;
            case EAST:
                this.X = -1d;
                break;
            case NORTH:
                this.Z = 1d;
                break;
            case WEST:
                this.X = 1d;
                break;
            case SOUTH:
                this.Z = -1d;
        }
    }

    public static Vector[] generateAxisAlignedNorms() {
        Vector[] norms = new Vector[]{new Vector(1.0D, 0.0D, 0.0D), new Vector(0.0D, 1.0D, 0.0D),
                new Vector(0.0D, 0.0D, 1.0D)};
        return norms;
    }

    public static void writeToBuffer(Vector vector, ByteBuf buffer) {
        buffer.writeFloat((float) vector.X);
        buffer.writeFloat((float) vector.Y);
        buffer.writeFloat((float) vector.Z);
    }

    public static Vector readFromBuffer(ByteBuf buffer) {
        return new Vector(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public Vector getSubtraction(Vector v) {
        return new Vector(v.X - this.X, v.Y - this.Y, v.Z - this.Z);
    }

    public Vector getAddition(Vector v) {
        return new Vector(v.X + this.X, v.Y + this.Y, v.Z + this.Z);
    }

    public void subtract(Vector v) {
        this.subtract(v.X, v.Y, v.Z);
    }

    public void subtract(double x, double y, double z) {
        this.X -= x;
        this.Y -= y;
        this.Z -= z;
    }

    public void add(Vector v) {
        this.add(v.X, v.Y, v.Z);
    }

    public void add(double x, double y, double z) {
        this.X += x;
        this.Y += y;
        this.Z += z;
    }

    public double dot(Vector v) {
        return this.X * v.X + this.Y * v.Y + this.Z * v.Z;
    }

    public Vector cross(Vector v) {
        return new Vector(this.Y * v.Z - v.Y * this.Z, this.Z * v.X - this.X * v.Z, this.X * v.Y - v.X * this.Y);
    }

    public void setCross(Vector v1, Vector v2) {
        this.X = v1.Y * v2.Z - v2.Y * v1.Z;
        this.Y = v1.Z * v2.X - v1.X * v2.Z;
        this.Z = v1.X * v2.Y - v2.X * v1.Y;
    }

    public void multiply(double scale) {
        this.X *= scale;
        this.Y *= scale;
        this.Z *= scale;
    }

    public void divide(double scale) {
        this.X /= scale;
        this.Y /= scale;
        this.Z /= scale;
    }

    public Vector getProduct(double scale) {
        return new Vector(this.X * scale, this.Y * scale, this.Z * scale);
    }

    public Vec3d toVec3d() {
        return new Vec3d(this.X, this.Y, this.Z);
    }

    public void normalize() {
        double length = this.length();
        if (length > 1.0E-6D) {
            this.divide(length);
        } else {
            this.zero();
        }
    }

    public double length() {
        return Math.sqrt(this.lengthSq());
    }

    public double lengthSq() {
        return this.X * this.X + this.Y * this.Y + this.Z * this.Z;
    }

    public boolean isZero() {
        return this.lengthSq() < 1.0E-12D;
    }

    public void zero() {
        this.X = this.Y = this.Z = 0D;
    }

    public void roundToWhole() {
        this.X = Math.round(this.X);
        this.Y = Math.round(this.Y);
        this.Z = Math.round(this.Z);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof Vector) {
            Vector vec = (Vector) other;
            return vec.X == this.X && vec.Y == this.Y && vec.Z == this.Z;
        }
        return false;
    }

    @Override
    public String toString() {
        String coords = new String("<" + this.X + ", " + this.Y + ", " + this.Z + ">");
        return coords;
    }

    public String toRoundedString() {
        String coords = new String("<" + Math.round(this.X * 100.0) / 100.0 + ", " + Math.round(this.Y * 100.0) / 100.0 + ", "
                + Math.round(this.Z * 100.0) / 100.0 + ">");
        return coords;
    }

    public Vector crossAndUnit(Vector v) {
        Vector crossProduct = this.cross(v);
        crossProduct.normalize();
        return crossProduct;
    }

    public void writeToByteBuf(ByteBuf toWrite) {
        toWrite.writeDouble(this.X);
        toWrite.writeDouble(this.Y);
        toWrite.writeDouble(this.Z);
    }

    public void setSubtraction(Vector inLocal, Vector centerCoord) {
        this.X = inLocal.X - centerCoord.X;
        this.Y = inLocal.Y - centerCoord.Y;
        this.Z = inLocal.Z - centerCoord.Z;
    }

    public void transform(double[] rotationMatrix) {
        RotationMatrices.applyTransform(rotationMatrix, this);
    }

    public void setValue(double x, double y, double z) {
        this.X = x;
        this.Y = y;
        this.Z = z;
    }

    public void setValue(Vector toCopy) {
        this.setValue(toCopy.X, toCopy.Y, toCopy.Z);
    }

    public double angleBetween(Vector other) {
        double dotProduct = this.dot(other);
        double normalizedDotProduect = dotProduct / (this.length() * other.length());
        return Math.acos(dotProduct);
    }

    public VectorImmutable toImmutable() {
        return new VectorImmutable(this);
    }
}