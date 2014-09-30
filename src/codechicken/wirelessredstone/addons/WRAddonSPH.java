package codechicken.wirelessredstone.addons;

import java.util.TreeSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapData;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IServerPacketHandler;
import codechicken.wirelessredstone.core.*;

public class WRAddonSPH implements IServerPacketHandler
{
    @Override
    public void handlePacket(PacketCustom packet, EntityPlayerMP sender, INetHandlerPlayServer handler) {
        handlePacket((WorldServer) sender.worldObj, sender, packet);
    }

    private void handlePacket(WorldServer world, EntityPlayerMP player, PacketCustom packet) {
        switch (packet.getType()) {
            case 50:
                if (packet.readBoolean())
                    RedstoneEtherAddons.server().addSniffer(player);
                else
                    RedstoneEtherAddons.server().remSniffer(player);
                break;
            case 51:
                if (packet.readBoolean())
                    RedstoneEtherAddons.server().activateRemote(world, player);
                else
                    RedstoneEtherAddons.server().deactivateRemote(world, player);
                break;
            case 52:
                RedstoneEtherAddons.server().setTriangRequired(player, packet.readUShort(), packet.readBoolean());
                break;
            case 58:
                RedstoneEtherAddons.server().clearMapNodes(player);
                break;
        }
    }

    public static void sendUpdateSnifferTo(EntityPlayer player, int freq, boolean on) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 53);
        packet.writeShort((short) freq);
        packet.writeBoolean(on);

        packet.sendToPlayer(player);
    }

    public static void sendEtherCopyTo(EntityPlayer player, byte[] ethercopy) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 54);
        packet.writeShort(ethercopy.length);
        packet.writeByteArray(ethercopy);

        packet.sendToPlayer(player);
    }

    public static void sendTriangAngleTo(EntityPlayer player, int freq, float angle) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 55);
        packet.writeShort((short) freq);
        packet.writeFloat(angle);

        packet.sendToPlayer(player);
    }

    public static void sendMapUpdatePacketTo(EntityPlayer player, int mapno, MapData mapdata, TreeSet<FreqCoord> addednodes, TreeSet<FreqCoord> removednodes, TreeSet<FreqCoord> remotes) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 57);
        packet.writeShort((short) addednodes.size());
        for (FreqCoord node : addednodes) {
            packet.writeShort((short) node.x);
            packet.writeShort((short) node.z);
            packet.writeShort((short) (node.freq));
        }

        packet.writeShort((short) removednodes.size());
        for (FreqCoord node : removednodes) {
            packet.writeShort((short) node.x);
            packet.writeShort((short) node.z);
            packet.writeShort((short) (node.freq));
        }

        packet.writeShort((short) remotes.size());
        for (FreqCoord node : remotes) {
            packet.writeInt(node.x);
            packet.writeInt(node.z);
            packet.writeShort((short) (node.freq));
        }

        packet.sendToPlayer(player);
    }

    public static void sendMapInfoTo(EntityPlayer player, int mapno, MapData mapdata) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 56);
        packet.writeShort((short) mapno);
        packet.writeInt(mapdata.xCenter);
        packet.writeInt(mapdata.zCenter);
        packet.writeByte(mapdata.scale);

        packet.sendToPlayer(player);
    }

    public static void sendSpawnREP(EntityREP activeREP) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 59);
        packet.writeBoolean(true);
        packet.writeInt(activeREP.getEntityId());
        packet.writeInt(activeREP.shootingEntity.getEntityId());

        packet.sendToChunk(activeREP.worldObj, (int) activeREP.posX >> 4, (int) activeREP.posZ >> 4);
    }

    public static void sendKillREP(EntityREP entityREP) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 59);
        packet.writeBoolean(false);
        packet.writeInt(entityREP.getEntityId());

        packet.sendToChunk(entityREP.worldObj, (int) entityREP.posX >> 4, (int) entityREP.posZ >> 4);
    }

    public static void sendTrackerUpdatePacketTo(EntityPlayerMP player, EntityWirelessTracker tracker) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 60);
        packet.writeInt(tracker.getEntityId());
        packet.writeShort(tracker.freq);
        packet.writeBoolean(tracker.isAttachedToEntity());
        if (tracker.isAttachedToEntity()) {
            packet.writeInt(tracker.attachedEntity.getEntityId());
            packet.writeFloat(tracker.attachedX);
            packet.writeFloat(tracker.attachedY);
            packet.writeFloat(tracker.attachedZ);
            packet.writeFloat(tracker.attachedYaw);
        } else {
            packet.writeFloat((float) tracker.posX);
            packet.writeFloat((float) tracker.posY);
            packet.writeFloat((float) tracker.posZ);
            packet.writeFloat((float) tracker.motionX);
            packet.writeFloat((float) tracker.motionY);
            packet.writeFloat((float) tracker.motionZ);
            packet.writeShort(tracker.attachmentCounter);
            packet.writeBoolean(tracker.item);
        }

        packet.sendToPlayer(player);
    }

    public static void sendRemoveTrackerTo(EntityPlayerMP player, EntityWirelessTracker tracker) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 61);
        packet.writeBoolean(false);
        packet.writeInt(tracker.getEntityId());

        packet.sendToPlayer(player);
    }

    public static void sendThrowTracker(EntityWirelessTracker tracker, EntityPlayer thrower) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 61);
        packet.writeBoolean(true);
        packet.writeInt(tracker.getEntityId());
        packet.writeInt(thrower.getEntityId());
        packet.writeShort(tracker.freq);

        packet.sendToChunk(thrower.worldObj, (int) thrower.posX >> 4, (int) thrower.posZ >> 4);
    }
}
