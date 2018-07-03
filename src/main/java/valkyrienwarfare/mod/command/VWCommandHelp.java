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

package valkyrienwarfare.mod.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import valkyrienwarfare.mod.multithreaded.VWThread;
import valkyrienwarfare.mod.multithreaded.VWThreadManager;

import java.util.ArrayList;
import java.util.List;

public class VWCommandHelp extends CommandBase {

    public static final List<String> COMMANDS = new ArrayList<String>();

    static {
        COMMANDS.add("/physsettings");
        COMMANDS.add("/airshipsettings");
        COMMANDS.add("/airshipmappings");
        COMMANDS.add("/vw tps");
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "vw";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        // TODO Auto-generated method stub
        return "/vw       See entire list of commands for Valkyrien Warfare";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString("All ValkyrienWarfare Commands"));

            for (String command : COMMANDS) {
                sender.sendMessage(new TextComponentString(command));
            }

            sender.sendMessage(new TextComponentString("To see avaliable subcommands, type /command help"));
        } else if (args.length == 1 && args[0].equals("tps")) {
            World world = sender.getEntityWorld();
            VWThread worldPhysicsThread = VWThreadManager.getVWThreadForWorld(world);
            if (worldPhysicsThread != null) {
                long averagePhysTickTimeNano = worldPhysicsThread.getAveragePhysicsTickTimeNano();
                double ticksPerSecond = 1000000000D / ((double) averagePhysTickTimeNano);
                double ticksPerSecondTwoDecimals = Math.floor(ticksPerSecond * 100) / 100;
                sender.sendMessage(new TextComponentString("Player world: " + ticksPerSecondTwoDecimals + " physics ticks per second"));
            }
        }
    }

}
