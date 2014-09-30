package codechicken.wirelessredstone.addons;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import codechicken.core.CommonUtils;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;
import codechicken.wirelessredstone.core.*;

public class EntityREP extends Entity
{
    public EntityREP(World world) {
        super(world);
        xTileREP = -1;
        yTileREP = -1;
        zTileREP = -1;
        setSize(0.25F, 0.25F);
    }

    protected void entityInit() {}

    public boolean isInRangeToRenderDist(double d) {
        return true;
    }

    public EntityREP(World world, EntityLivingBase entityliving) {
        super(world);
        xTileREP = -1;
        yTileREP = -1;
        zTileREP = -1;
        shootingEntity = entityliving;
        setSize(0.25F, 0.25F);
        setLocationAndAngles(entityliving.posX, entityliving.posY + entityliving.getEyeHeight(), entityliving.posZ, entityliving.rotationYaw, entityliving.rotationPitch);
        posX -= MathHelper.cos((rotationYaw / 180F) * 3.141593F) * 0.16F;
        posY -= 0.10000000149011612D;
        posZ -= MathHelper.sin((rotationYaw / 180F) * 3.141593F) * 0.16F;
        setPosition(posX, posY, posZ);
        float f = 0.4F;
        motionX = -MathHelper.sin((rotationYaw / 180F) * 3.141593F) * MathHelper.cos((rotationPitch / 180F) * 3.141593F) * f;
        motionZ = MathHelper.cos((rotationYaw / 180F) * 3.141593F) * MathHelper.cos((rotationPitch / 180F) * 3.141593F) * f;
        motionY = -MathHelper.sin((rotationPitch / 180F) * 3.141593F) * f;
        setREPHeading(motionX, motionY, motionZ, 1.5F, 1.0F);
    }

    public EntityREP(World world, double d, double d1, double d2) {
        super(world);
        xTileREP = -1;
        yTileREP = -1;
        zTileREP = -1;
        setSize(0.25F, 0.25F);
        setPosition(d, d1, d2);
    }

    public void setREPHeading(double d, double d1, double d2, float f,
                              float f1) {
        float f2 = MathHelper.sqrt_double(d * d + d1 * d1 + d2 * d2);
        d /= f2;
        d1 /= f2;
        d2 /= f2;
        d += rand.nextGaussian() * 0.0074999998323619366D * f1;
        d1 += rand.nextGaussian() * 0.0074999998323619366D * f1;
        d2 += rand.nextGaussian() * 0.0074999998323619366D * f1;
        d *= f;
        d1 *= f;
        d2 *= f;
        motionX = d;
        motionY = d1;
        motionZ = d2;
        float f3 = MathHelper.sqrt_double(d * d + d2 * d2);
        prevRotationYaw = rotationYaw = (float) ((Math.atan2(d, d2) * 180D) / 3.1415927410125732D);
        prevRotationPitch = rotationPitch = (float) ((Math.atan2(d1, f3) * 180D) / 3.1415927410125732D);
        ticksInGroundREP = 0;
    }

    public void setVelocity(double d, double d1, double d2) {
        motionX = d;
        motionY = d1;
        motionZ = d2;
        if (prevRotationPitch == 0.0F && prevRotationYaw == 0.0F) {
            float f = MathHelper.sqrt_double(d * d + d2 * d2);
            prevRotationYaw = rotationYaw = (float) ((Math.atan2(d, d2) * 180D) / 3.1415927410125732D);
            prevRotationPitch = rotationPitch = (float) ((Math.atan2(d1, f) * 180D) / 3.1415927410125732D);
        }
    }

    public void onUpdate() {
        lastTickPosX = posX;
        lastTickPosY = posY;
        lastTickPosZ = posZ;
        super.onUpdate();
        if (shakeREP > 0) {
            shakeREP--;
        }
        if (inGroundREP) {
            Block block = worldObj.getBlock(xTileREP, yTileREP, zTileREP);
            if (block != inTileREP) {
                inGroundREP = false;
                motionX *= rand.nextFloat() * 0.2F;
                motionY *= rand.nextFloat() * 0.2F;
                motionZ *= rand.nextFloat() * 0.2F;
                ticksInGroundREP = 0;
                ticksInAirREP = 0;
            } else {
                ticksInGroundREP++;
                if (ticksInGroundREP == 1200) {
                    setDead();
                }
                return;
            }
        } else {
            ticksInAirREP++;
        }
        Vec3 vec3d = Vec3.createVectorHelper(posX, posY, posZ);
        Vec3 vec3d1 = Vec3.createVectorHelper(posX + motionX, posY + motionY, posZ + motionZ);
        MovingObjectPosition movingobjectposition = worldObj.rayTraceBlocks(vec3d, vec3d1);
        vec3d = Vec3.createVectorHelper(posX, posY, posZ);
        vec3d1 = Vec3.createVectorHelper(posX + motionX, posY + motionY, posZ + motionZ);
        if (movingobjectposition != null)
            vec3d1 = Vec3.createVectorHelper(movingobjectposition.hitVec.xCoord, movingobjectposition.hitVec.yCoord, movingobjectposition.hitVec.zCoord);

        if (!worldObj.isRemote) {
            Entity entity = null;
            List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.addCoord(motionX, motionY, motionZ).expand(1.0D, 1.0D, 1.0D));
            double d = 0.0D;
            for (Entity entity1 : list) {
                if (!entity1.canBeCollidedWith() || entity1 == shootingEntity && ticksInAirREP < 5)
                    continue;

                float f4 = 0.3F;
                AxisAlignedBB axisalignedbb = entity1.boundingBox.expand(f4, f4, f4);
                MovingObjectPosition movingobjectposition1 = axisalignedbb.calculateIntercept(vec3d, vec3d1);
                if (movingobjectposition1 == null)
                    continue;

                double d1 = vec3d.distanceTo(movingobjectposition1.hitVec);
                if (d1 < d || d == 0.0D) {
                    entity = entity1;
                    d = d1;
                }
            }

            if (entity != null) {
                movingobjectposition = new MovingObjectPosition(entity);
            }
        }
        if (movingobjectposition != null) {
            detonate();
            setDead();
        }
        posX += motionX;
        posY += motionY;
        posZ += motionZ;
        float f = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ);
        rotationYaw = (float) ((Math.atan2(motionX, motionZ) * 180D) / 3.1415927410125732D);
        for (rotationPitch = (float) ((Math.atan2(motionY, f) * 180D) / 3.1415927410125732D); rotationPitch - prevRotationPitch < -180F; prevRotationPitch -= 360F) { }
        for (; rotationPitch - prevRotationPitch >= 180F; prevRotationPitch += 360F) { }
        for (; rotationYaw - prevRotationYaw < -180F; prevRotationYaw -= 360F) { }
        for (; rotationYaw - prevRotationYaw >= 180F; prevRotationYaw += 360F) { }
        rotationPitch = prevRotationPitch + (rotationPitch - prevRotationPitch) * 0.2F;
        rotationYaw = prevRotationYaw + (rotationYaw - prevRotationYaw) * 0.2F;
        float f1 = 0.99F;
        float f2 = 0.03F;
        if (isInWater()) {
            for (int k = 0; k < 4; k++) {
                float f3 = 0.25F;
                worldObj.spawnParticle("bubble", posX - motionX * f3, posY - motionY * f3, posZ - motionZ * f3, motionX, motionY, motionZ);
            }

            f1 = 0.8F;
        }
        motionX *= f1;
        motionY *= f1;
        motionZ *= f1;
        motionY -= f2;
        setPosition(posX, posY, posZ);
    }

    public void detonate() {
        if (worldObj.isRemote)
            return;

        int boltsgen = 0;
        List<Entity> entities = worldObj.getEntitiesWithinAABBExcludingEntity(this, AxisAlignedBB.getBoundingBox(posX - 10, posY - 10, posZ - 10, posX + 10, posY + 10, posZ + 10));
        for (Iterator<Entity> iterator = entities.iterator(); iterator.hasNext(); ) {
            if (boltsgen > maxbolts) {
                break;
            }
            Entity target = iterator.next();

            if (!(target instanceof EntityLivingBase) || Vector3.fromEntity(this).subtract(Vector3.fromEntity(target)).magSquared() > 100) {
                continue;
            }

            WirelessBolt bolt = new WirelessBolt(worldObj, Vector3.fromEntity(this), Vector3.fromEntity(target), worldObj.rand.nextLong());
            bolt.defaultFractal();
            bolt.finalizeBolt();
            bolt = new WirelessBolt(worldObj, Vector3.fromEntity(this), Vector3.fromEntity(target), worldObj.rand.nextLong());
            bolt.defaultFractal();
            bolt.finalizeBolt();
            boltsgen += 2;
        }

        TreeSet<BlockCoord> nodes = RedstoneEther.server().getNodesInRangeofPoint(CommonUtils.getDimension(worldObj), Vector3.fromEntity(this), RedstoneEther.jammerrange, true);
        for (Iterator<BlockCoord> iterator = nodes.iterator(); iterator.hasNext(); ) {
            if (boltsgen > maxbolts) {
                break;
            }
            BlockCoord node = iterator.next();
            ITileWireless tile = (ITileWireless) RedstoneEther.getTile(worldObj, node);

            WirelessBolt bolt = new WirelessBolt(worldObj, Vector3.fromEntity(this), tile, worldObj.rand.nextLong());
            bolt.defaultFractal();
            bolt.finalizeBolt();
            boltsgen++;
        }

        for (int i = 0; i < 16; i++) {
            if (boltsgen > maxbolts) {
                break;
            }
            WirelessBolt bolt = new WirelessBolt(worldObj, Vector3.fromEntity(this), new Vector3(
                    posX + 20 * worldObj.rand.nextFloat() - 10,
                    posY + 20 * worldObj.rand.nextFloat() - 10,
                    posZ + 20 * worldObj.rand.nextFloat() - 10), worldObj.rand.nextLong());
            bolt.defaultFractal();
            bolt.finalizeBolt();
            boltsgen++;
        }
    }

    @Override
    public void setDead() {
        super.setDead();
        RedstoneEtherAddons.get(worldObj.isRemote).invalidateREP((EntityPlayer) shootingEntity);
        if (!worldObj.isRemote)
            WRAddonSPH.sendKillREP(this);
    }

    public void writeEntityToNBT(NBTTagCompound tag) {
        tag.setShort("xTile", (short) xTileREP);
        tag.setShort("yTile", (short) yTileREP);
        tag.setShort("zTile", (short) zTileREP);
        tag.setShort("inTile", (short) Block.getIdFromBlock(inTileREP));
        tag.setByte("shake", (byte) shakeREP);
        tag.setByte("inGround", (byte) (inGroundREP ? 1 : 0));
    }

    public void readEntityFromNBT(NBTTagCompound tag) {
        xTileREP = tag.getShort("xTile");
        yTileREP = tag.getShort("yTile");
        zTileREP = tag.getShort("zTile");
        inTileREP = Block.getBlockById(tag.getShort("inTile") & 0xFFFF);
        shakeREP = tag.getByte("shake") & 0xff;
        inGroundREP = tag.getByte("inGround") == 1;
    }

    public float getShadowSize() {
        return 0.0F;
    }

    private int xTileREP;
    private int yTileREP;
    private int zTileREP;
    private Block inTileREP;
    private boolean inGroundREP;
    public int shakeREP;
    public EntityLivingBase shootingEntity;
    private int ticksInGroundREP;
    private int ticksInAirREP;

    public final int maxbolts = 50;
}
