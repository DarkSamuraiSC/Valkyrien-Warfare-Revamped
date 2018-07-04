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

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import valkyrienwarfare.ValkyrienWarfareMod;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.mod.multithreaded.VWThreadManager;
import valkyrienwarfare.util.PhysicsSettings;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PhysSettingsCommand extends CommandBase {

    public static final List<String> COMPLETED_OPTIONS = new ArrayList<>();

    static {
        COMPLETED_OPTIONS.add("gravityvector");
        COMPLETED_OPTIONS.add("maxshipsize");
        COMPLETED_OPTIONS.add("physicsspeed");
        COMPLETED_OPTIONS.add("dogravity");
        COMPLETED_OPTIONS.add("dophysicsblocks");
        COMPLETED_OPTIONS.add("doairshiprotation");
        COMPLETED_OPTIONS.add("doairshipmovement");
        COMPLETED_OPTIONS.add("save");
        COMPLETED_OPTIONS.add("doetheriumlifting");
        COMPLETED_OPTIONS.add("restartcrashedphysics");
    }

    @Override
    public String getName() {
        return "physsettings";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/physsettings <setting name> [value]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString("Avaliable physics Commands:"));
            for (String command : COMPLETED_OPTIONS) {
                sender.sendMessage(new TextComponentString(command));
            }
            return;
        }
        String key = args[0];
        if ("maxshipsize".equals(key)) {
            if (args.length == 1) {
                sender.sendMessage(new TextComponentString("maxshipsize=" + ValkyrienWarfareMod.maxShipSize + " (Default: 15000)"));
                return;
            } else if (args.length == 2) {
                int value = Integer.parseInt(args[1]);
                ValkyrienWarfareMod.maxShipSize = value;
                sender.sendMessage(new TextComponentString("Set maximum ship size to " + value));
                return;
            }
        } else if ("gravityvector".equals(key)) {
            switch (args.length) {
                case 1:
                    sender.sendMessage(new TextComponentString("gravityvector=" + ValkyrienWarfareMod.gravity.toRoundedString() + " (Default: <0,-9.8,0>)"));
                    return;
                case 4:
                    Vector newVector = new Vector(0, -9.8, 0);
                    try {
                        if (args[1] != null && args[2] != null && args[3] != null) {
                            newVector.X = Double.parseDouble(args[1]);
                            newVector.Y = Double.parseDouble(args[2]);
                            newVector.Z = Double.parseDouble(args[3]);
                        } else {
                            sender.sendMessage(new TextComponentString("Usage: /physsettings gravityVector <x> <y> <z>"));
                            return;
                        }
                    } catch (Exception e) {
                    }
                    ValkyrienWarfareMod.gravity = newVector;
                    sender.sendMessage(new TextComponentString("physics gravity set to " + newVector.toRoundedString() + " (Default: <0,-9.8,0>)"));
                    return;
                default:
                    sender.sendMessage(new TextComponentString("Usage: /physsettings gravityVector <x> <y> <z>"));
                    break;
            }
        } else if ("physicsspeed".equals(key)) {
            if (args.length == 1) {
                sender.sendMessage(new TextComponentString("physicsspeed=" + ValkyrienWarfareMod.physSpeed + " (Default: 100%)"));
                return;
            } else if (args.length == 2) {
                double value = Double.parseDouble(args[1].replace('%', ' '));
                if (value < 0 || value > 1000) {
                    sender.sendMessage(new TextComponentString("Please enter a value between 0 and 1000"));
                    return;
                }
                ValkyrienWarfareMod.physSpeed = value / 10000D;
                sender.sendMessage(new TextComponentString("Set physicsspeed to " + value + " percent"));
                return;
            }
        } else if ("dogravity".equals(key)) {
            if (args.length == 1) {
                sender.sendMessage(new TextComponentString("dogravity=" + PhysicsSettings.doGravity + " (Default: true)"));
                return;
            } else if (args.length == 2) {
                PhysicsSettings.doGravity = Boolean.parseBoolean(args[1]);
                sender.sendMessage(new TextComponentString("Set dogravity to " + (PhysicsSettings.doGravity ? "enabled" : "disabled")));
                return;
            }
        } else if ("dophysicsblocks".equals(key)) {
            if (args.length == 1) {
                sender.sendMessage(new TextComponentString("dophysicsblocks=" + PhysicsSettings.doPhysicsBlocks + " (Default: true)"));
                return;
            } else if (args.length == 2) {
                PhysicsSettings.doPhysicsBlocks = Boolean.parseBoolean(args[1]);
                sender.sendMessage(new TextComponentString("Set dophysicsblocks to " + (PhysicsSettings.doPhysicsBlocks ? "enabled" : "disabled")));
                return;
            }
        } else if ("doairshiprotation".equals(key)) {
            if (args.length == 1) {
                sender.sendMessage(new TextComponentString("doairshiprotation=" + PhysicsSettings.doAirshipRotation + " (Default: true)"));
                return;
            } else if (args.length == 2) {
                PhysicsSettings.doAirshipRotation = Boolean.parseBoolean(args[1]);
                sender.sendMessage(new TextComponentString("Set doairshiprotation to " + (PhysicsSettings.doAirshipRotation ? "enabled" : "disabled")));
                return;
            }
        } else if ("doairshipmovement".equals(key)) {
            if (args.length == 1) {
                sender.sendMessage(new TextComponentString("doairshipmovement=" + PhysicsSettings.doAirshipMovement + " (Default: true)"));
                return;
            } else if (args.length == 2) {
                PhysicsSettings.doAirshipMovement = Boolean.parseBoolean(args[1]);
                sender.sendMessage(new TextComponentString("Set doairshipmovement to " + (PhysicsSettings.doAirshipMovement ? "enabled" : "disabled")));
                return;
            }
        } else if ("doetheriumlifting".equals(key)) {
            if (args.length == 1) {
                sender.sendMessage(new TextComponentString("doetheriumlifting=" + PhysicsSettings.doEtheriumLifting + " (Default: true)"));
                return;
            } else if (args.length == 2) {
                PhysicsSettings.doEtheriumLifting = Boolean.parseBoolean(args[1]);
                sender.sendMessage(new TextComponentString("Set doetheriumlifting to " + (PhysicsSettings.doEtheriumLifting ? "enabled" : "disabled")));
                return;
            }
        } else if ("save".equals(key)) {
            ValkyrienWarfareMod.INSTANCE.saveConfig();
            sender.sendMessage(new TextComponentString("Saved phyisics settings"));
            return;
        } else if ("restartcrashedphysics".equals(key)) {
            List<World> crashedWorlds = VWThreadManager.restartCrashedPhysicsThreads();
            sender.sendMessage(new TextComponentString("Restart physics threads for " + crashedWorlds.size() + " worlds."));
            return;
        } else if (true || "help".equals(key)) {
            sender.sendMessage(new TextComponentString("Avaliable physics Commands:"));
            for (String command : COMPLETED_OPTIONS) {
                sender.sendMessage(new TextComponentString(command));
            }
        }

        sender.sendMessage(new TextComponentString(this.getUsage(sender)));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            List<String> possibleArgs = new ArrayList<>(COMPLETED_OPTIONS);
            //Don't like this, but I have to because concurrentmodificationexception
            possibleArgs.removeIf(s -> !s.startsWith(args[0]));
            return possibleArgs;
        } else if (args.length == 2) {
            if (args[0].startsWith("do")) {
                if (args[1].startsWith("t")) {
                    return Lists.newArrayList("true");
                } else if (args[1].startsWith("f")) {
                    return Lists.newArrayList("false");
                } else {
                    return Lists.newArrayList("true", "false");
                }
            }
        }

        return null;
    }
}
