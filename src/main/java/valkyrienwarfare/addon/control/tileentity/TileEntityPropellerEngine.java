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

package valkyrienwarfare.addon.control.tileentity;

import valkyrienwarfare.addon.control.nodenetwork.BasicForceNodeTileEntity;
import valkyrienwarfare.math.Vector;

public class TileEntityPropellerEngine extends BasicForceNodeTileEntity {

    private double propellerAngle;
    private double prevPropellerAngle;

    public TileEntityPropellerEngine(Vector normalVeclocityUnoriented, boolean isForceOutputOriented, double maxThrust) {
        super(normalVeclocityUnoriented, isForceOutputOriented, maxThrust);
        this.propellerAngle = Math.random() * 90D;
        this.prevPropellerAngle = this.propellerAngle;
    }

    public TileEntityPropellerEngine() {
        this.propellerAngle = Math.random() * 90D;
        this.prevPropellerAngle = this.propellerAngle;
    }

    public double getPropellerAngle(double partialTicks) {
        double delta = this.propellerAngle - this.prevPropellerAngle;
        if (Math.abs(delta) > 180D) {
            delta %= 180D;
            delta += 180D;
        }
        return this.prevPropellerAngle + delta * partialTicks;
    }

    @Override
    public void update() {
        super.update();
        this.prevPropellerAngle = this.propellerAngle;
        this.propellerAngle += 25D;
        this.propellerAngle %= 360D;
    }
}
