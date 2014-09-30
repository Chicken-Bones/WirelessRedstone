package codechicken.wirelessredstone.addons;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemWirelessSniffer extends Item
{
    public ItemWirelessSniffer() {
        setMaxStackSize(1);
    }

    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer) {
        if (world.isRemote) {
            WirelessRedstoneAddons.proxy.openSnifferGui(entityplayer);
            RedstoneEtherAddons.client().addSniffer(entityplayer);
        }
        return itemstack;
    }
}
