package codechicken.wirelessredstone.addons;

import codechicken.core.CommonUtils;
import codechicken.wirelessredstone.core.WRCoreSPH;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import codechicken.wirelessredstone.core.WirelessRedstoneCore;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

import static codechicken.wirelessredstone.addons.WirelessRedstoneAddons.*;

public class WRAddonProxy
{
    public void preInit()
    {
        MinecraftForge.EVENT_BUS.register(new WRAddonEventHandler());
    }

    private <T extends Item> T register(T item, String name) {
        item.setUnlocalizedName("wrcbe_addons:"+name).setTextureName("wrcbe_addons:"+name);
        item.setCreativeTab(CreativeTabs.tabRedstone);
        GameRegistry.registerItem(item, name);
        return item;
    }

    public void init()
    {
        WRCoreSPH.delegates.add(new WRAddonSPH());
        FMLCommonHandler.instance().bus().register(new WRAddonEventHandler());

        triangulator = register(new ItemWirelessTriangulator(), "triangulator");
        remote = register(new ItemWirelessRemote(), "remote");
        sniffer = register(new ItemWirelessSniffer(), "sniffer");
        emptyWirelessMap = register(new ItemEmptyWirelessMap(), "empty_map");
        wirelessMap = register(new ItemWirelessMap(), "map");
        tracker = register(new ItemWirelessTracker(), "tracker");
        rep = register(new ItemREP(), "rep");
        psniffer = register(new ItemPrivateSniffer(), "psniffer");

        CommonUtils.registerHandledEntity(EntityWirelessTracker.class, "WRTracker");
        
        addRecipes();
    }
    
    private void addRecipes()
    {
        GameRegistry.addRecipe(new ItemStack(triangulator), " i ",
                "iti",
                " i ",
                'i', Items.iron_ingot,
                't', WirelessRedstoneCore.wirelessTransceiver);
        
        GameRegistry.addRecipe(new ItemStack(remote), "t",
                "B",
                'B', Blocks.stone_button,
                't', WirelessRedstoneCore.wirelessTransceiver);
        
        GameRegistry.addRecipe(new ItemStack(sniffer), "dtd",
                "rBr",
                "SSS",
                'd', WirelessRedstoneCore.recieverDish,
                't', WirelessRedstoneCore.wirelessTransceiver,
                'r', Items.redstone,
                'B', Blocks.stone_button,
                'S', Blocks.stone);
        
        GameRegistry.addRecipe(new ItemStack(wirelessMap, 1), "ppp",
                "ptp",
                "ppp",
                'p', Items.paper,
                't', new ItemStack(triangulator, 1, OreDictionary.WILDCARD_VALUE));
        
        GameRegistry.addRecipe(new ItemStack(rep), " Ot",
                "OpO",
                "tO ",
                'p', WirelessRedstoneCore.retherPearl,
                't', WirelessRedstoneCore.blazeTransceiver,
                'r', Items.redstone,
                'g', Items.glowstone_dust,
                'O', Blocks.obsidian);

        GameRegistry.addRecipe(new ItemStack(tracker, 1), " p ",
                "OOO",
                " s ",
                'p', WirelessRedstoneCore.retherPearl,
                's', Items.slime_ball,
                'O', Blocks.obsidian);

        GameRegistry.addRecipe(new ItemStack(psniffer), "dtd",
                "rBr",
                "SSS",
                'd', WirelessRedstoneCore.blazeRecieverDish,
                't', WirelessRedstoneCore.blazeTransceiver,
                'r', Items.redstone,
                'B', Blocks.stone_button,
                'S', Blocks.stone);
    }
    
    public void openPSnifferGui(EntityPlayer player)
    {
    }

    public void openSnifferGui(EntityPlayer player)
    {
    }
}
