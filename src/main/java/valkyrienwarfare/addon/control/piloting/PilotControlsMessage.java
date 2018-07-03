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

package valkyrienwarfare.addon.control.piloting;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import valkyrienwarfare.VWKeyHandler;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;

import java.util.UUID;

public class PilotControlsMessage implements IMessage {

    public static boolean airshipUp_KeyPressedLast;
    public static boolean airshipDown_KeyPressedLast;
    public static boolean airshipForward_KeyPressedLast;
    public static boolean airshipBackward_KeyPressedLast;
    public static boolean airshipLeft_KeyPressedLast;
    public static boolean airshipRight_KeyPressedLast;
    public static boolean airshipStop_KeyPressedLast;
    private static UUID defaultUUID = new UUID(0, 0);
    public boolean airshipUp_KeyDown;
    public boolean airshipDown_KeyDown;
    public boolean airshipForward_KeyDown;
    public boolean airshipBackward_KeyDown;
    public boolean airshipLeft_KeyDown;
    public boolean airshipRight_KeyDown;
    public boolean airshipSprinting;
    public boolean airshipStop_KeyDown;
    public boolean airshipUp_KeyPressed;
    public boolean airshipDown_KeyPressed;
    public boolean airshipForward_KeyPressed;
    public boolean airshipBackward_KeyPressed;
    public boolean airshipLeft_KeyPressed;
    public boolean airshipRight_KeyPressed;
    public boolean airshipStop_KeyPressed;
    public Enum inputType;
    public UUID shipFor = defaultUUID;
    public BlockPos controlBlockPos;

    public PilotControlsMessage() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer packetBuf = new PacketBuffer(buf);

        {
            byte b = packetBuf.readByte();
            this.airshipUp_KeyDown = (b & 1) == 0;
            this.airshipDown_KeyDown = ((b >> 1) & 1) == 0;
            this.airshipForward_KeyDown = ((b >> 2) & 1) == 0;
            this.airshipBackward_KeyDown = ((b >> 3) & 1) == 0;
            this.airshipLeft_KeyDown = ((b >> 4) & 1) == 0;
            this.airshipRight_KeyDown = ((b >> 5) & 1) == 0;
            this.airshipSprinting = ((b >> 6) & 1) == 0;
            this.airshipStop_KeyDown = ((b >> 7) & 1) == 0;
        }

        {
            byte b = packetBuf.readByte();
            this.airshipUp_KeyPressed = (b & 1) == 0;
            this.airshipDown_KeyPressed = ((b >> 1) & 1) == 0;
            this.airshipForward_KeyPressed = ((b >> 2) & 1) == 0;
            this.airshipBackward_KeyPressed = ((b >> 3) & 1) == 0;
            this.airshipLeft_KeyPressed = ((b >> 4) & 1) == 0;
            this.airshipRight_KeyPressed = ((b >> 5) & 1) == 0;
            this.airshipStop_KeyPressed = ((b >> 6) & 1) == 0;
            //ignore most significant byte
        }

        this.inputType = packetBuf.readEnumValue(ControllerInputType.class);
        this.shipFor = packetBuf.readUniqueId();
        this.controlBlockPos = packetBuf.readBlockPos();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer packetBuf = new PacketBuffer(buf);

        {
            int i = 0;
            i |= this.airshipUp_KeyDown ? 0 : 1;
            i |= (this.airshipDown_KeyDown ? 0 : 1) << 1;
            i |= (this.airshipForward_KeyDown ? 0 : 1) << 2;
            i |= (this.airshipBackward_KeyDown ? 0 : 1) << 3;
            i |= (this.airshipLeft_KeyDown ? 0 : 1) << 4;
            i |= (this.airshipRight_KeyDown ? 0 : 1) << 5;
            i |= (this.airshipSprinting ? 0 : 1) << 6;
            i |= (this.airshipStop_KeyDown ? 0 : 1) << 7;
            packetBuf.writeByte(i);
        }

        {
            int i = 0;
            i |= this.airshipUp_KeyPressed ? 0 : 1;
            i |= (this.airshipDown_KeyPressed ? 0 : 1) << 1;
            i |= (this.airshipForward_KeyPressed ? 0 : 1) << 2;
            i |= (this.airshipBackward_KeyPressed ? 0 : 1) << 3;
            i |= (this.airshipLeft_KeyPressed ? 0 : 1) << 4;
            i |= (this.airshipRight_KeyPressed ? 0 : 1) << 5;
            i |= (this.airshipStop_KeyPressed ? 0 : 1) << 6;
            packetBuf.writeByte(i);
        }

        packetBuf.writeEnumValue(this.inputType);
        packetBuf.writeUniqueId(this.shipFor);
        if (this.controlBlockPos == null) {
            System.out.println(":(");
            this.controlBlockPos = BlockPos.ORIGIN;
        }
        packetBuf.writeBlockPos(this.controlBlockPos);
    }

    public void assignKeyBooleans(PhysicsWrapperEntity shipPiloting, Enum inputType) {
        this.airshipUp_KeyDown = VWKeyHandler.airshipUp.isKeyDown();
        this.airshipDown_KeyDown = VWKeyHandler.airshipDown.isKeyDown();
        this.airshipForward_KeyDown = VWKeyHandler.airshipForward.isKeyDown();
        this.airshipBackward_KeyDown = VWKeyHandler.airshipBackward.isKeyDown();
        this.airshipLeft_KeyDown = VWKeyHandler.airshipLeft.isKeyDown();
        this.airshipRight_KeyDown = VWKeyHandler.airshipRight.isKeyDown();
        this.airshipSprinting = VWKeyHandler.airshipSpriting.isKeyDown(); // Minecraft.getMinecraft().player.isSprinting();

        this.airshipUp_KeyPressed = this.airshipUp_KeyDown && !airshipUp_KeyPressedLast;
        this.airshipDown_KeyPressed = this.airshipDown_KeyDown && !airshipDown_KeyPressedLast;
        this.airshipForward_KeyPressed = this.airshipForward_KeyDown && !airshipForward_KeyPressedLast;
        this.airshipBackward_KeyPressed = this.airshipBackward_KeyDown && !airshipBackward_KeyPressedLast;
        this.airshipLeft_KeyPressed = this.airshipLeft_KeyDown && !airshipLeft_KeyPressedLast;
        this.airshipRight_KeyPressed = this.airshipRight_KeyDown && !airshipRight_KeyPressedLast;
        this.airshipStop_KeyPressed = this.airshipStop_KeyDown && !airshipStop_KeyPressedLast;

        if (shipPiloting != null) {
            this.shipFor = shipPiloting.getUniqueID();
        }
        this.inputType = inputType;
        if (inputType == ControllerInputType.Zepplin) {
            this.airshipUp_KeyDown = VWKeyHandler.airshipUp_Zepplin.isKeyDown();
            this.airshipDown_KeyDown = VWKeyHandler.airshipDown_Zepplin.isKeyDown();
            this.airshipForward_KeyDown = VWKeyHandler.airshipForward_Zepplin.isKeyDown();
            this.airshipBackward_KeyDown = VWKeyHandler.airshipBackward_Zepplin.isKeyDown();
            this.airshipLeft_KeyDown = VWKeyHandler.airshipLeft_Zepplin.isKeyDown();
            this.airshipRight_KeyDown = VWKeyHandler.airshipRight_Zepplin.isKeyDown();
            this.airshipStop_KeyDown = VWKeyHandler.airshipStop_Zepplin.isKeyDown();

            this.airshipUp_KeyPressed = this.airshipUp_KeyDown && !airshipUp_KeyPressedLast;
            this.airshipDown_KeyPressed = this.airshipDown_KeyDown && !airshipDown_KeyPressedLast;
            this.airshipForward_KeyPressed = this.airshipForward_KeyDown && !airshipForward_KeyPressedLast;
            this.airshipBackward_KeyPressed = this.airshipBackward_KeyDown && !airshipBackward_KeyPressedLast;
            this.airshipLeft_KeyPressed = this.airshipLeft_KeyDown && !airshipLeft_KeyPressedLast;
            this.airshipRight_KeyPressed = this.airshipRight_KeyDown && !airshipRight_KeyPressedLast;
            this.airshipStop_KeyPressed = this.airshipStop_KeyDown && !airshipStop_KeyPressedLast;
        }

        airshipUp_KeyPressedLast = this.airshipUp_KeyDown;
        airshipDown_KeyPressedLast = this.airshipDown_KeyDown;
        airshipForward_KeyPressedLast = this.airshipForward_KeyDown;
        airshipBackward_KeyPressedLast = this.airshipBackward_KeyDown;
        airshipLeft_KeyPressedLast = this.airshipLeft_KeyDown;
        airshipRight_KeyPressedLast = this.airshipRight_KeyDown;
        airshipStop_KeyPressedLast = this.airshipStop_KeyDown;
    }

}
