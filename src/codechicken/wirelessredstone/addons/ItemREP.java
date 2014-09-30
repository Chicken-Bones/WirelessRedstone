package codechicken.wirelessredstone.addons;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemREP extends Item
{
    public ItemREP() {
        setMaxStackSize(16);
    }

    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player) {
        if (world.isRemote)
            return itemstack;

        if (RedstoneEtherAddons.server().detonateREP(player))
            return itemstack;

        RedstoneEtherAddons.server().throwREP(itemstack, world, player);
        return itemstack;
    }
}
