package codechicken.wirelessredstone.core;

import java.util.*;
import java.util.Map.Entry;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import codechicken.core.CommonUtils;
import codechicken.lib.vec.BlockCoord;

public class RedstoneEtherFrequency
{
    public static class DelayedModification
    {
        public DelayedModification(BlockCoord node, int i)
        {
            coord = node;
            function = i;
        }
        
        BlockCoord coord;
        int function;
    }
    
    public class DimensionalNodeTracker
    {        
        public DimensionalNodeTracker(World world2)
        {
            world = world2;
            dimension = CommonUtils.getDimension(world2);
        }
        
        public TreeMap<BlockCoord, Boolean> transmittermap = new TreeMap<BlockCoord, Boolean>();
        public TreeSet<BlockCoord> receiverset = new TreeSet<BlockCoord>();
        public LinkedList<DelayedModification> temporarySet = new LinkedList<DelayedModification>();
        

        public void setDirty()
        {
            if(!isdirty)
                ((RedstoneEtherServer)ether).addFreqToSave(RedstoneEtherFrequency.this, dimension);
            isdirty = true;
        }
        
        public World world;
        public int dimension;
        private boolean isdirty = false;
    }
    
    private boolean powered;
    private int freq;
    private RedstoneEther ether;
    
    private HashMap<Integer, DimensionalNodeTracker> nodetrackers = new HashMap<Integer, DimensionalNodeTracker>();
    private HashMap<Integer, Integer> activeDimensions = new HashMap<Integer, Integer>();
    private ArrayList<WirelessTransmittingDevice> transmittingdevices = new ArrayList<WirelessTransmittingDevice>();
    
    private boolean useTemporarySet = false;
    
    private int colour = -1;
    private String name = "";
    
    public RedstoneEtherFrequency(RedstoneEther ether, int freq)
    {        
        this.ether = ether;
        this.freq = freq;
    }
    
    public void addEther(World world, int dimension)
    {
        nodetrackers.put(dimension, new DimensionalNodeTracker(world));
    }

    public void remEther(int dimension)
    {
        nodetrackers.remove(dimension);
    }

    public boolean isOn()
    {
        return powered;
    }
    
    public void saveFreq(int dimension)
    {
        DimensionalNodeTracker nodetracker = nodetrackers.get(dimension);
        if(nodetracker == null || !nodetracker.isdirty)
        {
            return;
        }
        
        SaveManager.getInstance(dimension).saveFreq(freq, getActiveTransmittersInDim(dimension), nodetracker.transmittermap, getDimensionHash());
        nodetracker.isdirty = false;
    }
    
    public void addReceiver(World world, BlockCoord node, int dimension)
    {
        if(useTemporarySet)
        {
            nodetrackers.get(dimension).temporarySet.add(new DelayedModification(node, 1));
            return;
        }
        
        nodetrackers.get(dimension).receiverset.add(node);
        updateReceiver(world, node, isOn());
        
    }
    
    public void remTransmitter(World world, BlockCoord node, int dimension)
    {
        if(useTemporarySet)
        {
            nodetrackers.get(dimension).temporarySet.add(new DelayedModification(node, 2));
            return;
        }
        
        Boolean wason = nodetrackers.get(dimension).transmittermap.get(node);
        nodetrackers.get(dimension).transmittermap.remove(node);

        if(wason != null && wason)
        {
            decrementActiveTransmitters(dimension);
            nodetrackers.get(dimension).setDirty();
        }
    }

    public void remReceiver(World world, BlockCoord node, int dimension)
    {
        if(useTemporarySet)            
        {
            nodetrackers.get(dimension).temporarySet.add(new DelayedModification(node, 0));
            return;
        }
        
        nodetrackers.get(dimension).receiverset.remove(node);
    }
    
    public void loadTransmitter(BlockCoord node, int dimension)
    {
        nodetrackers.get(dimension).transmittermap.put(node, true);
        incrementActiveTransmitters(dimension);
    }

    public void setTransmitter(World world, BlockCoord node, int dimension, boolean on)
    {
        if(useTemporarySet)            
        {
            nodetrackers.get(dimension).temporarySet.add(new DelayedModification(node, 4|(on ? 1 : 0)));
            return;
        }
        
        Boolean wasnodeon = nodetrackers.get(dimension).transmittermap.get(node);
        boolean newtransmitter = wasnodeon == null;// if the get returned null the transmitter needsadding to the list

        nodetrackers.get(dimension).transmittermap.put(node, on);

        if(!newtransmitter && (on == wasnodeon))
        {
            return;
        }

        if(newtransmitter)
        {
            if(on)
            {
                incrementActiveTransmitters(dimension);
                nodetrackers.get(dimension).setDirty();
            }
        }
        else
        {
            if(on)
            {
                incrementActiveTransmitters(dimension);
                nodetrackers.get(dimension).setDirty();
            }
            else
            {
                decrementActiveTransmitters(dimension);
                nodetrackers.get(dimension).setDirty();
            }
        }
    }

    public void incrementActiveTransmitters(int dimension)
    {
        setActiveTransmittersInDim(dimension, getActiveTransmittersInDim(dimension)+1);
    }

    public void decrementActiveTransmitters(int dimension)
    {
        setActiveTransmittersInDim(dimension, getActiveTransmittersInDim(dimension)-1);
    }
    
    public void updateAllReceivers()
    {
        ((RedstoneEtherServer)ether).updateReceivingDevices(freq, powered);
        
        for(Entry<Integer, DimensionalNodeTracker> entry : nodetrackers.entrySet())
        {
            int dimension = entry.getKey();
            DimensionalNodeTracker tracker = entry.getValue();
            
            useTemporarySet = true;
            for(BlockCoord coord : tracker.receiverset)
            {
                updateReceiver(tracker.world, coord, powered);
            }
            useTemporarySet = false;
            
            while(tracker.temporarySet.size() > 0)
            {
                DelayedModification mod = tracker.temporarySet.removeFirst();
                
                if(mod.function == 0)
                    remReceiver(tracker.world, mod.coord, dimension);
                else if(mod.function == 1)
                    addReceiver(tracker.world, mod.coord, dimension);
                else if(mod.function == 2)
                    remTransmitter(tracker.world, mod.coord, dimension);
                else if((mod.function&4)!= 0)
                    setTransmitter(tracker.world, mod.coord, dimension, (mod.function&1) != 0);
            }
        }
    }

    public void updateAllReceivers(DimensionalNodeTracker tracker)
    {
        
    }

    public void updateReceiver(World world, BlockCoord node, boolean on)
    {
        TileEntity tileentity = RedstoneEther.getTile(world, node);
        if(tileentity instanceof ITileReceiver)
        {
            ((ITileReceiver)tileentity).setActive(on);
        }
        else
        {
            System.out.println("Null Receiver");
        }
    }
    
    public void setColour(int colourid)
    {
        colour = colourid;
        if(!ether.remote && !SaveManager.isLoading())
        {
            if(colourid == -1)
                SaveManager.freqProp.removeProperty(freq+".colour");
            else
                SaveManager.freqProp.setProperty(freq+".colour", colour);
        }
    }
    
    public int getColourId()
    {
        return colour;
    }
    
    public void setName(String name)
    {
        this.name = name;
        if(!ether.remote && !SaveManager.isLoading())
        {
            if(name == null || name.equals(""))
                SaveManager.freqProp.removeProperty(freq+".name");
            else
                SaveManager.freqProp.setProperty(freq+".name", name);
        }
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setClean(int dimension)
    {
        nodetrackers.get(dimension).isdirty = false;
    }
    
    public int nodeCount()
    {
        int count = 0;
        for(Entry<Integer, DimensionalNodeTracker> entry : nodetrackers.entrySet())
        {
            count += entry.getValue().transmittermap.size() + entry.getValue().receiverset.size();
        }
        return count;
    }
    
    public TreeSet<BlockCoord> getReceivers(int dimension)
    {
        return nodetrackers.get(dimension).receiverset;
    }
    
    public TreeMap<BlockCoord, Boolean> getTransmitters(int dimension)
    {
        return nodetrackers.get(dimension).transmittermap;
    }
    
    public void addTransmittingDevice(WirelessTransmittingDevice device)
    {
        if(transmittingdevices.add(device))
        {
            incrementActiveTransmitters(device.getDimension());
        }
    }
    
    public void removeTransmittingDevice(WirelessTransmittingDevice device)
    {
        if(transmittingdevices.remove(device))
        {
            decrementActiveTransmitters(device.getDimension());
        }
    }
    
    public List<WirelessTransmittingDevice> getTransmittingDevices()
    {
        return transmittingdevices;
    }
    
    public void putActiveTransmittersInList(int dimension, ArrayList<FreqCoord> txnodes)
    {
        DimensionalNodeTracker nodetracker = nodetrackers.get(dimension);
        for(Iterator<BlockCoord> iterator = nodetracker.transmittermap.keySet().iterator(); iterator.hasNext();)
        {
            BlockCoord node = iterator.next();
            if(nodetracker.transmittermap.get(node))
                txnodes.add(new FreqCoord(node, freq));
        }
    }
    
    /*public void putTransmittingDevicesInList(ArrayList<WirelessTransmittingDevice> txnodes)
    {
        txnodes.addAll(transmittingdevices);
    }*/
    
    public int getActiveTransmitters()
    {
        int num = 0;
        for(Entry<Integer, Integer> entry : activeDimensions.entrySet())
        {
            num+=entry.getValue();
        }
        return num;
    }
    
    public int getActiveTransmittersInDim(int dim)
    {
        Integer val = activeDimensions.get(dim);
        if(val == null)
        {
            val = 0;
            activeDimensions.put(dim, 0);
        }
        return val;
    }
    
    /**
     * Note: Causes updates
     */
    public void setActiveTransmittersInDim(int dim, int num)
    {
        activeDimensions.put(dim, num);
        
        if((num == 0) == powered)//powered and setting 0 or not powered and setting >0
        {
            boolean nowPowered = getActiveTransmitters() > 0;
            if(nowPowered != powered)
            {
                powered = nowPowered;
                updateAllReceivers();
            }
        }
    }
    
    public Map<Integer, Integer> getDimensionHash()
    {
        return Collections.unmodifiableMap(activeDimensions);
    }
}
