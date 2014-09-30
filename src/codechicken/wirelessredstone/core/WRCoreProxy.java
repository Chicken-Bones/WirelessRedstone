package codechicken.wirelessredstone.core;

import codechicken.lib.packet.PacketCustom;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import codechicken.lib.config.ConfigTag;
import cpw.mods.fml.common.registry.GameRegistry;

import static codechicken.wirelessredstone.core.WirelessRedstoneCore.*;

public class WRCoreProxy
{
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(new WRCoreEventHandler());
        FMLCommonHandler.instance().bus().register(new WRCoreEventHandler());
    }

    public void init() {
        PacketCustom.assignHandler(channel, new WRCoreSPH());

        obsidianStick = createItem("obsidianStick");
        stoneBowl = createItem("stoneBowl");
        retherPearl = createItem("retherPearl");
        wirelessTransceiver = createItem("wirelessTransceiver");
        blazeTransceiver = createItem("blazeTransceiver");
        recieverDish = createItem("recieverDish");
        blazeRecieverDish = createItem("blazeRecieverDish");

        ConfigTag coreconfig = SaveManager.config().getTag("core").useBraces().setPosition(10);
        WirelessBolt.init(coreconfig);
        damagebolt = new DamageSource("bolt");

        addRecipies();
    }

    private Item createItem(String name) {
        Item item = new Item().setUnlocalizedName("wrcbe_core:" + name).setTextureName("wrcbe_core:"+name).setCreativeTab(CreativeTabs.tabMaterials);
        GameRegistry.registerItem(item, name);
        return item;
    }

    private void addRecipies() {
        GameRegistry.addRecipe(new ItemStack(obsidianStick, 2),
                "O",
                "O",
                'O', Blocks.obsidian);

        OreDictionary.registerOre("obsidianRod", new ItemStack(obsidianStick));
        OreDictionary.registerOre("stoneBowl", new ItemStack(stoneBowl));

        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(wirelessTransceiver),
                "r",
                "o",
                'r', retherPearl,
                'o', "obsidianRod"));

        GameRegistry.addRecipe(new ItemStack(stoneBowl),
                "S S",
                " S ",
                'S', Blocks.stone);
        GameRegistry.addRecipe(new ItemStack(retherPearl),
                "rgr",
                "gpg",
                "rgr",
                'r', Items.redstone,
                'g', Items.glowstone_dust,
                'p', Items.ender_pearl);
        GameRegistry.addRecipe(new ItemStack(retherPearl),
                "grg",
                "rpr",
                "grg",
                'r', Items.redstone,
                'g', Items.glowstone_dust,
                'p', Items.ender_pearl);
        GameRegistry.addRecipe(new ItemStack(blazeTransceiver),
                "r",
                "b",
                'r', retherPearl,
                'b', Items.blaze_rod);
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(recieverDish),
                "t",
                "b",
                't', wirelessTransceiver,
                'b', "stoneBowl"));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blazeRecieverDish),
                "t",
                "b",
                't', blazeTransceiver,
                'b', "stoneBowl"));
    }

    public void openItemWirelessGui(EntityPlayer entityplayer) {
    }

    public void openTileWirelessGui(EntityPlayer entityplayer, ITileWireless tileRPWireless) {
    }
}
