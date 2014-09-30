package codechicken.wirelessredstone.addons;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import codechicken.lib.vec.Vector3;
import codechicken.wirelessredstone.core.*;

public class Remote implements WirelessTransmittingDevice
{
    public Remote(EntityPlayer owner) {
        this.owner = owner;
        freq = owner.inventory.getCurrentItem().getItemDamage() & 0x1FFF;
        slot = owner.inventory.currentItem;
    }

    @Override
    public EntityLivingBase getAttachedEntity() {
        return owner;
    }

    @Override
    public Vector3 getPosition() {
        return Vector3.fromEntityCenter(owner);
    }

    @Override
    public int getDimension() {
        return owner.dimension;
    }

    @Override
    public int getFreq() {
        return freq;
    }

    public boolean isBeingHeld() {
        ItemStack held = owner.inventory.getCurrentItem();
        return owner.inventory.currentItem == slot && //same slot
                held != null && //not holding nothing
                held.getItem() == WirelessRedstoneAddons.remote && //same item type
                held.getItemDamage() == freq;//same freq
    }

    public void metaOff() {
        ItemStack stack = owner.inventory.getStackInSlot(slot);
        if (stack != null)
            ItemWirelessRemote.setOn(stack, false);
    }

    public void metaOn() {
        ItemStack stack = owner.inventory.getStackInSlot(slot);
        ItemWirelessRemote.setOn(stack, true);
    }

    EntityPlayer owner;
    int freq;
    int slot;
}
