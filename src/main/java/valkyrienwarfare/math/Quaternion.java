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

/**
 * Used for rendering interpolation and applying angular velocity to the physics controller
 *
 * @author thebest108
 */
public class Quaternion {

    private double x, y, z, w;

    public Quaternion(double xx, double yy, double zz, double ww) {
        this.x = xx;
        this.y = yy;
        this.z = zz;
        this.w = ww;
    }

    private Quaternion() {
    }

    public static Quaternion QuaternionFromMatrix(double[] matrix) {
        Quaternion q = new Quaternion();
        q.w = Math.sqrt(Math.max(0, 1 + matrix[0] + matrix[5] + matrix[10])) / 2;
        q.x = Math.sqrt(Math.max(0, 1 + matrix[0] - matrix[5] - matrix[10])) / 2;
        q.y = Math.sqrt(Math.max(0, 1 - matrix[0] + matrix[5] - matrix[10])) / 2;
        q.z = Math.sqrt(Math.max(0, 1 - matrix[0] - matrix[5] + matrix[10])) / 2;
        q.x *= Math.signum(q.x * (matrix[6] - matrix[9]));
        q.y *= Math.signum(q.y * (matrix[8] - matrix[2]));
        q.z *= Math.signum(q.z * (matrix[1] - matrix[4]));
        return q;
    }

    public static Quaternion slerpInterpolate(Quaternion old, Quaternion newOne, double timeStep) {
        double dotProduct = dotProduct(old, newOne);
        boolean makeNegative = dotProduct < 0;
        if (makeNegative) {
            old.x *= -1;
            old.y *= -1;
            old.z *= -1;
            old.w *= -1;
            dotProduct *= -1;
        }
        double betweenAngle = Math.acos(dotProduct);
        double sinMod = Math.sin((float) betweenAngle);
        double oldMod = 1.0D - timeStep;
        double newMod = timeStep;
        if (Math.abs(sinMod) > 0) {
            oldMod = Math.sin(oldMod * betweenAngle) / sinMod;
            newMod = Math.sin(timeStep * betweenAngle) / sinMod;
        }
        Quaternion betweenQuat = new Quaternion(old.x * oldMod + newOne.x * newMod, old.y * oldMod + newOne.y * newMod, old.z * oldMod + newOne.z * newMod, old.w * oldMod + newOne.w * newMod);
        double betweenLength = Math.sqrt(betweenQuat.x * betweenQuat.x + betweenQuat.y * betweenQuat.y + betweenQuat.z * betweenQuat.z + betweenQuat.w * betweenQuat.w);
        betweenQuat.x /= betweenLength;
        betweenQuat.y /= betweenLength;
        betweenQuat.z /= betweenLength;
        betweenQuat.w /= betweenLength;
        if (makeNegative) {
            old.x *= -1;
            old.y *= -1;
            old.z *= -1;
            old.w *= -1;
        }
        return betweenQuat;
    }

    /**
     * Creates a new Quaternion object for the given rotation.
     *
     * @param pitch in degrees
     * @param yaw   in degrees
     * @param roll  in degrees
     * @return
     */
    public static Quaternion fromEuler(double pitch, double yaw, double roll) {
        double[] rotationMatrix = RotationMatrices.getRotationMatrix(pitch, yaw, roll);
        return QuaternionFromMatrix(rotationMatrix);
    }

    public static double dotProduct(Quaternion first, Quaternion second) {
        return (first.x * second.x) + (first.y * second.y) + (first.z * second.z) + (first.w * second.w);
    }

    public double[] toRadians() {
        // double test = x*y + z*w;
        double sqw = this.w * this.w;
        double sqx = this.x * this.x;
        double sqy = this.y * this.y;
        double sqz = this.z * this.z;
        double pitch = -Math.atan2(2.0 * (this.y * this.z + this.x * this.w), (-sqx - sqy + sqz + sqw));
        double yaw = -Math.asin(-2.0 * (this.x * this.z - this.y * this.w) / (sqx + sqy + sqz + sqw));
        double roll = -Math.atan2(2.0 * (this.x * this.y + this.z * this.w), (sqx - sqy - sqz + sqw));
        sqw = this.x * this.y + this.z * this.w;
        if (sqw > .9) {
            System.out.println("Quaternion singularity at North Pole");
            roll = 2 * Math.atan2(this.x, this.w);
            yaw = Math.PI / 2;
            pitch = 0;
        }
        if (sqw < -.9) {
            System.out.println("Quaternion singularity at South Pole");
            roll = -2 * Math.atan2(this.x, this.w);
            yaw = -Math.PI / 2;
            pitch = 0;
        }
        return new double[]{pitch, yaw, roll};
    }

    public void multiply(Quaternion q1) {
        double oldw = this.w;
        double oldx = this.x;
        double oldy = this.y;
        double oldz = this.z;
        this.w = oldw * q1.w - oldx * q1.x - oldy * q1.y - oldz * q1.z;
        this.x = oldw * q1.x + q1.w * oldx + oldy * q1.z - oldz * q1.y;
        this.y = oldw * q1.y + q1.w * oldy - oldx * q1.z + oldz * q1.x;
        this.z = oldw * q1.z + q1.w * oldz + oldx * q1.y - oldy * q1.x;
        oldw = this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w;
        if (Math.abs(1D - oldw) > .001) {
            oldw = Math.sqrt(oldw);
            this.w /= oldw;
            this.x /= oldw;
            this.y /= oldw;
            this.z /= oldw;
        }
    }

}