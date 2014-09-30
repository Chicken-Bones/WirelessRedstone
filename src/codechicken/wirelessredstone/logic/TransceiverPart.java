package codechicken.wirelessredstone.logic;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import codechicken.core.asm.InterfaceDependancies;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.vec.Vector3;
import codechicken.wirelessredstone.core.ITileWireless;
import codechicken.wirelessredstone.core.RedstoneEther;
import codechicken.wirelessredstone.core.WirelessRedstoneCore;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
/*import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;*/

@InterfaceDependancies
public abstract class TransceiverPart extends WirelessPart implements ITileWireless//, IPeripheral
{
    public byte deadmap;
    public int currentfreq;

    @Override
    public int getFreq() {
        return currentfreq;
    }

    @Override
    public void setFreq(int newfreq) {
        removeFromEther();
        currentfreq = newfreq;
        addToEther();
        if (disabled())
            RedstoneEther.server().jamNode(world(), x(), y(), z(), newfreq);
        updateChange();
    }

    @Override
    public void load(NBTTagCompound tag) {
        super.load(tag);
        currentfreq = tag.getInteger("freq");
        deadmap = tag.getByte("deadmap");
    }

    @Override
    public void save(NBTTagCompound tag) {
        super.save(tag);
        tag.setInteger("freq", currentfreq);
        tag.setByte("deadmap", deadmap);
    }

    @Override
    public void writeDesc(MCDataOutput packet) {
        super.writeDesc(packet);
        packet.writeShort(currentfreq);
    }

    @Override
    public void readDesc(MCDataInput packet) {
        super.readDesc(packet);
        currentfreq = packet.readUShort();
    }

    @Override
    public void jamTile() {
        setDisabled(true);
        deadmap = (byte) world().rand.nextInt(256);
        scheduleTick(3);
    }

    @Override
    public void unjamTile() {
        if (disabled()) {
            deadmap = (byte) world().rand.nextInt(256);
            scheduleTick(3);
        }
        setDisabled(false);
    }

    @Override
    public void scheduledTick() {
        if (deadmap != 0) {
            deadmap = (byte) ((deadmap & 0xFF) >> 1);
            if (deadmap != 0)
                scheduleTick(3);

            updateChange();
        }
    }

    @Override
    public boolean activate(EntityPlayer player, MovingObjectPosition hit, ItemStack held) {
        if (super.activate(player, hit, held))
            return true;

        if (hit.sideHit == (side() ^ 1) && !player.isSneaking()) {
            if (world().isRemote)
                WirelessRedstoneCore.proxy.openTileWirelessGui(player, (ITileWireless) tile());
            return true;
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderDynamic(Vector3 pos, float frame, int pass) {
        super.renderDynamic(pos, frame, pass);
        if (pass == 0)
            RenderWireless.renderFreq(pos, this);
    }
    
    /*@Override
    public void attach(IComputerAccess computer)
    {
    }
    
    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception
    {
        switch(method)
        {
            case 0:
                if(arguments.length < 1)
                    throw new Exception("Not Enough Arguments");
                if(!(arguments[0] instanceof Double) || Math.floor((Double) arguments[0]) != (Double)arguments[0])
                    throw new Exception("Argument 0 is not a number");
                int freq = ((Double)arguments[0]).intValue();
                if(freq < 0 || freq > RedstoneEther.numfreqs)
                    throw new Exception("Invalid Frequency: "+freq);
                if(!RedstoneEther.server().canBroadcastOnFrequency(owner, freq))
                    throw new Exception("Frequency: "+freq+" is private");
                setFreq(freq);
                return null;
            case 1:
                return new Object[]{getFreq()};
        }
        throw new Exception("derp?");
    }
    
    @Override
    public boolean canAttachToSide(int side)
    {
        return (side&6) != (side()&6);
    }
    
    @Override
    public void detach(IComputerAccess computer)
    {
    }
    
    @Override
    public String[] getMethodNames()
    {
        return new String[]{"setFreq", "getFreq"};
    }*/
}
