package codechicken.wirelessredstone.core;

import codechicken.core.launch.CodeChickenCorePlugin;
import net.minecraft.command.CommandHandler;
import net.minecraft.util.DamageSource;
import net.minecraft.item.Item;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = "WR-CBE|Core", dependencies = "required-after:CodeChickenCore@[" + CodeChickenCorePlugin.version + ",);" +
        "required-after:ForgeMultipart")//have to make sure it's before all mods within this container until FML fixes proxy injection.
public class WirelessRedstoneCore
{
    public static Item obsidianStick;
    public static Item stoneBowl;
    public static Item retherPearl;
    public static Item wirelessTransceiver;
    public static Item blazeTransceiver;
    public static Item recieverDish;
    public static Item blazeRecieverDish;

    public static DamageSource damagebolt;
    public static final String channel = "WRCBE";

    @SidedProxy(clientSide = "codechicken.wirelessredstone.core.WRCoreClientProxy",
            serverSide = "codechicken.wirelessredstone.core.WRCoreProxy")
    public static WRCoreProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        CommandHandler commandManager = (CommandHandler) event.getServer().getCommandManager();
        commandManager.registerCommand(new CommandFreq());
    }
}
