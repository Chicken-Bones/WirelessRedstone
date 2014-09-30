package codechicken.wirelessredstone.addons;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IClientPacketHandler;
import codechicken.wirelessredstone.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.world.World;

public class WRAddonCPH implements IClientPacketHandler
{
    @Override
    public void handlePacket(PacketCustom packet, Minecraft mc, INetHandlerPlayClient handler) {
        handlePacket(mc.theWorld, mc.thePlayer, packet);
    }

    private void handlePacket(WorldClient world, EntityPlayer player, PacketCustom packet) {
        switch (packet.getType()) {
            case 53:
                processSnifferFreqUpdate(packet);
                break;
            case 54:
                processSnifferEtherCopy(packet);
                break;
            case 55:
                RedstoneEtherAddons.client().setTriangAngle(packet.readUShort(), packet.readFloat());
                break;
            case 56:
                processMapInfo(world, player, packet);
                break;
            case 57:
                processMapUpdate(world, player, packet);
                break;
            case 59:
                if (packet.readBoolean())
                    throwREP(packet.readInt(), packet.readInt(), world, player);
                else
                    world.removeEntityFromWorld(packet.readInt());
                break;
            case 60:
                processTrackerUpdate(packet, world, player);
                break;
            case 61:
                if (packet.readBoolean())
                    throwTracker(world, player, packet.readInt(), packet.readInt(), packet.readUShort());
                else
                    world.removeEntityFromWorld(packet.readInt());
                break;
        }
    }

    private void throwTracker(WorldClient world, EntityPlayer player, int entityID, int throwerID, int freq) {
        Entity thrower = world.getEntityByID(throwerID);
        if (throwerID == player.getEntityId())
            thrower = player;

        if (thrower != null && thrower instanceof EntityLiving) {
            EntityWirelessTracker tracker = new EntityWirelessTracker(world, 0, (EntityLiving) thrower);
            tracker.setEntityId(entityID);
            world.addEntityToWorld(entityID, tracker);
            world.playSoundAtEntity(thrower, "random.bow", 0.5F, 0.4F / (world.rand.nextFloat() * 0.4F + 0.8F));
        }
    }

    private void processTrackerUpdate(PacketCustom packet, WorldClient world, EntityPlayer player) {
        int entityID = packet.readInt();
        int freq = packet.readUShort();
        boolean attached = packet.readBoolean();

        Entity e = world.getEntityByID(entityID);
        if (e != null && e.isDead)
            e = null;

        if (!(e instanceof EntityWirelessTracker)) {
            if (e != null)
                throw new IllegalStateException("EntityID mapped to non tracker");

            e = new EntityWirelessTracker(world, freq);
            e.setEntityId(entityID);
            world.addEntityToWorld(entityID, e);
        }
        EntityWirelessTracker tracker = (EntityWirelessTracker) e;

        if (attached) {
            int attachedEntityID = packet.readInt();

            Entity attachedEntity;
            if (attachedEntityID == player.getEntityId())
                attachedEntity = player;
            else
                attachedEntity = world.getEntityByID(attachedEntityID);

            if (attachedEntity == null) {
                return;
            }

            tracker.attached = true;
            tracker.attachedEntity = attachedEntity;
            tracker.attachedX = packet.readFloat();
            tracker.attachedY = packet.readFloat();
            tracker.attachedZ = packet.readFloat();
            tracker.attachedYaw = packet.readFloat();
        } else {
            tracker.attachedEntity = null;
            tracker.attached = false;

            tracker.posX = packet.readFloat();
            tracker.posY = packet.readFloat();
            tracker.posZ = packet.readFloat();
            tracker.motionX = packet.readFloat();
            tracker.motionY = packet.readFloat();
            tracker.motionZ = packet.readFloat();

            tracker.setPosition(tracker.posX, tracker.posY, tracker.posZ);
            tracker.setVelocity(tracker.motionX, tracker.motionY, tracker.motionZ);

            tracker.attachmentCounter = packet.readUShort();
            tracker.item = packet.readBoolean();
        }
    }

    private void throwREP(int entityID, int throwerID, WorldClient world, EntityPlayer player) {
        Entity thrower = world.getEntityByID(throwerID);
        if (throwerID == player.getEntityId())
            thrower = player;

        if (thrower != null && thrower instanceof EntityLivingBase) {
            EntityREP rep = new EntityREP(world, (EntityLivingBase) thrower);
            rep.setEntityId(entityID);
            world.addEntityToWorld(entityID, rep);
        }
    }

    private static void processSnifferFreqUpdate(PacketCustom packet) {
        GuiScreen currentscreen = Minecraft.getMinecraft().currentScreen;
        if (currentscreen == null || !(currentscreen instanceof GuiWirelessSniffer))
            return;

        GuiWirelessSniffer sniffergui = ((GuiWirelessSniffer) currentscreen);
        sniffergui.setEtherFreq(packet.readUShort(), packet.readBoolean());
    }

    private static void processSnifferEtherCopy(PacketCustom packet) {
        GuiScreen currentscreen = Minecraft.getMinecraft().currentScreen;
        if (currentscreen == null || !(currentscreen instanceof GuiWirelessSniffer))
            return;

        GuiWirelessSniffer sniffergui = ((GuiWirelessSniffer) currentscreen);
        sniffergui.setEtherCopy(packet.readByteArray(packet.readUShort()));
    }

    private static void processMapUpdate(World world, EntityPlayer player, PacketCustom packet) {
        WirelessMapNodeStorage mapstorage = RedstoneEtherAddons.client().getMapNodes();
        int numaddednodes = packet.readUShort();
        for (int i = 0; i < numaddednodes; i++) {
            FreqCoord node = new FreqCoord(packet.readShort(), -1, packet.readShort(), packet.readUShort());
            mapstorage.nodes.add(node);
        }

        int numremovednodes = packet.readUShort();
        for (int i = 0; i < numremovednodes; i++) {
            FreqCoord node = new FreqCoord(packet.readShort(), -1, packet.readShort(), packet.readUShort());
            mapstorage.nodes.remove(node);
        }

        int numremotes = packet.readUShort();
        mapstorage.devices.clear();
        for (int i = 0; i < numremotes; i++) {
            mapstorage.devices.add(new FreqCoord(packet.readInt(), -1, packet.readInt(), packet.readUShort()));
        }
    }

    private static void processMapInfo(World world, EntityPlayer player, PacketCustom packet) {
        short mapno = packet.readShort();
        int xCenter = packet.readInt();
        int zCenter = packet.readInt();
        byte scale = packet.readByte();
        RedstoneEtherAddons.client().setMPMapInfo(mapno, new ClientMapInfo(xCenter, zCenter, scale));
    }

    public static void sendOpenSniffer() {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 50);
        packet.writeBoolean(true);

        packet.sendToServer();
    }

    public static void sendCloseSniffer() {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 50);
        packet.writeBoolean(false);

        packet.sendToServer();
    }

    public static void sendSetRemote(boolean active) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 51);
        packet.writeBoolean(active);

        packet.sendToServer();
    }

    public static void sendSyncTriang(int freq, boolean required) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 52);
        packet.writeShort(freq);
        packet.writeBoolean(required);

        packet.sendToServer();
    }

    public static void sendResetMap() {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 58);

        packet.sendToServer();
    }
}
