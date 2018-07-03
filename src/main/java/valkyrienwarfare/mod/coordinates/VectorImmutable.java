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

package valkyrienwarfare.mod.coordinates;

import io.netty.buffer.ByteBuf;
import valkyrienwarfare.math.Vector;

import javax.annotation.concurrent.Immutable;

/**
 * An immutable version of the Vector class that is wrapping another vector.
 * Used to ensure that the the data used by ISubspace objects is never tampered,
 * and we can therefore consider it completely safe.
 *
 * @author thebest108
 */
@Immutable
public class VectorImmutable {

    private final double x;
    private final double y;
    private final double z;

    public VectorImmutable(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public VectorImmutable(Vector vectorData) {
        this(vectorData.X, vectorData.Y, vectorData.Z);
    }

    public static VectorImmutable readFromByteBuf(ByteBuf bufToRead) {
        return new VectorImmutable(bufToRead.readDouble(), bufToRead.readDouble(), bufToRead.readDouble());
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vector createMutibleVectorCopy() {
        return new Vector(x, y, z);
    }

    public void writeToByteBuf(ByteBuf bufToWrite) {
        bufToWrite.writeDouble(this.getX());
        bufToWrite.writeDouble(this.getY());
        bufToWrite.writeDouble(this.getZ());
    }
}
