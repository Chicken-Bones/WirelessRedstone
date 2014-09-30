package codechicken.wirelessredstone.logic;

import net.minecraft.item.ItemStack;

import codechicken.core.ClientUtils;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;
import codechicken.wirelessredstone.core.*;
import net.minecraft.util.StatCollector;

import static codechicken.lib.vec.Rotation.*;
import static codechicken.lib.vec.Vector3.*;

public class ReceiverPart extends TransceiverPart implements ITileReceiver
{
    public static Cuboid6[] extensionBB = new Cuboid6[24];
    
    static
    {
        Cuboid6 base = new Cuboid6(3/16D, 1/8D, 1/8D, 13/16D, 13/16D, 5/8D);
        for(int s = 0; s < 6; s++)
            for(int r = 0; r < 4; r++)
                extensionBB[s<<2|r] = base.copy().apply(sideOrientation(s, r).at(center));
    }
    
    @Override
    public ItemStack getItem()
    {
        return new ItemStack(WirelessRedstoneLogic.itemwireless, 1, 1);
    }
    
    @Override
    public int strongPowerLevel(int side)
    {
        return side == Rotation.rotateSide(side(), rotation()) && active() ? 15 : 0;
    }

    @Override
    public void setActive(boolean on)
    {
        super.setActive(on);
        updateChange();
        tile().notifyNeighborChange(Rotation.rotateSide(side(), rotation()));
    }

    @Override
    public void jamTile()
    {
        super.jamTile();
        changeSpinState(true);
        setActive(false);
    }

    @Override
    public void unjamTile()
    {
        super.unjamTile();
        changeSpinState(false);
        addToEther();
    }
    
    public void changeSpinState(boolean jam)
    {
        if(!jam && spinoffset < 0)//unjamming
        {
            int time = (int) (world().getTotalWorldTime() % 100000);
            spinoffset = time + spinoffset;
        }
        else if(jam && spinoffset >= 0)//jamming
        {
            int time = (int) ((world().getTotalWorldTime() - spinoffset) % 100000);
            spinoffset = -time;
        }
    }
    
    public Vector3 getFocalPoint()
    {
        return new Vector3(0.0755, 0.255, 0).apply(rotationT());
    }

    @Override
    public Vector3 getPearlPos()
    {
        return new Vector3(0, getFloating()*0.02, 0)
            .apply(getPearlRotation())
            .add(0.5, 0.755, 0.545);
    }

    @Override
    public Transformation getPearlRotation()
    {
        return new Rotation(0.7854, 1, 0, 0);
    }

    @Override
    public double getPearlScale()
    {
        return 0.04;
    }

    @Override
    public double getPearlSpin()
    {
        if(spinoffset < 0)
            return RedstoneEther.getRotation(-spinoffset, currentfreq);

        return RedstoneEther.getRotation(ClientUtils.getRenderTime()-spinoffset, currentfreq);
    }
    
    public float getPearlLight()
    {
        float light = world().getLightBrightness(x(), y(), z());
        if((deadmap & 1) == 1 || (deadmap == 0 && (disabled() || !active() || currentfreq == 0)))
            light = (light + 1) * 0.25F;
        else
            light = (light + 1) * 0.5F;
        
        return light;
    }    

    @Override
    public void setFreq(int newfreq)
    {
        super.setFreq(newfreq);
        
        if(newfreq == 0)
            setActive(false);
    }
    
    public void addToEther()
    {
        RedstoneEther.server().addReceiver(world(), x(), y(), z(), currentfreq);
    }
    
    public void removeFromEther()
    {
        RedstoneEther.server().remReceiver(world(), x(), y(), z(), currentfreq);
    }
    
    public String getGuiName()
    {
        return StatCollector.translateToLocal("item.wrcbe_logic:wirelesspart|1.name");
    }
    
    @Override
    public String getType()
    {
        return "wrcbe-recv";
    }
    
    @Override
    public void read(MCDataInput packet)
    {
        super.read(packet);
        changeSpinState(disabled());
    }
    
    @Override
    public Cuboid6 getExtensionBB()
    {
        return extensionBB[shape()];
    }
    
    @Override
    public int modelId()
    {
        return 1;
    }
    
    @Override
    public double m1() {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public double m2(double d) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public void m3(double d) {
        // TODO Auto-generated method stub
        
    }
}
