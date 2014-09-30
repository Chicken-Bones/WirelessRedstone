package codechicken.wirelessredstone.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IServerPacketHandler;
import codechicken.lib.vec.BlockCoord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class WRCoreSPH implements IServerPacketHandler
{
    public static List<IServerPacketHandler> delegates = new LinkedList<IServerPacketHandler>();

    @Override
    public void handlePacket(PacketCustom packet, EntityPlayerMP sender, INetHandlerPlayServer handler) {
        handlePacket((WorldServer) sender.worldObj, sender, packet);
        for(IServerPacketHandler delegate : delegates)
            delegate.handlePacket(packet, sender, handler);
    }

    private void handlePacket(WorldServer world, EntityPlayerMP player, PacketCustom packet) {
        switch (packet.getType()) {
            case 1:
                setTileFreq(player, world, packet.readCoord(), packet.readShort());
                break;
            case 2:
                setItemFreq(player, packet.readShort(), packet.readShort());
                break;
            case 4:
                handleFreqInfo(packet);
                break;
            case 5:
                decrementSlot(player, packet.readShort());
                break;
            case 9:
                RedstoneEther.get(false).setFreqOwner(packet.readShort(), packet.readString());
                break;
        }
    }

    private void decrementSlot(EntityPlayerMP player, int slot) {
        try {
            ItemStack item = player.inventory.mainInventory[slot];
            item.stackSize--;

            if (item.stackSize == 0) {
                player.inventory.mainInventory[slot] = null;
            }
        } catch (ArrayIndexOutOfBoundsException e) {}
    }

    private void setItemFreq(EntityPlayerMP sender, int slot, int freq) {
        if (RedstoneEther.get(false).canBroadcastOnFrequency(sender, freq)) {
            ItemStack stack = sender.inventory.mainInventory[slot];
            if (stack != null && stack.getItem() instanceof ItemWirelessFreq) {
                ((ItemWirelessFreq) stack.getItem()).setFreq(sender, slot, stack, freq);
            }
        }
    }

    private void setTileFreq(EntityPlayer sender, World world, BlockCoord pos, int freq) {
        if (RedstoneEther.get(false).canBroadcastOnFrequency(sender, freq)) {
            TileEntity tile = RedstoneEther.getTile(world, pos);
            if (tile instanceof ITileWireless)
                RedstoneEther.get(false).setFreq((ITileWireless) tile, freq);
        }
    }

    private void handleFreqInfo(PacketCustom packet) {
        int freq = packet.readUShort();
        String name = packet.readString();
        int colourid = packet.readUByte();

        RedstoneEther.get(false).setFreqName(freq, name);
        RedstoneEther.get(false).setFreqColour(freq, colourid);

        sendSetFreqInfoTo(null, freq, name, colourid);
    }

    public static void sendSetFrequencyRangeTo(EntityPlayer player, int startfreq, int endfreq, boolean jam) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 3);
        packet.writeShort((short) startfreq);
        packet.writeShort((short) endfreq);
        packet.writeBoolean(jam);

        packet.sendToPlayer(player);
    }

    public static void sendPublicFrequencyTo(EntityPlayer player, int freq) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 2);
        packet.writeShort(freq);
        packet.writeByte(1);

        packet.sendToPlayer(player);
    }

    public static void sendSharedFrequencyTo(EntityPlayer player, int freq) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 2);
        packet.writeShort(freq);
        packet.writeByte(2);

        packet.sendToPlayer(player);
    }

    public static void sendSetFreqInfoTo(EntityPlayer player, int freq, String name, int colourid) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 4);
        packet.writeShort(freq);
        packet.writeByte(colourid);
        packet.writeString(name);

        packet.sendToPlayer(player);
    }

    public static void sendJamPlayerPacketTo(EntityPlayer player, boolean jam) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 7);
        packet.writeBoolean(jam);

        packet.sendToPlayer(player);
    }

    public static void sendWirelessBolt(WirelessBolt bolt) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 8);
        packet.writeFloat((float) bolt.start.x);
        packet.writeFloat((float) bolt.start.y);
        packet.writeFloat((float) bolt.start.z);
        packet.writeFloat((float) bolt.end.x);
        packet.writeFloat((float) bolt.end.y);
        packet.writeFloat((float) bolt.end.z);
        packet.writeLong(bolt.seed);

        packet.sendToChunk(bolt.world, (int) bolt.start.x >> 4, (int) bolt.start.z >> 4);
    }

    public static void sendSetSlot(int slot, ItemStack stack) {
    }

    public static void sendFreqInfoTo(EntityPlayer player, ArrayList<Integer> freqsWithInfo) {
        if (freqsWithInfo.size() == 0)
            return;

        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 1);
        packet.writeShort(freqsWithInfo.size());
        for (int freq : freqsWithInfo) {
            packet.writeShort(freq);
            packet.writeByte(RedstoneEther.get(false).getFreqColourId(freq));
            packet.writeString(RedstoneEther.get(false).getFreqName(freq));
        }
        packet.sendToPlayer(player);
    }

    public static void sendFreqOwnerTo(EntityPlayer player, ArrayList<Integer> freqsWithOwners) {
        if (freqsWithOwners.size() == 0)
            return;

        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 10);
        packet.writeShort(freqsWithOwners.size());
        for (int freq : freqsWithOwners) {
            packet.writeShort(freq);
            packet.writeString(RedstoneEther.get(false).getFreqOwner(freq));
        }
        packet.sendToPlayer(player);
    }

    public static void sendSetFreqOwner(int freq, String username) {
        PacketCustom packet = new PacketCustom(WirelessRedstoneCore.channel, 9);
        packet.writeShort(freq);
        packet.writeString(username);

        packet.sendToClients();
    }
}
