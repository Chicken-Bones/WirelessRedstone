package codechicken.wirelessredstone.addons;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.item.ItemMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.world.World;

public class ItemWirelessMap extends ItemMap
{
    @Override
    @SideOnly(Side.CLIENT)
    public void onUpdate(ItemStack itemstack, World world, Entity entity, int slotno, boolean held)
    {
        super.onUpdate(itemstack, world, entity, slotno, held);
        EntityPlayer player = (EntityPlayer)entity;
        if(held)
        {
            if(slotno != lastheldmap)
            {
                RedstoneEtherAddons.client().clearMapNodes(player);
                lastheldmap = slotno;
            }
        }
        else
        {
            ItemStack helditem = player.inventory.getCurrentItem();
            if((helditem == null || helditem.getItem()!= this) && lastheldmap >= 0)
            {
                lastheldmap = -1;
                RedstoneEtherAddons.client().clearMapNodes(player);
            }
        }
    }

    @Override
    public Packet func_150911_c(ItemStack itemstack, World world, EntityPlayer entityplayer)
    {
        RedstoneEtherAddons.server().updateSMPMapInfo(world, entityplayer, getMapData(itemstack, world), itemstack.getItemDamage());
        return super.func_150911_c(itemstack, world, entityplayer);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return super.getItemStackDisplayName(stack) + " #"+stack.getItemDamage();
    }

    public int lastheldmap = -1;
}
