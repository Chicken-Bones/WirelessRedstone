package codechicken.wirelessredstone.addons;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import codechicken.wirelessredstone.core.*;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.StatCollector;

public class ItemWirelessTriangulator extends ItemWirelessFreq
{
    public ItemWirelessTriangulator() {
        setMaxStackSize(1);
    }

    public IIcon getIconFromDamage(int damage) {
        if (damage < 0 || damage > RedstoneEther.numfreqs)
            damage = 0;

        return TriangTexManager.getIconFromDamage(damage);
    }

    public int getItemFreq(ItemStack itemstack) {
        return itemstack.getItemDamage();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public String getItemStackDisplayName(ItemStack itemstack) {
        return RedstoneEtherAddons.localizeWirelessItem(
                StatCollector.translateToLocal("wrcbe_addons.triangulator.short"),
                itemstack.getItemDamage());
    }

    public String getGuiName() {
        return StatCollector.translateToLocal("item.wrcbe_addons:triangulator.name");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister par1IconRegister) {
    }
}
