package codechicken.wirelessredstone.logic;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import codechicken.core.ClientUtils;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.wirelessredstone.core.*;

public class JammerPart extends WirelessPart implements ITileJammer
{
    int randfreqspeed;
    
    public JammerPart()
    {
        setActive(true);
    }
    
    @Override
    public ItemStack getItem()
    {
        return new ItemStack(WirelessRedstoneLogic.itemwireless, 1, 2);
    }
    
    @Override
    public int textureSet()
    {
        return active() ? 0 : 1;
    }
    
    @Override
    public void onNeighborChanged()
    {
        if(dropIfCantStay())
            return;
        
        int gettingPowered = getPoweringLevel();
        if(active() && gettingPowered > 0)
        {
            setActive(false);
            removeFromEther();
            updateChange();
        }
        else if(!active() && gettingPowered == 0)
        {
            setActive(true);
            addToEther();
            updateChange();
        }
    }

    @Override
    public Vector3 getPearlPos()
    {
        return new Vector3(0.5, 0.74+getFloating()*0.04, 5/16D);
    }
    
    @Override
    public double getPearlSpin()
    {
        if(world().rand.nextInt(100) == 0 || randfreqspeed == 0)
            randfreqspeed = world().rand.nextInt(5000) + 1;
        
        if(!active())
            return 0;

        return RedstoneEther.getRotation(ClientUtils.getRenderTime(), randfreqspeed);
    }
    
    public float getPearlLight()
    {
        float light = world().getLightBrightness(x(), y(), z());
        if(active())
            light = (light + 1) * 0.5F;
        else
            light *= 0.75F;

        return light;
    }

    @Override
    public void onWorldJoin()
    {
        if(!world().isRemote && active())
            addToEther();
    }
    
    @Override
    public void onWorldSeparate()
    {
        if(!world().isRemote && active())
            removeFromEther();
    }

    public Vector3 getFocalPoint()
    {
        return new Vector3(0.3125, 0.24, 0).apply(rotationT());
    }
    
    public void jamTile(ITileWireless tile)
    {
        WirelessBolt bolt = new WirelessBolt(world(), 
                WirelessBolt.getFocalPoint((ITileJammer)tile()), 
                tile, world().rand.nextLong());
        bolt.defaultFractal();
        bolt.finalizeBolt();
    }

    public void jamEntity(Entity entity)
    {
        WirelessBolt bolt = new WirelessBolt(world(),
                WirelessBolt.getFocalPoint((ITileJammer)tile()), 
                Vector3.fromEntity(entity), world().rand.nextLong());
        bolt.defaultFractal();
        bolt.finalizeBolt();
    }
    
    public void addToEther()
    {
        if(active())
            RedstoneEther.server().addJammer(world(), x(), y(), z());
    }

    public void removeFromEther()
    {
        RedstoneEther.server().remJammer(world(), x(), y(), z());
    }
    
    @Override
    public String getType()
    {
        return "wrcbe-jamm";
    }
    
    @Override
    public Cuboid6 getExtensionBB()
    {
        return TransmitterPart.extensionBB[shape()];
    }
    
    @Override
    public int modelId()
    {
        return 2;
    }
}
