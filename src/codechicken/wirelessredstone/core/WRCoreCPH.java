package codechicken.wirelessredstone.core;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IClientPacketHandler;
import codechicken.lib.vec.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.play.INetHandlerPlayClient;

import java.util.LinkedList;
import java.util.List;

public class WRCoreCPH implements IClientPacketHandler
{
    public static List<IClientPacketHandler> delegates = new LinkedList<IClientPacketHandler>();

    @Override
    public void handlePacket(PacketCustom packet, Minecraft mc, INetHandlerPlayClient handler) {
        handlePacket(mc.theWorld, mc.thePlayer, packet);
        for (IClientPacketHandler delegate : delegates)
            delegate.handlePacket(packet, mc, handler);
    }

    private void handlePacket(WorldClient world, EntityPlayer player, PacketCustom packet) {
        switch (packet.getType()) {
            case 1:
                handleFreqInfoList(packet);
                break;
            case 2:
                handleLastFreqInfo(packet.readUShort(), packet.readUByte());
                break;
            case 3:
                RedstoneEther.get(true).setFrequencyRange(player.getCommandSenderName(), packet.readUShort(), packet.readUShort(), packet.readBoolean());
                break;
            case 4:
                handleFreqInfo(packet);
                break;
            case 7:
                RedstoneEther.client().jamEntity(player, packet.readBoolean());
                break;
            case 8:
                WirelessBolt bolt = new WirelessBolt(world,
                        new Vector3(packet.readFloat(), packet.readFloat(), packet.readFloat()),
                        new Vector3(packet.readFloat(), packet.readFloat(), packet.readFloat()),
                        packet.readLong());

                bolt.defaultFractal();
                bolt.finalizeBolt();
                break;
            case 9:
                RedstoneEther.get(true).setFreqOwner(packet.readShort(), packet.readString());
                break;
            case 10:
                handleFreqOwnerList(packet);
                break;
        }
    }

    private void handleFreqOwnerList(PacketCustom packet) {
        int numFreqs = packet.readUShort();
        for (int i = 0; i < numFreqs; i++)
            RedstoneEther.get(true).setFreqOwner(packet.readShort(), packet.readString());
    }

    private void handleFreqInfoList(PacketCustom packet) {
        int numFreqs = packet.readUShort();
        for (int i = 0; i < numFreqs; i++)
            handleFreqInfo(packet);
    }

    private void handleFreqInfo(PacketCustom packet) {
        int freq = packet.readUShort();
        RedstoneEther.get(true).setFreqColour(freq, packet.readByte());
        RedstoneEther.get(true).setFreqName(freq, packet.readString());
    }

    private void handleLastFreqInfo(int freq, int type) {
        switch (type) {
            case 1:
                RedstoneEther.get(true).setLastPublicFrequency(freq);
                break;
            case 2:
                RedstoneEther.get(true).setLastSharedFrequency(freq);
                break;
        }
    }

    public static void sendSetTileFreq(int x, int y, int z, int freq) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 1);
        packet.writeCoord(x, y, z);
        packet.writeShort(freq);
        packet.sendToServer();
    }

    public static void sendSetFreqInfo(int freq, String name, int colourid) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 4);
        packet.writeShort((short) freq);
        packet.writeString(name);
        packet.writeByte((byte) colourid);
        packet.sendToServer();
    }

    public static void sendDecrementSlot(int slot) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 5);
        packet.writeShort(slot);
        packet.sendToServer();
    }

    public static void sendSetFreqOwner(int freq, String username) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 9);
        packet.writeShort(freq);
        packet.writeString(username);
        packet.sendToServer();
    }

    public static void sendSetItemFreq(int slot, int freq) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 2);
        packet.writeShort(slot);
        packet.writeShort(freq);
        packet.sendToServer();
    }
}
