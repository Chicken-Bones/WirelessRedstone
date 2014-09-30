package codechicken.wirelessredstone.logic;

import java.util.List;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.JItemMultiPart;
import codechicken.multipart.TMultiPart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class ItemWirelessPart extends JItemMultiPart
{
    public ItemWirelessPart() {
        setHasSubtypes(true);
        setUnlocalizedName("wrcbe_logic:wirelesspart");
    }

    @Override
    public TMultiPart newPart(ItemStack item, EntityPlayer player, World world, BlockCoord pos, int side, Vector3 vhit) {
        BlockCoord onPos = pos.copy().offset(side ^ 1);
        if (!world.isSideSolid(onPos.x, onPos.y, onPos.z, ForgeDirection.getOrientation(side)))
            return null;

        WirelessPart part = getPart(item.getItemDamage());
        part.setupPlacement(player, side);
        return part;
    }

    public static WirelessPart getPart(int damage) {
        switch (damage) {
            case 0:
                return new TransmitterPart();
            case 1:
                return new ReceiverPart();
            case 2:
                return new JammerPart();
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (int d = 0; d < 3; d++)
            list.add(new ItemStack(item, 1, d));
    }

    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "|" + stack.getItemDamage();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister par1IconRegister) {
        RenderWireless.loadIcons(par1IconRegister);
    }
}
