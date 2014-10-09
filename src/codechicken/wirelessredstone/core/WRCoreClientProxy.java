package codechicken.wirelessredstone.core;

import codechicken.core.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import codechicken.core.CCUpdateChecker;
import codechicken.lib.packet.PacketCustom;
import cpw.mods.fml.common.Mod;

import static codechicken.wirelessredstone.core.WirelessRedstoneCore.*;

public class WRCoreClientProxy extends WRCoreProxy
{
    @Override
    public void init()
    {
        if(SaveManager.config().getTag("checkUpdates").getBooleanValue(true))
            CCUpdateChecker.updateCheck("WR-CBE", WirelessRedstoneCore.class.getAnnotation(Mod.class).version());
        ClientUtils.enhanceSupportersList("WR-CBE|Core");
        
        super.init();

        PacketCustom.assignHandler(channel, new WRCoreCPH());
    }

    public void openItemWirelessGui(EntityPlayer entityplayer)
    {
        Minecraft.getMinecraft().displayGuiScreen(new GuiRedstoneWireless(entityplayer.inventory));
    }

    public void openTileWirelessGui(EntityPlayer entityplayer, ITileWireless tile)
    {
        Minecraft.getMinecraft().displayGuiScreen(new GuiRedstoneWireless(entityplayer.inventory, tile));
    }
}
