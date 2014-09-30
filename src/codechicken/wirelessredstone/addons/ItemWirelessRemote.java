package codechicken.wirelessredstone.addons;

import codechicken.wirelessredstone.core.*;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class ItemWirelessRemote extends ItemWirelessFreq
{
    public ItemWirelessRemote() {
        setMaxStackSize(1);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking() && stack.getItemDamage() <= 5000 && stack.getItemDamage() > 0)//not sneaking, off and valid freq
        {
            TileEntity tile = world.getTileEntity(x, y, z);
            int freq = stack.getItemDamage();
            if (tile != null && tile instanceof ITileWireless && RedstoneEther.get(world.isRemote).canBroadcastOnFrequency(player, freq)) {
                RedstoneEther.get(world.isRemote).setFreq((ITileWireless) tile, freq);
                return true;
            }
        }
        onItemRightClick(stack, world, player);
        return false;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer entityplayer) {
        if (entityplayer.isSneaking()) {
            return super.onItemRightClick(stack, world, entityplayer);
        }

        if (!getTransmitting(stack) && stack.getItemDamage() != 0)
            RedstoneEtherAddons.get(world.isRemote).activateRemote(world, entityplayer);

        return stack;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
        if (!(entity instanceof EntityPlayer))
            return;

        int freq = getItemFreq(stack);
        EntityPlayer player = (EntityPlayer) entity;

        if (getTransmitting(stack) && (!held || !RedstoneEtherAddons.get(world.isRemote).isRemoteOn(player, freq)) &&
                !RedstoneEtherAddons.get(world.isRemote).deactivateRemote(world, player))
            stack.setItemDamage(freq);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, EntityPlayer player) {
        return !getTransmitting(stack);
    }

    @Override
    public int getItemFreq(ItemStack itemstack) {
        return itemstack.getItemDamage() & 0x1FFF;
    }

    public static boolean getTransmitting(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey("on", 1) && tag.getBoolean("on");
    }

    public static void setOn(ItemStack stack, boolean on) {
        if(!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setBoolean("on", on);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ItemStack stack, int pass) {
        return getIconIndex(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) {
        int freq = stack.getItemDamage();

        if (freq <= 0 || freq > RedstoneEther.numfreqs)
            return RemoteTexManager.getIcon(-1, false);

        return RemoteTexManager.getIcon(RedstoneEther.get(true).getFreqColourId(freq), getTransmitting(stack));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getItemStackDisplayName(ItemStack itemstack) {
        return RedstoneEtherAddons.localizeWirelessItem(
                StatCollector.translateToLocal("wrcbe_addons.remote.short"),
                itemstack.getItemDamage());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister par1IconRegister) {
    }

    public String getGuiName() {
        return StatCollector.translateToLocal("item.wrcbe_addons:remote.name");
    }
}
