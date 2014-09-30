package codechicken.wirelessredstone.addons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import codechicken.core.CommonUtils;
import codechicken.core.ServerUtils;
import codechicken.lib.math.MathHelper;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;
import codechicken.wirelessredstone.core.FreqCoord;
import codechicken.wirelessredstone.core.RedstoneEther;
import codechicken.wirelessredstone.core.RedstoneEther.TXNodeInfo;
import codechicken.wirelessredstone.core.WirelessTransmittingDevice;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.World;

public class RedstoneEtherServerAddons extends RedstoneEtherAddons
{
    private HashMap<String, AddonPlayerInfo> playerInfos = new HashMap<String, AddonPlayerInfo>();
    /**
     * A list of trackers and the players who are 'tracking' them on their clients.
     */
    private HashMap<EntityWirelessTracker, HashSet<EntityPlayerMP>> trackerPlayerMap = new HashMap<EntityWirelessTracker, HashSet<EntityPlayerMP>>();
    /**
     * Trackers that are attached to players.
     */
    private HashSet<EntityWirelessTracker> playerTrackers = new HashSet<EntityWirelessTracker>();

    public void setTriangRequired(EntityPlayer player, int freq, boolean required) {
        AddonPlayerInfo info = getPlayerInfo(player);
        if (required)
            info.triangSet.add(freq);
        else
            info.triangSet.remove(freq);
    }

    private AddonPlayerInfo getPlayerInfo(EntityPlayer player) {
        return playerInfos.get(player.getCommandSenderName());
    }

    public boolean isRemoteOn(EntityPlayer player, int freq) {
        Remote currentremote = getPlayerInfo(player).remote;
        return currentremote != null && currentremote.getFreq() == freq;
    }

    public int getRemoteFreq(EntityPlayer player) {
        Remote currentremote = getPlayerInfo(player).remote;
        if (currentremote == null) {
            return 0;
        }

        return currentremote.getFreq();
    }

    public void activateRemote(World world, EntityPlayer player) {
        AddonPlayerInfo info = getPlayerInfo(player);
        if (info.remote != null) {
            if (info.remote.isBeingHeld()) {
                return;
            }
            deactivateRemote(world, player);
        }
        if (RedstoneEther.server().isPlayerJammed(player))
            return;
        info.remote = new Remote(player);
        info.remote.metaOn();
        RedstoneEther.server().addTransmittingDevice(info.remote);
    }

    public boolean deactivateRemote(World world, EntityPlayer player) {
        AddonPlayerInfo info = getPlayerInfo(player);
        if (info.remote == null)
            return false;

        info.remote.metaOff();
        RedstoneEther.server().removeTransmittingDevice(info.remote);
        info.remote = null;
        return true;
    }

    public void addSniffer(EntityPlayer player) {
        AddonPlayerInfo info = getPlayerInfo(player);
        if (info.sniffer != null) {
            remSniffer(player);
        }
        info.sniffer = new Sniffer(player);
        RedstoneEther.server().addReceivingDevice(info.sniffer);

        byte ethercopy[] = new byte[625];
        for (int freq = 1; freq <= 5000; freq++) {
            int arrayindex = (freq - 1) >> 3;
            int bit = (freq - 1) & 7;
            if (RedstoneEther.server().isFreqOn(freq)) {
                ethercopy[arrayindex] |= 1 << bit;
            }
        }

        WRAddonSPH.sendEtherCopyTo(player, ethercopy);
    }

    public void remSniffer(EntityPlayer player) {
        AddonPlayerInfo info = getPlayerInfo(player);
        if (info.sniffer == null) {
            return;
        }
        RedstoneEther.server().removeReceivingDevice(info.sniffer);
        info.sniffer = null;
    }

    public void processSMPMaps(World world) {
        RedstoneEther.loadServerWorld(world);
        int dimension = CommonUtils.getDimension(world);
        ArrayList<EntityPlayer> players = ServerUtils.getPlayersInDimension(dimension);

        Map<BlockCoord, TXNodeInfo> txnodes = RedstoneEther.server().getTransmittersInDimension(dimension);
        Set<WirelessTransmittingDevice> devices = RedstoneEther.server().getTransmittingDevicesInDimension(dimension);

        for (EntityPlayer player : players) {
            ItemStack helditem = player.getCurrentEquippedItem();

            if (helditem == null || helditem.getItem() != WirelessRedstoneAddons.wirelessMap || RedstoneEther.server().isPlayerJammed(player)) {
                continue;
            }

            ItemWirelessMap map = (ItemWirelessMap) helditem.getItem();
            MapData mapdata = map.getMapData(helditem, world);

            if (mapdata.dimension != player.dimension) {
                continue;
            }

            WirelessMapNodeStorage mapnodes = getMapNodes(player);
            TreeSet<FreqCoord> oldnodes = mapnodes.nodes;
            int lastdevices = mapnodes.devices.size();

            updatePlayerMapData(player, world, mapdata, txnodes, devices);

            TreeSet<FreqCoord> addednodes = new TreeSet<FreqCoord>(mapnodes.nodes);
            TreeSet<FreqCoord> removednodes = new TreeSet<FreqCoord>();

            if (oldnodes.size() != 0) {
                for (Iterator<FreqCoord> nodeiterator = oldnodes.iterator(); nodeiterator.hasNext(); ) {
                    FreqCoord node = nodeiterator.next();
                    if (!addednodes.remove(node))//remove returns false if the item was not in the set
                    {
                        removednodes.add(node);
                    }
                }
            }

            if (addednodes.size() != 0 || removednodes.size() != 0 || devices.size() != 0 || lastdevices > 0) {
                WRAddonSPH.sendMapUpdatePacketTo(player, helditem.getItemDamage(), mapdata, addednodes, removednodes, mapnodes.devices);
            }
        }
    }

    private void updatePlayerMapData(EntityPlayer player, World world, MapData mapdata, Map<BlockCoord, TXNodeInfo> txnodes, Set<WirelessTransmittingDevice> devices) {
        TreeSet<FreqCoord> mnodes = new TreeSet<FreqCoord>();
        TreeSet<FreqCoord> mdevices = new TreeSet<FreqCoord>();

        int blockwidth = 1 << mapdata.scale;
        int minx = mapdata.xCenter - blockwidth * 64;
        int minz = mapdata.zCenter - blockwidth * 64;
        int maxx = mapdata.xCenter + blockwidth * 64;
        int maxz = mapdata.zCenter + blockwidth * 64;

        for (Entry<BlockCoord, TXNodeInfo> entry : txnodes.entrySet()) {
            BlockCoord node = entry.getKey();
            TXNodeInfo info = entry.getValue();
            if (info.on && node.x > minx && node.x < maxx && node.z > minz && node.z < maxz && RedstoneEther.server().canBroadcastOnFrequency(player, info.freq)) {
                mnodes.add(new FreqCoord(node.x - mapdata.xCenter, node.y, node.z - mapdata.zCenter, info.freq));
            }
        }

        for (Iterator<WirelessTransmittingDevice> iterator = devices.iterator(); iterator.hasNext(); ) {
            WirelessTransmittingDevice device = iterator.next();
            Vector3 pos = device.getPosition();
            if (pos.x > minx && pos.x < maxx && pos.z > minz && pos.z < maxz && RedstoneEther.server().canBroadcastOnFrequency(player, device.getFreq())) {
                mdevices.add(new FreqCoord((int) pos.x, (int) pos.y, (int) pos.z, device.getFreq()));
            }
        }

        WirelessMapNodeStorage mapnodes = getMapNodes(player);

        mapnodes.nodes = mnodes;
        mapnodes.devices = mdevices;
    }

    public void onLogin(EntityPlayer player) {
        playerInfos.put(player.getCommandSenderName(), new AddonPlayerInfo());
    }

    public void onLogout(EntityPlayer player) {
        playerInfos.remove(player.getCommandSenderName());
    }

    public void onDimensionChange(EntityPlayer player) {
        deactivateRemote(player.worldObj, player);
        remSniffer(player);

        playerInfos.put(player.getCommandSenderName(), new AddonPlayerInfo());

        for (Iterator<EntityWirelessTracker> iterator = playerTrackers.iterator(); iterator.hasNext(); ) {
            EntityWirelessTracker tracker = iterator.next();
            if (tracker.attachedPlayerName.equals(player.getCommandSenderName())) {
                tracker.copyToDimension(player.dimension);
                iterator.remove();
            }
        }
    }

    public WirelessMapNodeStorage getMapNodes(EntityPlayer player) {
        AddonPlayerInfo info = getPlayerInfo(player);
        return info.mapNodes;
    }

    public void updateSMPMapInfo(World world, EntityPlayer player, MapData mapdata, int mapno) {
        AddonPlayerInfo info = getPlayerInfo(player);
        if (!info.mapInfoSet.contains(Integer.valueOf(mapno))) {
            WRAddonSPH.sendMapInfoTo(player, mapno, mapdata);
            info.mapInfoSet.add(mapno);
        }
    }

    public void clearMapNodes(EntityPlayer player) {
        getPlayerInfo(player).mapNodes.clear();
    }

    public void tickTriangs() {
        for (Entry<String, AddonPlayerInfo> entry : playerInfos.entrySet()) {
            EntityPlayer player = ServerUtils.getPlayer(entry.getKey());

            for (Integer freq : entry.getValue().triangSet) {
                double spinto;
                if (!RedstoneEther.server().isFreqOn(freq)) {
                    spinto = -1;
                } else if (isRemoteOn(player, freq)) {
                    spinto = -2;
                } else {
                    Vector3 strengthvec = getBroadcastVector(player, freq);
                    if (strengthvec == null)//in another dimension
                    {
                        spinto = -2;//spin to a random place
                    } else {
                        spinto = (player.rotationYaw + 180) * MathHelper.torad - Math.atan2(-strengthvec.x, strengthvec.z);//spin to the transmitter vec
                    }
                }
                WRAddonSPH.sendTriangAngleTo(player, freq, (float) spinto);
            }
        }
    }

    public Vector3 getBroadcastVector(EntityPlayer player, int freq) {
        Vector3 vecAmplitude = new Vector3(0, 0, 0);
        Vector3 vecPlayer = new Vector3(player.posX, 0, player.posZ);

        for (Iterator<FreqCoord> iterator = RedstoneEther.server().getActiveTransmittersOnFreq(freq, player.dimension).iterator(); iterator.hasNext(); ) {
            FreqCoord node = iterator.next();

            Vector3 vecTransmitter = new Vector3(node.x + 0.5, 0, node.z + 0.5);
            double distancePow2 = vecTransmitter.subtract(vecPlayer).magSquared();
            vecAmplitude.add(vecTransmitter.multiply(1 / distancePow2));
        }

        for (Iterator<WirelessTransmittingDevice> iterator = RedstoneEther.server().getTransmittingDevicesOnFreq(freq).iterator(); iterator.hasNext(); ) {
            WirelessTransmittingDevice device = iterator.next();

            if (device.getAttachedEntity() == player)
                return null;

            if (device.getDimension() != player.dimension)
                continue;

            Vector3 vecTransmitter = device.getPosition();
            vecTransmitter.y = 0;
            double distancePow2 = vecTransmitter.subtract(vecPlayer).magSquared();
            vecAmplitude.add(vecTransmitter.multiply(1 / distancePow2));
        }

        if (vecAmplitude.isZero())
            return null;

        return vecAmplitude;
    }

    public void addTracker(EntityWirelessTracker tracker) {
        trackerPlayerMap.put(tracker, new HashSet<EntityPlayerMP>());

        if (tracker.attachedPlayerName != null)
            playerTrackers.add(tracker);
    }

    public void removeTracker(EntityWirelessTracker tracker) {
        HashSet<EntityPlayerMP> trackedPlayers = trackerPlayerMap.get(tracker);
        if (trackedPlayers != null) {
            for (EntityPlayerMP player : trackedPlayers) {
                WRAddonSPH.sendRemoveTrackerTo(player, tracker);
            }
        }
        trackerPlayerMap.remove(tracker);

        if (tracker.attachedInOtherDimension()) {
            //removed as player has left dimension...
            //keep this tracker in the playerTrackers list, to be notified of the dimension change
        } else
            playerTrackers.remove(tracker);
    }

    public void updateTracker(EntityWirelessTracker tracker) {
        HashSet<EntityPlayerMP> trackedPlayers = trackerPlayerMap.get(tracker);
        if (trackedPlayers == null)
            trackerPlayerMap.put(tracker, trackedPlayers = new HashSet<EntityPlayerMP>());

        for (EntityPlayerMP player : trackedPlayers) {
            WRAddonSPH.sendTrackerUpdatePacketTo(player, tracker);
        }

        if (tracker.attachedPlayerName != null)
            playerTrackers.add(tracker);
        else
            playerTrackers.remove(tracker);
    }

    int trackerTicks = 0;

    public void processTrackers() {
        trackerTicks++;
        HashSet<EntityPlayer> playerEntities = new HashSet<EntityPlayer>(ServerUtils.getPlayers());

        boolean updateFree = trackerTicks % 5 == 0;
        boolean updateAttached = trackerTicks % 100 == 0;

        for (Iterator<Entry<EntityWirelessTracker, HashSet<EntityPlayerMP>>> iterator = trackerPlayerMap.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<EntityWirelessTracker, HashSet<EntityPlayerMP>> entry = iterator.next();

            HashSet<EntityPlayerMP> trackedPlayers = entry.getValue();
            HashSet<EntityPlayerMP> playersToTrack = new HashSet<EntityPlayerMP>();

            EntityWirelessTracker tracker = entry.getKey();
            ChunkCoordIntPair chunk = new ChunkCoordIntPair(tracker.chunkCoordX, tracker.chunkCoordZ);

            for (EntityPlayer entityPlayer : playerEntities) {
                EntityPlayerMP player = (EntityPlayerMP) entityPlayer;
                if (tracker.isDead) {
                    WRAddonSPH.sendRemoveTrackerTo(player, tracker);
                } else if (tracker.getDimension() == player.dimension && !player.loadedChunks.contains(chunk) && !tracker.attachedToLogout())//perform update, add to list
                {
                    playersToTrack.add(player);
                    if (!trackedPlayers.contains(player) || (tracker.isAttachedToEntity() && updateAttached) || (!tracker.isAttachedToEntity() && updateFree)) {
                        WRAddonSPH.sendTrackerUpdatePacketTo(player, tracker);
                    }
                } else if (trackedPlayers.contains(player))//no longer in listening range
                {
                    WRAddonSPH.sendRemoveTrackerTo(player, tracker);
                }
            }

            if (tracker.isDead) {
                iterator.remove();
                continue;
            }

            trackedPlayers.clear();
            trackedPlayers.addAll(playersToTrack);
        }
    }

    public boolean detonateREP(EntityPlayer player) {
        AddonPlayerInfo info = getPlayerInfo(player);
        if (info.activeREP == null) {
            return false;
        } else if (info.activeREP.isDead) {
            info.activeREP = null;
            return false;
        } else {
            info.activeREP.detonate();
            info.activeREP.setDead();
            return true;
        }
    }

    public void invalidateREP(EntityPlayer player) {
        AddonPlayerInfo info = getPlayerInfo(player);
        if (info != null)
            info.activeREP = null;
    }

    public void updateREPTimeouts() {
        for (Entry<String, AddonPlayerInfo> entry : playerInfos.entrySet()) {
            AddonPlayerInfo info = entry.getValue();
            if (info.REPThrowTimeout > 0) {
                info.REPThrowTimeout--;
            }
        }
    }

    public void throwREP(ItemStack itemstack, World world, EntityPlayer player) {
        AddonPlayerInfo info = getPlayerInfo(player);
        if (info.REPThrowTimeout > 0) {
            return;
        }

        if (!player.capabilities.isCreativeMode) {
            itemstack.stackSize--;
        }
        EntityREP activeREP = new EntityREP(world, player);
        world.spawnEntityInWorld(activeREP);
        WRAddonSPH.sendSpawnREP(activeREP);
        world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (world.rand.nextFloat() * 0.4F + 0.8F));
        info.activeREP = activeREP;
        info.REPThrowTimeout = 40;
    }
}
