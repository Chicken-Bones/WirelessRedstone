package codechicken.wirelessredstone.addons;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMapBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;

public class ItemEmptyWirelessMap extends ItemMapBase
{
    /**
     * Called whenever this item is equipped and the right mouse button is pressed. Args: itemStack, world, entityPlayer
     */
    public ItemStack onItemRightClick(ItemStack p_77659_1_, World p_77659_2_, EntityPlayer p_77659_3_)
    {
        ItemStack itemstack1 = new ItemStack(WirelessRedstoneAddons.wirelessMap, 1, p_77659_2_.getUniqueDataId("map"));
        String s = "map_" + itemstack1.getItemDamage();
        MapData mapdata = new MapData(s);
        p_77659_2_.setItemData(s, mapdata);
        mapdata.scale = 0;
        int i = 128 * (1 << mapdata.scale);
        mapdata.xCenter = (int)(Math.round(p_77659_3_.posX / (double)i) * (long)i);
        mapdata.zCenter = (int)(Math.round(p_77659_3_.posZ / (double)i) * (long)i);
        mapdata.dimension = p_77659_2_.provider.dimensionId;
        mapdata.markDirty();
        --p_77659_1_.stackSize;

        if (p_77659_1_.stackSize <= 0)
        {
            return itemstack1;
        }
        else
        {
            if (!p_77659_3_.inventory.addItemStackToInventory(itemstack1.copy()))
            {
                p_77659_3_.dropPlayerItemWithRandomChoice(itemstack1, false);
            }

            return p_77659_1_;
        }
    }
}