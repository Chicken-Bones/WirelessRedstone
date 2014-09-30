package codechicken.wirelessredstone.addons;

import java.util.List;

import codechicken.core.CommonUtils;
import codechicken.core.ServerUtils;
import codechicken.lib.vec.Quat;
import codechicken.lib.vec.Vector3;
import codechicken.wirelessredstone.core.RedstoneEther;
import codechicken.wirelessredstone.core.WirelessTransmittingDevice;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class EntityWirelessTracker extends Entity implements WirelessTransmittingDevice
{
    public EntityWirelessTracker(World world)
    {
        super(world);
        setSize(0.25F, 0.25F);
    }
    
    public EntityWirelessTracker(World world, int freq)
    {
        this(world);
        this.freq = freq;
    }

    public EntityWirelessTracker(World world, int freq, EntityLivingBase entityliving)
    {
        this(world, freq);
        setLocationAndAngles(entityliving.posX, entityliving.posY + entityliving.getEyeHeight(), entityliving.posZ, entityliving.rotationYaw, entityliving.rotationPitch);
        float speed = 1.3F;
        motionX = -MathHelper.sin((rotationYaw / 180F) * 3.141593F) * MathHelper.cos((rotationPitch / 180F) * 3.141593F) * speed;
        motionZ = MathHelper.cos((rotationYaw / 180F) * 3.141593F) * MathHelper.cos((rotationPitch / 180F) * 3.141593F) * speed;
        motionY = -MathHelper.sin((rotationPitch / 180F) * 3.141593F) * speed;
        this.freq = freq;
    }

    protected void entityInit()
    {
    }

    public boolean isInRangeToRenderDist(double d)
    {
        return true;
    }
    
    public void onEntityUpdate()
    {
        if((attachmentCounter < -6000 && !attached) || (!worldObj.isRemote && attachedInOtherDimension()))
        {
            setDead();
            return;
        }
        
        if(!loaded && !worldObj.isRemote)
        {
            loaded = true;
            RedstoneEtherAddons.server().addTracker(this);
            
            if(!attachedToLogout())
            {
                RedstoneEther.server().addTransmittingDevice(this);
            }
        }
        
        boundingBox.setBB(boundingBox.expand(0,0.2,0));//so we actually consist of something decent for material tests
        super.onEntityUpdate();
        boundingBox.setBB(boundingBox.expand(0,-0.2,0));
        
        if(attached && attachedEntity == null)
        {
            if(!worldObj.isRemote)
                findAttachedEntity();
        }
        else if(isAttachedToEntity())
        {
            trackEntity();
            
            if(!worldObj.isRemote)
                checkDetachment();
        }
        else
        {
            applyPhysics();
            moveEntityWithBounce(1);
            if(!worldObj.isRemote)
                attachToNearbyEntities();
        }
        
        if(item && attachmentCounter == 0)
        {
            item = false;
            if(!worldObj.isRemote)
                RedstoneEtherAddons.server().updateTracker(this);
        }
        if(isBurning())
        {
            extinguish();
            if(!worldObj.isRemote)
                RedstoneEtherAddons.server().updateTracker(this);
            item = true;
            attachmentCounter = 1200;//1 min
        }
        
        if(!attachedToLogout())
            attachmentCounter--;
    }
    
    @Override
    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2)
    {
        if(par1DamageSource == DamageSource.lava || par1DamageSource == DamageSource.outOfWorld)
        {
            setDead();
            return true;
        }
        return false;
    }
    
    @Override
    public void setDead()
    {
        super.setDead();
        
        if(!worldObj.isRemote)
        {
            RedstoneEther.server().removeTransmittingDevice(this);
            RedstoneEtherAddons.server().removeTracker(this);
        }
    }
    
    @Override
    public void onCollideWithPlayer(EntityPlayer par1EntityPlayer)
    {
        if (!this.worldObj.isRemote && item && par1EntityPlayer.inventory.addItemStackToInventory(new ItemStack(WirelessRedstoneAddons.tracker, 1, freq)))
        {
            this.worldObj.playSoundAtEntity(this, "random.pop", 0.2F, ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            par1EntityPlayer.onItemPickup(this, 1);
            setDead();
        }
    }
    
    private void checkDetachment()
    {
        if(attachedEntity.isDead && attachedPlayerName != null)
        {
            attachedEntity = null;//just invalidate and hide :)
            RedstoneEther.server().removeTransmittingDevice(this);
            return;
        }
        
        if(attachedEntity.isBurning() || attachedEntity.isDead)
        {
            detachFromEntity();
        }
        
        int x = MathHelper.floor_double(posX);
        int y = MathHelper.floor_double(posY+height/2);
        int z = MathHelper.floor_double(posZ);
        
        if(isRetractingStickyPistonFacing(x, y + 2, z, 0) ||
            isRetractingStickyPistonFacing(x, y - 2, z, 1) ||
            isRetractingStickyPistonFacing(x, y, z + 2, 2) ||
            isRetractingStickyPistonFacing(x, y, z - 2, 3) ||
            isRetractingStickyPistonFacing(x + 2, y, z, 4) ||
            isRetractingStickyPistonFacing(x - 2, y, z, 5))
        {
            detachFromEntity();
        }
    }

    public boolean isRetractingStickyPistonFacing(int x, int y, int z, int facing)
    {
        Block block = worldObj.getBlock(x, y, z);
        if(block != Blocks.piston_extension)
            return false;
        
        TileEntity t = worldObj.getTileEntity(x, y, z);
        if(!(t instanceof TileEntityPiston))
            return false;
        
        TileEntityPiston tep = (TileEntityPiston)t;
        return tep.getPistonOrientation() == facing && !tep.isExtending() && tep.getStoredBlockID() == Blocks.sticky_piston;
    }

    private void detachFromEntity()
    {
        attachedEntity = null;
        attached = false;
        attachedX = 0;
        attachedY = 0;
        attachedZ = 0;
        attachedYaw = 0;
        attachmentCounter = 5;
        
        attachedPlayerName = null;

        motionX = rand.nextFloat()-0.5;
        motionY = rand.nextFloat()-0.5;
        motionZ = rand.nextFloat()-0.5;
        
        RedstoneEtherAddons.server().updateTracker(this);
    }

    private void trackEntity()
    {        
        Vector3 relPos = getRotatedAttachment();
        setPosition(attachedEntity.posX + relPos.x, 
                attachedEntity.posY + attachedEntity.height/2 - attachedEntity.yOffset + relPos.y - height, 
                attachedEntity.posZ + relPos.z);
    }

    private void applyPhysics()
    {
        motionY -= 0.05D;//gravity
        
        if(onGround)//ground drag
        {
            motionX *= 0.8;
            motionZ *= 0.8;
        }

        func_145771_j(posX, posY+height/2, posZ);
    }

    private void attachToNearbyEntities()
    {
        if(isAttachedToEntity() || item || attachmentCounter > 0)
            return;
        
        for(Entity entity : (List<Entity>)worldObj.getEntitiesWithinAABBExcludingEntity(this, AxisAlignedBB.getBoundingBox(-10, -10, -10, 10, 10, 10).offset(posX, posY, posZ)))
        {            
            AxisAlignedBB bb = entity.boundingBox;
            if(bb != null && 
                    entity.width >= 0.3)
            {
                if(tryAttach(entity, 0.4, 0.2))
                    return;
            }
        }
    }

    private boolean tryAttach(Entity entity, double extraw, double extrah)
    {
        Vector3 evec = Vector3.fromEntityCenter(entity);
        Vector3 posVec = new Vector3(posX, posY + height / 2, posZ);
        Vector3 lastPosVec = new Vector3(lastTickPosX, lastTickPosY + height / 2, lastTickPosZ);
        Vector3 diffVec = lastPosVec.copy().subtract(posVec);
        double distanceBetweenTicks = diffVec.mag();
        
        double width = entity.width+extraw;
        double height = entity.height+extrah;
        
        for(double d = 0; d <= distanceBetweenTicks; d+=0.05)
        {
            Vector3 interpVec = diffVec.copy().normalize().multiply(d).add(lastPosVec);

            double xDiff = Math.abs(evec.x - interpVec.x);
            double yDiff = Math.abs(evec.y - interpVec.y);
            double zDiff = Math.abs(evec.z - interpVec.z);
            
            if(yDiff <= height / 2 && xDiff <= width / 2 && zDiff <= width / 2)
            {
                attachToEntity(interpVec, entity);                
                return true;
            }
        }
        return false;        
    }
    
    public void attachToEntity(Vector3 pos, Entity e)
    {
        attached = true;
        attachedEntity = e;
        attachedX = (float) (pos.x - e.posX);
        attachedY = (float) (pos.y + height/2 - (e.posY - e.yOffset + e.height/2));
        attachedZ = (float) (pos.z - e.posZ);
        attachedYaw = getEntityRotation();
        
        if(attachedEntity instanceof EntityPlayer)
            attachedPlayerName = attachedEntity.getCommandSenderName();

        moveToEntityExterior();
        RedstoneEtherAddons.server().updateTracker(this);
    }

    private void moveToEntityExterior()
    {
        Vector3 attachPosVec2 = getRotatedAttachment().normalize().multiply(Math.max(attachedEntity.width, attachedEntity.height));
        
        Vector3 diffVec = attachPosVec2.copy().negate();
        double distanceBetweenTicks = diffVec.mag();
        
        double width = attachedEntity.width;
        double height = attachedEntity.height;
        
        for(double d = 0; d <= distanceBetweenTicks; d+=0.05)
        {
            Vector3 interpVec = diffVec.copy().normalize().multiply(d).add(attachPosVec2);

            double xDiff = Math.abs(interpVec.x);
            double yDiff = Math.abs(interpVec.y);
            double zDiff = Math.abs(interpVec.z);
            
            if(yDiff <= height / 2 && xDiff <= width / 2 && zDiff <= width / 2)
            {
                attachedYaw = getEntityRotation();
                attachedX = (float) (interpVec.x);
                attachedY = (float) (interpVec.y);
                attachedZ = (float) (interpVec.z);
                return;
            }
        }
    }

    public boolean isAttachedToEntity()
    {
        return attachedEntity != null;
    }

    public void moveEntityWithBounce(double bounceFactor)
    {
        double dx = motionX;
        double dz = motionZ;
        
        moveEntity(motionX, motionY, motionZ);
        setPosition(posX, posY, posZ);
        
        boolean isCollidedX = motionX != dx;
        boolean isCollidedZ = motionZ != dz;
        
        motionX = dx;
        motionZ = dz;
                
        if(isCollidedX)
        {
            motionX *= -bounceFactor;
            posX += Math.signum(motionX)*0.1;
        }
        if(isCollidedZ)
        {
            motionZ *= -bounceFactor;
            posZ += Math.signum(motionZ)*0.1;
        }

        func_145771_j(posX, posY + height / 2, posZ);
    }

    private void findAttachedEntity()
    {
        if(attachmentCounter == 0)
        {
            detachFromEntity();
            return;
        }
        
        if(attachedPlayerName != null)
        {
            EntityPlayer player = ServerUtils.getPlayer(attachedPlayerName);
            if(player != null)
            {
                attachedEntity = player;
                moveToEntityExterior();
                RedstoneEther.server().addTransmittingDevice(this);
                attachmentCounter = 0;
                return;
            }
        }
        else
        {
            for(Entity entity : (List<Entity>)worldObj.getEntitiesWithinAABBExcludingEntity(this, AxisAlignedBB.getBoundingBox(-10, -10, -10, 10, 10, 10).offset(posX, posY, posZ)))
            {
                if(tryAttach(entity, 0.4, 0.2))
                {
                    attachmentCounter = 0;
                    return;
                }
            }
            
            attachmentCounter--;
        }
    }

    public void writeEntityToNBT(NBTTagCompound nbttagcompound)
    {        
        nbttagcompound.setBoolean("attached", attached);
        nbttagcompound.setShort("attachCount", (short) attachmentCounter);
        nbttagcompound.setBoolean("item", item);
        nbttagcompound.setShort("freq", (short) freq);
        if(attachedPlayerName != null)
        {
            nbttagcompound.setString("player", attachedPlayerName);
            nbttagcompound.setFloat("attachedX", attachedX);
            nbttagcompound.setFloat("attachedY", attachedY);
            nbttagcompound.setFloat("attachedZ", attachedZ);
            nbttagcompound.setFloat("attachedYaw", attachedYaw);
        }
    }
    
    public void onChunkUnload()
    {        
        if(!worldObj.isRemote)
        {
            RedstoneEther.server().removeTransmittingDevice(this);
            RedstoneEtherAddons.server().removeTracker(this);
        }
    }

    public boolean attachedInOtherDimension()
    {
        return (attachedEntity instanceof EntityPlayer) && ((EntityPlayer)attachedEntity).dimension != getDimension();
    }

    public void readEntityFromNBT(NBTTagCompound nbttagcompound)
    {
        attached = nbttagcompound.getBoolean("attached");
        attachmentCounter = nbttagcompound.getShort("attachCount");
        freq = nbttagcompound.getShort("freq");
        item = nbttagcompound.getBoolean("item");
        if(nbttagcompound.hasKey("player"))
        {
            attachedPlayerName = nbttagcompound.getString("player");
            attachedX = nbttagcompound.getFloat("attachedX");
            attachedY = nbttagcompound.getFloat("attachedY");
            attachedZ = nbttagcompound.getFloat("attachedZ");
            attachedYaw = nbttagcompound.getFloat("attachedYaw");
        }
        
        if(attached)
            attachmentCounter = 5;
    }

    public float getShadowSize()
    {
        return 0.0F;
    }
    
    @Override
    protected boolean canTriggerWalking()
    {
        return false;
    }
    
    public Vector3 getRotatedAttachment()
    {
        Vector3 relPos = new Vector3(attachedX, attachedY, attachedZ);
        Quat rot = Quat.aroundAxis(0, 1, 0, (getEntityRotation()-attachedYaw)*torad);
        rot.rotate(relPos);
        return relPos;
    }
    
    public float getEntityRotation()
    {
        if(attachedEntity instanceof EntityLivingBase)
            return -((EntityLivingBase)attachedEntity).renderYawOffset;
        
        return attachedEntity.rotationYaw;
    }
    
    public boolean attachedToLogout()
    {
        return attached && attachedEntity == null && attachedPlayerName != null;
    }
    
    public void copyToDimension(int dimension)
    {
        World otherWorld = DimensionManager.getWorld(dimension);
        EntityWirelessTracker copy = new EntityWirelessTracker(otherWorld, freq);
        copy.attached = true;
        copy.attachedPlayerName = attachedPlayerName;
        copy.attachedX = attachedX;
        copy.attachedY = attachedY;
        copy.attachedZ = attachedZ;
        copy.attachedYaw = attachedYaw;
        
        copy.setPosition(attachedEntity.posX, attachedEntity.posY, attachedEntity.posZ);//make sure we spawn in the right chunk :D
        
        otherWorld.spawnEntityInWorld(copy);
    }
    
    @Override
    public EntityLivingBase getAttachedEntity()
    {
        if(attachedEntity instanceof EntityLivingBase)
            return (EntityLivingBase) attachedEntity;
        return null;
    }

    @Override
    public Vector3 getPosition()
    {
        return Vector3.fromEntityCenter(this);
    }

    @Override
    public int getDimension()
    {
        return CommonUtils.getDimension(worldObj);
    }

    @Override
    public int getFreq()
    {
        return freq;
    }
    
    String attachedPlayerName;
    
    boolean attached;
    boolean item;
    float attachedX;
    float attachedY;
    float attachedZ;
    float attachedYaw;
    /**
     * Multi purpose counter.
     * If not attached and positive, ticks before attachment can begin (after being thrown)
     * If not attached and negative, timer when reaches -6000 (5 mins) entity despawns
     * If attached but no entity, ticks to wait in the air searching for entity
     * If item, ticks to remain an item.
     */
    int attachmentCounter = 2;  
    int freq;
    
    boolean loaded;
    public Entity attachedEntity;
    
    public static final double torad = 0.017453;
    public static final double todeg = 57.295779;
}
