package codechicken.wirelessredstone.core;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;

public class RedstoneEtherClient extends RedstoneEther
{
    public RedstoneEtherClient()
    {
        super(true);
    }
    
    @Override
    public void jamEntity(EntityLivingBase entity, boolean jam)
    {
        if(jam)
            jammedentities.put(entity, 1);
        else
            jammedentities.remove(entity);
    }
    
    @Override
    public void setFreq(ITileWireless tile, int freq)
    {
        TileEntity t = (TileEntity)tile;
        WRCoreCPH.sendSetTileFreq(t.xCoord, t.yCoord, t.zCoord, freq);
    }
}
