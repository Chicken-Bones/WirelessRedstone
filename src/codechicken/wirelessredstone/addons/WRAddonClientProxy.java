package codechicken.wirelessredstone.addons;

import codechicken.core.ClientUtils;
import codechicken.wirelessredstone.core.SaveManager;
import codechicken.wirelessredstone.core.WRCoreCPH;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.MinecraftForgeClient;

import static codechicken.wirelessredstone.addons.WirelessRedstoneAddons.*;

public class WRAddonClientProxy extends WRAddonProxy
{
    @Override
    public void init() {
        super.init();
        ClientUtils.enhanceSupportersList("WR-CBE|Addons");

        WRCoreCPH.delegates.add(new WRAddonCPH());

        GuiWirelessSniffer.loadColours(SaveManager.config().getTag("addon"));
        MinecraftForgeClient.registerItemRenderer(tracker, new RenderTracker());
        MinecraftForgeClient.registerItemRenderer(wirelessMap, new WirelessMapRenderer());

        RenderingRegistry.registerEntityRenderingHandler(EntityREP.class, new RenderSnowball(rep));
        RenderingRegistry.registerEntityRenderingHandler(EntityWirelessTracker.class, new RenderTracker());
    }

    @Override
    public void openSnifferGui(EntityPlayer player) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiWirelessSniffer());
    }

    public void openPSnifferGui(EntityPlayer player) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiPrivateSniffer());
    }
}
