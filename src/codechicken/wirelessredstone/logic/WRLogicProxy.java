package codechicken.wirelessredstone.logic;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultipartGenerator;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.MultiPartRegistry.IPartFactory;
import codechicken.wirelessredstone.core.WirelessRedstoneCore;

import cpw.mods.fml.common.registry.GameRegistry;

import static codechicken.wirelessredstone.logic.WirelessRedstoneLogic.*;

public class WRLogicProxy implements IPartFactory
{
    public void init()
    {  
        MultiPartRegistry.registerParts(this, new String[]{
                "wrcbe-tran",
                "wrcbe-recv",
                "wrcbe-jamm"
        });

        MultipartGenerator.registerPassThroughInterface("codechicken.wirelessredstone.core.ITileWireless");
        MultipartGenerator.registerPassThroughInterface("codechicken.wirelessredstone.core.ITileReceiver");
        MultipartGenerator.registerPassThroughInterface("codechicken.wirelessredstone.core.ITileJammer");
        //until CC proper integration
        //MultipartGenerator.registerPassThroughInterface("dan200.computer.api.IPeripheral");
        
        itemwireless = new ItemWirelessPart().setCreativeTab(CreativeTabs.tabRedstone);
        GameRegistry.registerItem(itemwireless, "wirelessLogic");

        addRecipies();
    }
    
    private static void addRecipies()
    {
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(itemwireless, 1, 0),
                "t  ",
                "srr",
                "fff",
                't', WirelessRedstoneCore.wirelessTransceiver,
                's', "obsidianRod",
                'f', new ItemStack(Blocks.stone_slab, 1, 0),
                'r', Items.redstone));

        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(itemwireless, 1, 1),
                "d  ",
                "srr",
                "fff",
                'd', WirelessRedstoneCore.recieverDish,
                's', "obsidianRod",
                'f', new ItemStack(Blocks.stone_slab, 1, 0),
                'r', Items.redstone));
        
        GameRegistry.addRecipe(new ItemStack(itemwireless, 1, 2),
                "p  ",
                "srr",
                "fff",
                'p', WirelessRedstoneCore.blazeTransceiver,
                's', Items.blaze_rod,
                'f', new ItemStack(Blocks.stone_slab, 1, 0),
                'r', Items.redstone);
    }
    
    @Override
    public TMultiPart createPart(String name, boolean client)
    {
        if(name.equals("wrcbe-tran"))
            return new TransmitterPart();
        if(name.equals("wrcbe-recv"))
            return new ReceiverPart();
        if(name.equals("wrcbe-jamm"))
            return new JammerPart();
        return null;
    }
}
