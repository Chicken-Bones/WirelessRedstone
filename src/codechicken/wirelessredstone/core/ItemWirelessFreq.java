package codechicken.wirelessredstone.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public abstract class ItemWirelessFreq extends Item
{
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking())
            return false;

        WirelessRedstoneCore.proxy.openItemWirelessGui(player);
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack par1ItemStack, World par2World, EntityPlayer entityplayer) {
        if (entityplayer.isSneaking())
            WirelessRedstoneCore.proxy.openItemWirelessGui(entityplayer);

        return par1ItemStack;
    }

    public final void setFreq(EntityPlayer player, int slot, ItemStack stack, int freq) {
        if (player.worldObj.isRemote)
            WRCoreCPH.sendSetItemFreq(slot, freq);
        else
            stack.setItemDamage(freq);
    }

    public abstract int getItemFreq(ItemStack itemstack);

    public abstract String getGuiName();
}
