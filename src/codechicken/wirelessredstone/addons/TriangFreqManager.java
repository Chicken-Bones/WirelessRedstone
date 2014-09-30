package codechicken.wirelessredstone.addons;

import net.minecraft.entity.player.EntityPlayer;

public class TriangFreqManager
{
    public TriangFreqManager(int freq)
    {
        this.freq = freq;
    }

    public void setTriangAngle(float angle)
    {
        this.triangsmpspinto = angle;
    }
    
    public boolean isTriangOn()
    {
        return triangsmpspinto != -1;
    }
    
    public void tickTriang(EntityPlayer player)
    {        
        double spinto;//the angle the triang should finally point
        if(!isTriangOn() || player == null)
        {
            spinto = triangangle;//leave it where it is
        }
        else if(RedstoneEtherAddons.client().isRemoteOn(player, freq) || triangsmpspinto == -2)//holding remote active or in another dimension
        {
            spinto = Math.random() * 6.2831853071795862D;//spin to a random place
        }
        else
        {
            spinto = triangsmpspinto;
        }
        
        double spindiff;//distance to spin
        for(spindiff = spinto - triangangle; spindiff < -3.1415926535897931D; spindiff += 6.2831853071795862D)
        {
        }//make sure angle is between PI and -PI
        for(; spindiff >= 3.1415926535897931D; spindiff -= 6.2831853071795862D)
        {
        }
        if(spindiff < -1D)//bound spin to 1radian per tick
        {
            spindiff = -1D;
        }
        if(spindiff > 1.0D)
        {
            spindiff = 1.0D;
        }
        triangspinvelocity += spindiff * 0.1D;//add some spin velocity to the pointer based on the diff
        triangspinvelocity *= 0.8D;//decellerate the spinner
        triangangle += triangspinvelocity;//add the spin(velocity) to the angle(position)
    }
    
    public float getTriangAngle()
    {
        return triangangle;
    }
    
    int freq;
    
    private float triangangle;
    private float triangspinvelocity;
    
    private float triangsmpspinto = -1;
    
}
