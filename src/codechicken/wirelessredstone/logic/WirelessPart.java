package codechicken.wirelessredstone.logic;

import java.util.Arrays;

import codechicken.core.ClientUtils;
import codechicken.core.asm.InterfaceDependancies;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.RedundantTransformation;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.FaceMicroClass;
import codechicken.microblock.JMicroShrinkRender;
import codechicken.microblock.MicroOcclusion;
import codechicken.multipart.IFaceRedstonePart;
import codechicken.multipart.IRedstonePart;
import codechicken.multipart.IconHitEffects;
import codechicken.multipart.JCuboidPart;
import codechicken.multipart.JIconHitEffects;
import codechicken.multipart.JNormalOcclusion;
import codechicken.multipart.JPartialOcclusion;
import codechicken.multipart.NormalOcclusionTest;
import codechicken.multipart.RedstoneInteractions;
import codechicken.multipart.TFacePart;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.wirelessredstone.core.RedstoneEther;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;

import static codechicken.lib.vec.Rotation.*;
import static codechicken.lib.vec.Vector3.*;

@InterfaceDependancies
public abstract class WirelessPart extends JCuboidPart implements TFacePart, JIconHitEffects, IFaceRedstonePart, JNormalOcclusion, JPartialOcclusion, JMicroShrinkRender
{
    private static Cuboid6[] nBoxes = new Cuboid6[6];

    static {
        Cuboid6 base = new Cuboid6(1 / 8D, 0, 1 / 8D, 7 / 8D, 1 / 8D, 7 / 8D);
        for (int s = 0; s < 6; s++)
            nBoxes[s] = base.copy().apply(sideRotations[s].at(center));
    }

    public byte state;
    public String owner;

    //rendering
    public Cuboid6 baseRenderBounds;
    public int baseRenderMask;
    protected int spinoffset;

    public int rotation() {
        return state & 3;
    }

    public void setRotation(int rot) {
        state = (byte) ((state & 0xFC) | rot);
    }

    public int side() {
        return state >> 2 & 7;
    }

    public void setSide(int side) {
        state = (byte) ((state & 0xE3) | side << 2);
    }

    public int shape() {
        return state & 0x1F;
    }

    /**
     * Whether the device is currently operational (powered for transmitters, receiving, or unpowered for jammers)
     */
    public boolean active() {
        return (state & 0x20) > 0;
    }

    public void setActive(boolean active) {
        state &= 0xDF;
        if (active)
            state |= 0x20;
    }

    public boolean disabled() {
        return (state & 0x40) > 0;
    }

    public void setDisabled(boolean disabled) {
        state &= 0xBF;
        if (disabled)
            state |= 0x40;
    }

    public int getPoweringLevel() {
        int s = RedstoneInteractions.getPowerTo(this, Rotation.rotateSide(side(), rotation()));
        int i = getInternalPower();
        if (i > s)
            s = i;

        return s;
    }

    public int getInternalPower() {
        TMultiPart part = tile().partMap(Rotation.rotateSide(side(), rotation()));
        if (part instanceof IRedstonePart) {
            IRedstonePart rp = (IRedstonePart) part;
            return Math.max(rp.strongPowerLevel(side()), rp.weakPowerLevel(side())) << 4;
        }

        return 0;
    }

    public Transformation rotationT() {
        return sideOrientation(side(), rotation());
    }

    @Override
    public void load(NBTTagCompound tag) {
        state = tag.getByte("state");
        if (tag.hasKey("owner"))
            owner = tag.getString("owner");
    }

    @Override
    public void save(NBTTagCompound tag) {
        tag.setByte("state", state);
        if (owner != null)
            tag.setString("owner", owner);
    }

    @Override
    public void readDesc(MCDataInput packet) {
        state = packet.readByte();
    }

    @Override
    public void read(MCDataInput packet) {
        super.read(packet);
        onPartChanged(this);
    }

    @Override
    public void writeDesc(MCDataOutput packet) {
        packet.writeByte(state);
    }

    @Override
    public void onWorldJoin() {
        if (!world().isRemote)
            addToEther();
    }

    @Override
    public void onWorldSeparate() {
        if (!world().isRemote)
            removeFromEther();
    }

    @Override
    public int getFace() {
        return side();
    }

    @Override
    public boolean canConnectRedstone(int side) {
        return side == rotateSide(side(), rotation());
    }

    @Override
    public void onNeighborChanged() {
        dropIfCantStay();
    }

    @Override
    public ItemStack pickItem(MovingObjectPosition hit) {
        return getItem();
    }

    @Override
    public Iterable<ItemStack> getDrops() {
        return Arrays.asList(getItem());
    }

    @Override
    public int strongPowerLevel(int side) {
        return 0;
    }

    @Override
    public int weakPowerLevel(int side) {
        return strongPowerLevel(side);
    }

    public void updateChange() {
        tile().markDirty();
        tile().notifyPartChange(this);
        sendDescUpdate();
    }

    public boolean dropIfCantStay() {
        BlockCoord pos = new BlockCoord(tile()).offset(side());
        if (!world().isSideSolid(pos.x, pos.y, pos.z, ForgeDirection.getOrientation(side() ^ 1))) {
            drop();
            return true;
        }
        return false;
    }

    public void drop() {
        TileMultipart.dropItem(getItem(), world(), Vector3.fromTileEntityCenter(tile()));
        tile().remPart(this);
    }

    public void setupPlacement(EntityPlayer player, int side) {
        setSide(side ^ 1);
        setRotation(Rotation.getSidedRotation(player, side) ^ 2);
        owner = player.getCommandSenderName();
    }

    public abstract ItemStack getItem();

    public abstract void addToEther();

    public abstract void removeFromEther();

    public abstract Cuboid6 getExtensionBB();

    public abstract int modelId();

    public int textureSet() {
        return active() ? 1 : 0;
    }

    public abstract Vector3 getPearlPos();

    public Transformation getPearlRotation() {
        return new RedundantTransformation();
    }

    public double getPearlScale() {
        return 0.05;
    }

    public abstract double getPearlSpin();

    public abstract float getPearlLight();

    public double getFloating() {
        if (tile() != null)
            return RedstoneEther.getSineWave(ClientUtils.getRenderTime() + x() * 3 + y() * 5 + z() * 9, 7);
        return 0;
    }

    @Override
    public boolean allowCompleteOcclusion() {
        return false;
    }

    @Override
    public Cuboid6 getBounds() {
        return baseBounds(side());
    }

    @Override
    public Iterable<Cuboid6> getPartialOcclusionBoxes() {
        return getCollisionBoxes();
    }

    @Override
    public Iterable<Cuboid6> getOcclusionBoxes() {
        return Arrays.asList(nBoxes[side()], getExtensionBB());
    }

    @Override
    public boolean occlusionTest(TMultiPart npart) {
        return NormalOcclusionTest.apply(this, npart);
    }

    @Override
    public Iterable<IndexedCuboid6> getSubParts() {
        return Arrays.asList(new IndexedCuboid6(0, getBounds()));
    }

    @Override
    public float getStrength(MovingObjectPosition hit, EntityPlayer player) {
        return 10F;
    }

    @Override
    public boolean renderStatic(Vector3 pos, int pass) {
        if (pass == 0) {
            RenderWireless.renderWorld(this);
            return true;
        }
        return false;
    }

    @Override
    public void renderDynamic(Vector3 pos, float frame, int pass) {
        if (pass == 0)
            RenderWireless.renderPearl(pos, this);
    }

    @Override
    public void onPartChanged(TMultiPart part) {
        if (world().isRemote) {
            recalcBounds();
        } else {
            onNeighborChanged();
        }
    }

    @Override
    public void onAdded() {
        super.onAdded();
        if (world().isRemote)
            recalcBounds();
    }

    public void recalcBounds() {
        baseRenderBounds = getBounds().copy();
        baseRenderMask = MicroOcclusion.recalcBounds(this, baseRenderBounds);
        baseRenderBounds = baseRenderBounds.apply(rotationT().at(center).inverse());
    }

    @Override
    public int getSlotMask() {
        return 1 << getSlot();
    }

    @Override
    public boolean solid(int side) {
        return false;
    }

    @Override
    public int redstoneConductionMap() {
        return 0;
    }

    @Override
    public int getPriorityClass() {
        return -1;
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public int getSlot() {
        return side();
    }

    @Override
    public boolean isTransparent() {
        return false;
    }

    public static Cuboid6 baseBounds(int i) {
        return FaceMicroClass.aBounds()[0x10 | i];
    }

    @Override
    public boolean activate(EntityPlayer player, MovingObjectPosition hit, ItemStack held) {
        if (hit.sideHit == (side() ^ 1) && player.isSneaking()) {
            int r = rotation();
            setRotation((r + 1) % 4);
            if (!tile().canReplacePart(this, this)) {
                setRotation(r);
                return false;
            }

            if (!world().isRemote) {
                updateChange();
                onPartChanged(this);
            } else {
                setRotation(r);
            }
            return true;
        }
        return false;
    }

    @Override
    public void addDestroyEffects(MovingObjectPosition hit, EffectRenderer effectRenderer) {
        IconHitEffects.addDestroyEffects(this, effectRenderer);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addHitEffects(MovingObjectPosition hit, EffectRenderer effectRenderer) {
        IconHitEffects.addHitEffects(this, hit, effectRenderer);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getBreakingIcon(Object subPart, int side) {
        return getBrokenIcon(side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getBrokenIcon(int side) {
        return RenderWireless.getBreakingIcon(textureSet());
    }
}
