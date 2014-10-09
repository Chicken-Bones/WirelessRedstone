package codechicken.wirelessredstone.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import codechicken.core.CommonUtils;
import codechicken.core.ServerUtils;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class RedstoneEtherServer extends RedstoneEther
{
    public RedstoneEtherServer()
    {
        super(false);
    }
    
    public void init(World world)
    {
        super.init(world);
        
        SaveManager.resetWorld();
        SaveManager.loadFreqInfo();
        SaveManager.loadDimensionHash();    
        
        publicfrequencyend = SaveManager.generalProp.getProperty("PublicFrequencies", 1000);
        sharedfrequencyend = SaveManager.generalProp.getProperty("SharedFrequencies", 5000);
        numprivatefreqs = SaveManager.generalProp.getProperty("PrivateFrequencies", 50);
    }
    
    @Override
    protected void addEther(World world, int dimension)
    {
        if(ethers.get(dimension) != null)
            return;
        
        super.addEther(world, dimension);
        
        SaveManager.reloadSave(world);
        SaveManager.getInstance(dimension).loadEther();
    }

    public void remEther(World world, int dimension)
    {
        if(ethers.get(dimension) == null)
            return;
        
        super.remEther(world, dimension);
        
        SaveManager.unloadSave(dimension);
    }
    
    public void saveEther(World world)
    {
        int dimension = CommonUtils.getDimension(world);
        if(!ethers.containsKey(dimension))
            return;
        
        for(RedstoneEtherFrequency freq : ethers.get(dimension).freqsToSave)
            freq.saveFreq(dimension);
        
        ethers.get(dimension).freqsToSave.clear();
        SaveManager.getInstance(dimension).removeTrailingSectors();
        SaveManager.saveDimensionHash();
    }
    
    public void verifyChunkTransmitters(World world, int chunkx, int chunkz)
    {
        int dimension = CommonUtils.getDimension(world);
        DimensionalEtherHash ether = ethers.get(dimension);
        int blockxmin = chunkx * 16;
        int blockxmax = blockxmin + 15;
        int blockzmin = chunkz * 16;
        int blockzmax = blockzmin + 15;
        
        ArrayList<BlockCoord> transmittingblocks = new ArrayList<BlockCoord>(ether.transmittingblocks.keySet());
        
        for(BlockCoord node : transmittingblocks)
        {            
            if(node.x >= blockxmin && node.x <= blockxmax && node.z >= blockzmin && node.z <= blockzmax)
            {
                TileEntity tile = RedstoneEther.getTile(world, node);
                int freq = ether.transmittingblocks.get(node).freq;
                if(tile == null || !(tile instanceof ITileWireless) || ((ITileWireless)tile).getFreq() != freq)
                {
                    remTransmitter(world, node.x, node.y, node.z, freq);
                    System.out.println("Removed Badly Synced node at:"+node.x+","+node.y+","+node.z+" on "+freq+" in dim"+dimension);
                }
            }
        }
    }    
    
    public void setTransmitter(World world, int x, int y, int z, int freq, boolean on)
    {
        if(freq == 0)
        {
            return;
        }
        
        BlockCoord node = new BlockCoord(x, y, z);
        int dimension = CommonUtils.getDimension(world);
        
        if(isNodeInAOEofJammer(node, dimension))
        {
            jamNodeSometime(world, node, dimension, freq);
        }
        TXNodeInfo info = ethers.get(dimension).transmittingblocks.get(node);
        if(info == null)
            ethers.get(dimension).transmittingblocks.put(node, new TXNodeInfo(freq, on));
        else
            info.on = on;
        freqarray[freq].setTransmitter(world, node, dimension, on);
    }

    public void remTransmitter(World world, int x, int y, int z, int freq)
    {
        if(freq == 0)
        {
            return;
        }

        int dimension = CommonUtils.getDimension(world);
        BlockCoord node = new BlockCoord(x, y, z);
        
        ethers.get(dimension).jammednodes.remove(node);
        ethers.get(dimension).transmittingblocks.remove(node);
        freqarray[freq].remTransmitter(world, node, dimension);
    }

    public void addReceiver(World world, int x, int y, int z, int freq)
    {
        if(freq == 0)
            return;
        
        BlockCoord node = new BlockCoord(x, y, z);
        int dimension = CommonUtils.getDimension(world);
        
        if(isNodeInAOEofJammer(node, dimension))
        {
            jamNodeSometime(world, node, dimension, freq);
        }
        ethers.get(dimension).recievingblocks.put(node, freq);
        freqarray[freq].addReceiver(world, node, dimension);
    }

    public void remReceiver(World world, int x, int y, int z, int freq)
    {
        if(freq == 0)
            return;

        int dimension = CommonUtils.getDimension(world);
        BlockCoord node = new BlockCoord(x, y, z);

        ethers.get(dimension).jammednodes.remove(node);    
        ethers.get(dimension).recievingblocks.remove(node);
        freqarray[freq].remReceiver(world, node, dimension);
    }
    
    public void addJammer(World world, int x, int y, int z)
    {
        int dimension = CommonUtils.getDimension(world);
        BlockCoord jammer = new BlockCoord(x, y, z);
        
        ethers.get(dimension).jammerset.add(jammer);
        jamNodesInAOEOfJammer(world, jammer, dimension);        
    }

    public void remJammer(World world, int x, int y, int z)
    {
        ethers.get(CommonUtils.getDimension(world)).jammerset.remove(new BlockCoord(x, y, z));
    }

    public boolean isNodeJammed(World world, int x, int y, int z)
    {
        Integer timeout = ethers.get(CommonUtils.getDimension(world)).jammednodes.get(new BlockCoord(x, y, z));
        return timeout != null && timeout > 0;
    }

    public boolean isNodeInAOEofJammer(BlockCoord node, int dimension)
    {
        for(Iterator<BlockCoord> iterator = ethers.get(dimension).jammerset.iterator(); iterator.hasNext();)
        {
            BlockCoord jammer = iterator.next();
            if(pythagorasPow2(jammer, node) < jammerrangePow2)
            {
                return true;
            }
        }
        return false;
    }

    public boolean isPointInAOEofJammer(Vector3 point, int dimension)
    {
        for(Iterator<BlockCoord> iterator = ethers.get(dimension).jammerset.iterator(); iterator.hasNext();)
        {
            BlockCoord jammer = iterator.next();
            if(pythagorasPow2(jammer, point) < jammerrangePow2)
            {
                return true;
            }
        }
        return false;
    }

    public BlockCoord getClosestJammer(BlockCoord node, int dimension)
    {
        BlockCoord closestjammer = null;
        double closestdist = jammerrangePow2;
        for(Iterator<BlockCoord> iterator = ethers.get(dimension).jammerset.iterator(); iterator.hasNext();)
        {
            BlockCoord jammer = iterator.next();
            double distance = pythagorasPow2(jammer, node);
            if(distance < closestdist)
            {
                closestjammer = jammer;
                closestdist = distance;
            }
        }
        return closestjammer;
    }

    public BlockCoord getClosestJammer(Vector3 point, int dimension)
    {
        BlockCoord closestjammer = null;
        double closestdist = jammerrangePow2;
        for(Iterator<BlockCoord> iterator = ethers.get(dimension).jammerset.iterator(); iterator.hasNext();)
        {
            BlockCoord jammer = iterator.next();
            double distance = pythagorasPow2(jammer, point);
            if(distance < closestdist)
            {
                closestjammer = jammer;
                closestdist = distance;
            }
        }
        return closestjammer;
    }

    public void jamNodeSometime(World world, BlockCoord node, int dimension, int freq)
    {
        ethers.get(dimension).jammednodes.put(node, -world.rand.nextInt(jammerblockwait));
    }

    public void jamEntitySometime(EntityLivingBase entity)
    {
        jammedentities.put(entity, -entity.worldObj.rand.nextInt(jammerentitywait));
    }

    public void jamNode(World world, BlockCoord node, int dimension, int freq)
    {        
        ethers.get(dimension).jammednodes.put(node, getRandomTimeout(world.rand));
    
        freqarray[freq].remTransmitter(world, node, dimension);
        freqarray[freq].remReceiver(world, node, dimension);
    }
    
    public void jamNode(World world, int x, int y, int z, int freq)
    {
        if(freq == 0)
            return;
        
        jamNode(world, new BlockCoord(x, y, z), CommonUtils.getDimension(world), freq);
    }
    
    @Override
    public void jamEntity(EntityLivingBase entity, boolean jam)
    {
        if(jam)//iterator.remove will be used to unjam entities. We only need to send the packet.
        {
            jammedentities.put(entity, getRandomTimeout(entity.worldObj.rand));
        }
        if(entity instanceof EntityPlayer)
        {
            WRCoreSPH.sendJamPlayerPacketTo((EntityPlayer) entity, jam);
        }
    }
    
    public void jamNodesInAOEOfJammer(World world, BlockCoord jammer, int dimension)
    {
        for(int freq = 1; freq <= numfreqs; freq++)
        {
            TreeMap<BlockCoord, Boolean> transmittermap = freqarray[freq].getTransmitters(dimension);
            for(Iterator<BlockCoord> iterator = transmittermap.keySet().iterator(); iterator.hasNext();)
            {
                BlockCoord node = iterator.next();
                if(pythagorasPow2(node, jammer) < jammerrangePow2)
                {
                    jamNodeSometime(world, node, dimension, freq);
                }
            }
            
            TreeSet<BlockCoord> receiverset = freqarray[freq].getReceivers(dimension);
            for(Iterator<BlockCoord> iterator = receiverset.iterator(); iterator.hasNext();)
            {
                BlockCoord node = iterator.next();
                if(pythagorasPow2(node, jammer) < jammerrangePow2)
                {
                    jamNodeSometime(world, node, dimension, freq);
                }
            }
        }
    }

    public void unjamTile(World world, int x, int y, int z)
    {
        BlockCoord node = new BlockCoord(x, y, z);
        int dimension = CommonUtils.getDimension(world);
        
        Integer timeout = ethers.get(dimension).jammednodes.remove(node);
        
        if(timeout != null && timeout >= 0)//tile was jammed
        {
            ITileWireless tile = (ITileWireless) getTile(world, node);
            tile.unjamTile();
        }
    }
    
    public void saveJammedFrequencies(String username)
    {
        username = username.toLowerCase();
        String jammedfreqs = getJammedFrequencies(username);
        
        if(jammedfreqs.equals(""+(sharedfrequencyend+1)+"-"+numfreqs))
            SaveManager.generalProp.removeProperty(username+".jammedFreqs");
        else
            SaveManager.generalProp.setProperty(username+".jammedFreqs", jammedfreqs);
    }

    public void loadJammedFrequencies(String jammedString, String username)
    {
        String freqranges[] = jammedString.split(",");
        for(int i = 0; i < freqranges.length; i++)
        {
            String currentrange[] = freqranges[i].split("-");
            int startfreq;
            int endfreq;
            if(currentrange.length == 1)
            {
                try {
                    startfreq = endfreq = Integer.parseInt(currentrange[0]);
                } catch(NumberFormatException numberformatexception)
                {continue;}
            }
            else
            {
                try {
                    startfreq = Integer.parseInt(currentrange[0]);
                    endfreq = Integer.parseInt(currentrange[1]);
                } catch(NumberFormatException numberformatexception1)
                {continue;}
            }
            
            setFrequencyRange(username, startfreq, endfreq, true);
        }
    }
    
    @Override
    protected void loadJammedFrequencies(String username)
    {
        String openstring = SaveManager.generalProp.getProperty(username+".jammedFreqs");
        if(openstring == null)
            jamDefaultRange(username);
        else
            loadJammedFrequencies(openstring, username);
    }
    
    public void setFrequencyRangeCommand(String username, int startfreq, int endfreq, boolean flag)
    {
        setFrequencyRange(username, startfreq, endfreq, flag);
        saveJammedFrequencies(username);
    }
    
    public void jamAllFrequencies(String username)
    {
        setFrequencyRange(username, 1, numfreqs, true);
    }
    
    public void jamDefaultRange(String username)
    {
        setFrequencyRange(username, 1, numfreqs, false);
        setFrequencyRange(username, sharedfrequencyend+1, numfreqs, true);
    }
    
    public void setFreqClean(int freq, int dimension)
    {
        freqarray[freq].setClean(dimension);
    }
    
    public void resetPlayer(EntityPlayer player)
    {
        WRCoreSPH.sendPublicFrequencyTo(player, publicfrequencyend);
        WRCoreSPH.sendSharedFrequencyTo(player, sharedfrequencyend);        
        
        String openstring = SaveManager.generalProp.getProperty(player.getCommandSenderName()+".jammedFreqs");
        if(openstring == null)
            jamDefaultRange(player.getCommandSenderName());
        else
            loadJammedFrequencies(openstring, player.getCommandSenderName());
        
        sendFreqInfoTo(player);
        sendPrivateFreqsTo(player);
    }
    
    public void removePlayer(EntityPlayer player)
    {
        playerJammedMap.remove(player.getCommandSenderName());
    }
    
    private void sendFreqInfoTo(EntityPlayer player)
    {        
        ArrayList<Integer> freqsWithInfo = new ArrayList<Integer>();
        for(int freq = 1; freq <= numfreqs; freq++)
        {
            if(!freqarray[freq].getName().equals("") || freqarray[freq].getColourId() != -1)
                freqsWithInfo.add(freq);
        }
        
        WRCoreSPH.sendFreqInfoTo(player, freqsWithInfo);
    }
    
    private void sendPrivateFreqsTo(EntityPlayer player)
    {
        ArrayList<Integer> freqsWithOwners = new ArrayList<Integer>();
        for(int freq = 1; freq <= numfreqs; freq++)
        {
            if(isFreqPrivate(freq))
                freqsWithOwners.add(freq);
        }
        
        WRCoreSPH.sendFreqOwnerTo(player, freqsWithOwners);
    }

    public TreeMap<Integer, Integer> getLoadedFrequencies()
    {
        TreeMap<Integer, Integer> treemap = new TreeMap<Integer, Integer>();
        for(int freq = 1; freq <= numfreqs; freq++)
        {
            if(freqarray[freq].nodeCount() != 0)
            {
                treemap.put(freq, freqarray[freq].getActiveTransmitters());
            }
        }
    
        return treemap;
    }

    public Map<BlockCoord, Boolean> getTransmittersOnFreq(int freq, int dimension)
    {
        return Collections.unmodifiableMap(freqarray[freq].getTransmitters(dimension));
    }

    public Collection<BlockCoord> getReceiversOnFreq(int freq, int dimension)
    {
        return Collections.unmodifiableCollection(freqarray[freq].getReceivers(dimension));
    }

    public Map<BlockCoord, TXNodeInfo> getTransmittersInDimension(int dimension)
    {
        return Collections.unmodifiableMap(ethers.get(dimension).transmittingblocks);
    }
    
    public Set<WirelessTransmittingDevice> getTransmittingDevicesInDimension(int dimension)
    {
        return Collections.unmodifiableSet(ethers.get(dimension).transmittingdevices);
    }

    public ArrayList<FreqCoord> getActiveTransmittersOnFreq(int freq, int dimension)
    {
        ArrayList<FreqCoord> txnodes = new ArrayList<FreqCoord>();
        freqarray[freq].putActiveTransmittersInList(dimension, txnodes);
        return txnodes;
    }
    
    public TreeSet<BlockCoord> getJammers(int dimension)
    {
        return ethers.get(dimension).jammerset;
    }

    public TreeMap<BlockCoord, Integer> getJammedNodes(int dimension)
    {
        return ethers.get(dimension).jammednodes;
    }
    
    public TreeSet<BlockCoord> getNodesInRangeofPoint(int dimension, Vector3 point, float range, boolean includejammed)
    {
        TreeSet<BlockCoord> nodes = new TreeSet<BlockCoord>();
        float rangePow2 = range*range;
        for(int freq = 1; freq <= numfreqs; freq++)
        {
            TreeMap<BlockCoord, Boolean> transmittermap = freqarray[freq].getTransmitters(dimension);
            for(Iterator<BlockCoord> iterator = transmittermap.keySet().iterator(); iterator.hasNext();)
            {
                BlockCoord node = iterator.next();
                if(pythagorasPow2(node, point) < rangePow2)
                {
                    nodes.add(node);
                }
            }
            
            TreeSet<BlockCoord> receiverset = freqarray[freq].getReceivers(dimension);
            for(Iterator<BlockCoord> iterator = receiverset.iterator(); iterator.hasNext();)
            {
                BlockCoord node = iterator.next();
                if(pythagorasPow2(node, point) < rangePow2)
                {
                    nodes.add(node);
                }
            }
        }
        
        if(includejammed)
        {
            for(Iterator<BlockCoord> iterator = ethers.get(dimension).jammednodes.keySet().iterator(); iterator.hasNext();)
            {
                BlockCoord node = iterator.next();
                if(pythagorasPow2(node, point) < rangePow2)
                {
                    nodes.add(node);
                }
            }
        }
        
        return nodes;
    }
    
    public TreeSet<BlockCoord> getNodesInRangeofNode(int dimension, BlockCoord block, float range, boolean includejammed)
    {
        TreeSet<BlockCoord> nodes = new TreeSet<BlockCoord>();
        float rangePow2 = range*range;
        for(int freq = 1; freq <= numfreqs; freq++)
        {
            TreeMap<BlockCoord, Boolean> transmittermap = freqarray[freq].getTransmitters(dimension);
            for(Iterator<BlockCoord> iterator = transmittermap.keySet().iterator(); iterator.hasNext();)
            {
                BlockCoord node = iterator.next();
                if(pythagorasPow2(node, block) < rangePow2)
                {
                    nodes.add(node);
                }
            }
            
            TreeSet<BlockCoord> receiverset = freqarray[freq].getReceivers(dimension);
            for(Iterator<BlockCoord> iterator = receiverset.iterator(); iterator.hasNext();)
            {
                BlockCoord node = iterator.next();
                if(pythagorasPow2(node, block) < rangePow2)
                {
                    nodes.add(node);
                }
            }
        }

        if(includejammed)
        {
            for(Iterator<BlockCoord> iterator = ethers.get(dimension).jammednodes.keySet().iterator(); iterator.hasNext();)
            {
                BlockCoord node = iterator.next();
                if(pythagorasPow2(node, block) < rangePow2)
                {
                    nodes.add(node);
                }
            }
        }
        
        return nodes;
    }
    
    public void updateReceivingDevices(int freq, boolean on)
    {
        for(Iterator<WirelessReceivingDevice> iterator = receivingdevices.iterator(); iterator.hasNext();)
        {
            iterator.next().updateDevice(freq, on);
        }
    }

    public List<WirelessTransmittingDevice> getTransmittingDevicesOnFreq(int freq)
    {
        return Collections.unmodifiableList(freqarray[freq].getTransmittingDevices());
    }
    
    public void addTransmittingDevice(WirelessTransmittingDevice device)
    {
        ethers.get(device.getDimension()).transmittingdevices.add(device);
        freqarray[device.getFreq()].addTransmittingDevice(device);
    }
    
    public void removeTransmittingDevice(WirelessTransmittingDevice device)
    {
        ethers.get(device.getDimension()).transmittingdevices.remove(device);
        freqarray[device.getFreq()].removeTransmittingDevice(device);
    }

    public void addReceivingDevice(WirelessReceivingDevice device)
    {
        receivingdevices.add(device);
    }
    
    public void removeReceivingDevice(WirelessReceivingDevice device)
    {
        receivingdevices.remove(device);
    }

    public void setDimensionTransmitterCount(int freq, int dimension, int count)
    {
        freqarray[freq].setActiveTransmittersInDim(dimension, count);
    }
    
    public void addFreqToSave(RedstoneEtherFrequency freq, int dimension)
    {
        ethers.get(dimension).freqsToSave.add(freq);
    }
    
    public void tick(World world)
    {
        updateJammedNodes(world);
        randomJamTest(world);
        updateJammedEntities(world);
        entityJamTest(world);
        unloadJammedMap();
    }
    
    private void unloadJammedMap()
    {
        for(Iterator<String> iterator = playerJammedMap.keySet().iterator(); iterator.hasNext();)
        {
            String username = iterator.next();
            if(ServerUtils.getPlayer(username) == null)
            {
                saveJammedFrequencies(username);
                iterator.remove();
            }
        }
    }
    
    private void updateJammedNodes(World world)
    {
        int dimension = CommonUtils.getDimension(world);
        for(Iterator<BlockCoord> iterator = ethers.get(dimension).jammednodes.keySet().iterator(); iterator.hasNext();)
        {
            BlockCoord node = iterator.next();
            int inactivetime = ethers.get(dimension).jammednodes.get(node);
            inactivetime--;
            
            if(inactivetime == 0 || inactivetime < 0 && inactivetime%jammerrandom == 0)
            {
                ITileWireless tile = (ITileWireless) getTile(world, node);
                if(tile == null)
                {
                    iterator.remove();
                    continue;
                }
                
                BlockCoord jammer = getClosestJammer(node, dimension);
                ITileJammer jammertile = jammer == null ? null : (ITileJammer)getTile(world, jammer);
                if(jammertile == null)
                {
                    iterator.remove();
                    tile.unjamTile();
                    continue;
                }
                jammertile.jamTile(tile);
            }
            
            if(inactivetime == 0)//so the node doesn't think it's unjammed
                inactivetime = jammertimeout;
            
            ethers.get(dimension).jammednodes.put(node, inactivetime);
        }
    }

    private void randomJamTest(World world)
    {
        if(world.getTotalWorldTime() % 600 != 0)//30 seconds
            return;
        
        for(Entry<Integer, DimensionalEtherHash> entry : ethers.entrySet())
            if(entry.getValue().jammerset != null)
                for(Iterator<BlockCoord> iterator = entry.getValue().jammerset.iterator(); iterator.hasNext();)
                    jamNodesInAOEOfJammer(world, iterator.next(), entry.getKey());
    }

    private void updateJammedEntities(World world)
    {
        int dimension = CommonUtils.getDimension(world);
        for(Iterator<EntityLivingBase> iterator = jammedentities.keySet().iterator(); iterator.hasNext();)
        {
            EntityLivingBase entity = iterator.next();
            int inactivetime = jammedentities.get(entity);
            inactivetime--;
            
            if(entity == null || entity.isDead)//logged out or killed
            {
                iterator.remove();
                continue;
            }
            
            if(inactivetime == 0//time for unjam or rejam
                    || (inactivetime < 0 && inactivetime%jammerentitywait == 0)//time to jam from the sometime
                    || (inactivetime > 0 && inactivetime%jammerentityretry == 0))//send another bolt after the retry time
            {
                BlockCoord jammer = getClosestJammer(Vector3.fromEntity(entity), dimension);
                ITileJammer jammertile = jammer == null ? null : (ITileJammer)getTile(world, jammer);
                if(jammertile == null)
                {
                    if(inactivetime <= 0)//not a rejam test
                    {
                        iterator.remove();
                        jamEntity(entity, false);
                        continue;
                    }
                }
                else
                {
                    jammertile.jamEntity(entity);
                }
            }
            
            if(inactivetime == 0)//so the node doesn't think it's unjammed
            {
                inactivetime = jammertimeout;
            }
            
            jammedentities.put(entity, inactivetime);
        }
    }

    private void entityJamTest(World world)
    {
        if(world.getTotalWorldTime() % 10 != 0)
            return;
        
        int dimension = CommonUtils.getDimension(world);
        for(Iterator<BlockCoord> iterator = ethers.get(dimension).jammerset.iterator(); iterator.hasNext();)
        {
            BlockCoord jammer = iterator.next();
            List<Entity> entitiesinrange = world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox(jammer.x-9.5, jammer.y-9.5, jammer.z-9.5, jammer.x+10.5, jammer.y+10.5, jammer.z+10.5));
            for(Iterator<Entity> iterator2 = entitiesinrange.iterator(); iterator2.hasNext();)
            {
                Entity entity = iterator2.next();
                if(!(entity instanceof EntityLivingBase))
                    continue;
                
                if(entity instanceof EntityPlayer)
                    if(isPlayerJammed((EntityPlayer)entity))
                        continue;
                
                jamEntitySometime((EntityLivingBase) entity);
            }
        }
    }

    public void unload()
    {
        SaveManager.unloadAll();
    }
    
    @Override
    public void setFreq(ITileWireless tile, int freq)
    {
        tile.setFreq(freq);
    }
}
