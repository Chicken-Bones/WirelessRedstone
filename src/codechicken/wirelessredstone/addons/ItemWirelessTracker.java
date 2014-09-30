package codechicken.wirelessredstone.addons;

import codechicken.wirelessredstone.core.ItemWirelessFreq;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class ItemWirelessTracker extends ItemWirelessFreq
{
    public ItemWirelessTracker() {
        setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player) {
        if (player.isSneaking()) {
            return super.onItemRightClick(itemstack, world, player);
        }

        if (getItemFreq(itemstack) == 0)
            return itemstack;

        if (!player.capabilities.isCreativeMode) {
            itemstack.stackSize--;
        }
        if (!world.isRemote) {
            world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
            EntityWirelessTracker tracker = new EntityWirelessTracker(world, getItemFreq(itemstack), player);
            world.spawnEntityInWorld(tracker);
            WRAddonSPH.sendThrowTracker(tracker, player);
        }
        return itemstack;
    }


    @Override
    public int getItemFreq(ItemStack itemstack) {
        return itemstack.getItemDamage();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public String getItemStackDisplayName(ItemStack itemstack) {
        return RedstoneEtherAddons.localizeWirelessItem(
                StatCollector.translateToLocal("wrcbe_addons.tracker.short"),
                itemstack.getItemDamage());
    }

    @Override
    public String getGuiName() {
        return StatCollector.translateToLocal("item.wrcbe_addons:tracker.name");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister par1IconRegister) {
    }
}
