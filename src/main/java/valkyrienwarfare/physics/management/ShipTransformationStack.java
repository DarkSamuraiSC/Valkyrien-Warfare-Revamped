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

package valkyrienwarfare.physics.management;

import valkyrienwarfare.network.PhysWrapperPositionMessage;
import valkyrienwarfare.physics.data.ShipTransformData;

/**
 * Used by the client to manage all the transformations sent to it by the server, and queues them for smooth delivery and presentation on screen
 *
 * @author BigBastard
 */
public class ShipTransformationStack {

    public ShipTransformData[] recentTransforms = new ShipTransformData[20];
    // Number of ticks the parent ship has been active for
    // Increases by 1 for every message pushed onto the stack

    public void pushMessage(PhysWrapperPositionMessage toPush) {
        // Shift whole array to the right
        for (int index = recentTransforms.length - 2; index >= 0; index--) {
            recentTransforms[index + 1] = recentTransforms[index];
        }
        recentTransforms[0] = new ShipTransformData(toPush);

    }

    // TODO: Make this auto-adjust to best settings for the server
    public ShipTransformData getDataForTick(int lastTick) {
        if (recentTransforms[0] == null) {
            System.err.println("A SHIP JUST RETURNED NULL FOR 'recentTransforms[0]==null'; ANY WEIRD ERRORS PAST HERE ARE DIRECTLY LINKED TO THAT!");
            return null;
        }
        int tickToGet = lastTick + 1;

        int realtimeTick = recentTransforms[0].relativeTick;

        if (realtimeTick - lastTick > 3) {
            tickToGet = realtimeTick - 2;
//			System.out.println("Too Slow");
        }

        for (ShipTransformData transform : recentTransforms) {
            if (transform != null) {
                if (transform.relativeTick == tickToGet) {
                    return transform;
                }
            }
        }

//		System.out.println("Couldnt find the needed transform");

        if (recentTransforms[1] != null) {
            return recentTransforms[1];
        }

        return recentTransforms[0];
    }

}
