package codechicken.wirelessredstone.addons;

import net.minecraft.entity.player.EntityPlayer;
import codechicken.wirelessredstone.core.RedstoneEther;
import codechicken.wirelessredstone.core.WirelessReceivingDevice;

public class Sniffer implements WirelessReceivingDevice
{
    public Sniffer(EntityPlayer player)
    {
        owner = player;
    }

    public void updateDevice(int freq, boolean on)
    {
        if(RedstoneEther.get(false).canBroadcastOnFrequency(owner, freq))
        {
            WRAddonSPH.sendUpdateSnifferTo(owner, freq, on);
        }
    }
    
    EntityPlayer owner;
}
