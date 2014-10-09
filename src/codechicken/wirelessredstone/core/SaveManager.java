package codechicken.wirelessredstone.core;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import codechicken.core.CommonUtils;
import codechicken.lib.config.ConfigFile;
import codechicken.lib.config.SimpleProperties;
import codechicken.lib.vec.BlockCoord;
import net.minecraft.world.World;

public class SaveManager
{    
    private RandomAccessFile freqMapFile;
    private RandomAccessFile smallSectorFile;
    private RandomAccessFile largeSectorFile;
    private RandomAccessFile rwfile;
    
    protected static SimpleProperties freqProp;
    protected static SimpleProperties generalProp;
    private static ConfigFile globalconfig;
    
    private static File activeMapFile;

    private ArrayList<Boolean> usedSmallSectors;
    private ArrayList<Boolean> usedLargeSectors;
    private int[] freqMapOffsets;
    
    private long lastcleanuptime;
    
    private int dimension;

    private static ArrayList<Entry<Integer, Integer>>[] freqDimensionHashes = new ArrayList[RedstoneEther.numfreqs+1];
    private static boolean hashChanged = false;
    
    private static boolean loadinginfo;
    
    private static HashMap<Integer, SaveManager> managers = new HashMap<Integer, SaveManager>();
    
    private final int largesectorsize = 256;
    private final int largesectornodes = largesectorsize / 12;
    private final int smallsectorsize = 64;
    private final int smallsectornodes = smallsectorsize / 12;
    
    private final int largesectoroffset = 32767;
    private final int maxsmallnodes = 5;
    
    private final int cleanuptimeneeded = 300*1000;//5 min
    
    static
    {
        globalconfig = new ConfigFile(new File(CommonUtils.getMinecraftDir()+"/config", "WirelessRedstone.cfg")).setComment("Wireless Redstone Chicken Bones Edition Configuration File:Deleting any element will restore it to it's default value:Block ID's will be automatically generated the first time it's run");
    }
    
    public static SaveManager getInstance(int dimension)
    {
        return managers.get(dimension);
    }

    public static void reloadSave(World world)
    {
        managers.put(CommonUtils.getDimension(world), new SaveManager(world));
    }
    
    public static void unloadSave(int dimension)
    {
        SaveManager m = managers.remove(dimension);
        if(m != null)
            m.unload();
    }
    
    public static void resetWorld()
    {
        try
        {
            if(managers.size() == 0)//dim 0 global save stuff
            {
                File etherdir = getEtherDir(CommonUtils.getSaveLocation(0));
                File file = new File(etherdir, "fprop.dat");
                if(!file.exists())
                    file.createNewFile();
                freqProp = new SimpleProperties(file, true);
                
                file = new File(etherdir, "gprop.dat");
                if(!file.exists())
                    file.createNewFile();
                generalProp = new SimpleProperties(file, true);
                generalProp.load();
                
                file = new File(etherdir, "dimMap.dat");
                if(!file.exists())
                    file.createNewFile();
                activeMapFile = file;
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private SaveManager(World world)
    {        
        try
        {
            dimension = CommonUtils.getDimension(world);
            
            File etherdir = getEtherDir(CommonUtils.getSaveLocation(world));
            File file = new File(etherdir, "fmap.dat");
            boolean newlycreated = false;
            if(!file.exists())
            {
                file.createNewFile();
                newlycreated = true;
            }
            freqMapFile = new RandomAccessFile(file, "rw");
            if(newlycreated)
                for(int i = 0; i < 5000; i++)
                    freqMapFile.writeShort(-1);
            
            file = new File(etherdir, "node1.dat");
            if(!file.exists())
                file.createNewFile();
            smallSectorFile = new RandomAccessFile(file, "rw");
            
            file = new File(etherdir, "node2.dat");
            if(!file.exists())
                file.createNewFile();
            largeSectorFile = new RandomAccessFile(file, "rw");
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static File getEtherDir(File worldsave)
    {        
        File dir = new File(worldsave, "RedstoneEther");
        if(!dir.exists())
            dir.mkdirs();
        return dir;
    }
    
    private void setFreqSector(int freq, int sector)
    {
        try
        {
            freqMapOffsets[(freq-1)] = sector;
            freqMapFile.seek((freq-1)*2);
            freqMapFile.writeShort(sector);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private void seekSector(int sector)
    {        
        try
        {
            if(sector >= largesectoroffset)
            {
                rwfile = largeSectorFile;
                sector -= largesectoroffset;
                rwfile.seek(sector * largesectorsize);
            }
            else
            {
                rwfile = smallSectorFile;
                rwfile.seek(sector * smallsectorsize);
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private void setSectorUsed(int sector, boolean used)
    {
        ArrayList<Boolean> usedsectors = (sector >= largesectoroffset) ? usedLargeSectors : usedSmallSectors;
        if(sector >= largesectoroffset)
        {
            sector -= largesectoroffset;
        }
        usedsectors.set(sector, used);
    }
    
    private int getNewSector(boolean large)
    {
        try
        {
            if(large)
            {
                RandomAccessFile file = largeSectorFile;
                
                long prevpos = file.getFilePointer();
                
                file.seek(file.length());
                file.writeShort(-1);//the next sector is not set
                file.write(new byte[largesectorsize - 2]);//fill the rest of this sector with 0's
                file.seek(prevpos);
                
                usedLargeSectors.add(false);
                
                return ((int) file.length() / largesectorsize) - 1 + largesectoroffset;
            }
            
            RandomAccessFile file = smallSectorFile;
            
            long prevpos = file.getFilePointer();
            
            file.seek(file.length());
            file.writeShort(-1);//the next sector is not set
            file.write(new byte[smallsectorsize - 2]);//fill the rest of this sector with 0's
            file.seek(prevpos);
            
            usedSmallSectors.add(false);
            
            return ((int) file.length() / smallsectorsize) - 1;
            
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private int getUnusedSector(int numnodes)
    {
        boolean largesector = (numnodes > maxsmallnodes);
        ArrayList<Boolean> usedsectors = largesector ? usedLargeSectors : usedSmallSectors;
        
        for(int i = 0; i < usedsectors.size(); i++)
            if(!usedsectors.get(i))
                return i + (largesector ? largesectoroffset : 0);
        
        return getNewSector(numnodes > maxsmallnodes);
    }
    
    private void setNextSector(int sector, int nextsector)
    {
        seekSector(sector);
        try
        {
            rwfile.writeShort((short)nextsector);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private void setSectorLength(int sector, int numnodes)
    {
        seekSector(sector);
        try
        {
            rwfile.skipBytes(2);
            rwfile.writeShort((short)numnodes);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private int getNextSector(int sector)
    {
        seekSector(sector);
        try
        {
            int nextsector = rwfile.readShort() & 0xFFFF;
            if(nextsector == 0xFFFF)//empty handling
            {
                nextsector = -1;
            }
            return nextsector;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private int getSectorLength(int sector)
    {
        seekSector(sector);
        try
        {
            rwfile.skipBytes(2);
            return rwfile.readShort() & 0xFFFF;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }        
    }
    
    private void markSectorStackUnused(int firstsector)
    {
        int nextsector = firstsector;
        while(true)//sector unused setting loop
        {
            if(nextsector == -1)
            {
                break;
            }
            int sector = nextsector;
            setSectorUsed(sector, false);
            setNextSector(sector, -1);
            nextsector = getNextSector(nextsector);
        }
    }
    
    private void writeNodes(int freq, int numnodes, ArrayList<BlockCoord> nodes)
    {
        try
        {        
            int nextsector = freqMapOffsets[freq-1];
            if(nextsector == -1)
            {
                nextsector = getUnusedSector(numnodes);
                setSectorUsed(nextsector, true);
                setFreqSector(freq, nextsector);
            }
            else
            {
                markSectorStackUnused(nextsector);
                setSectorUsed(nextsector, true);
            }
            int thissector;
            
            int writtennodes;
            boolean largesector;
            int nodespersector;
            
            while(true)//sector following / creation loop
            {
                seekSector(nextsector);
                rwfile.skipBytes(4);//skip the header info
                
                thissector = nextsector;
                writtennodes = 0;
                largesector = nextsector >= largesectoroffset;
                nodespersector = largesector ? largesectornodes : smallsectornodes;
                
                while(writtennodes != nodespersector && numnodes > 0)//sector node writing loop
                {
                    BlockCoord node = nodes.get(nodes.size() - numnodes);
                    
                    rwfile.writeInt(node.x);
                    rwfile.writeInt(node.y);
                    rwfile.writeInt(node.z);
                
                    numnodes--;
                    writtennodes++;
                }
                
                setSectorLength(thissector, writtennodes);
                
                if(numnodes == 0)//finished
                {
                    setNextSector(thissector, -1);
                    break;
                }

                nextsector = getUnusedSector(numnodes);
                setSectorUsed(nextsector, true);
                setNextSector(thissector, nextsector);
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public void saveFreq(int freq, int activetransmitters, TreeMap<BlockCoord, Boolean> transmittermap, Map<Integer, Integer> dimensionHash)
    {        
        try
        {            
            freqDimensionHashes[freq] = new ArrayList<Entry<Integer, Integer>>(dimensionHash.entrySet());
            hashChanged = true;
            
            int numnodes = 0;
            ArrayList<BlockCoord> nodes = new ArrayList<BlockCoord>(activetransmitters);
            
            for(Iterator<BlockCoord> iterator = transmittermap.keySet().iterator(); iterator.hasNext() && numnodes < activetransmitters;)
            {
                BlockCoord node = iterator.next();
                if(transmittermap.get(node))
                {
                    nodes.add(node);
                    numnodes++;
                }
            }
            
            if(numnodes == 0)
            {
                setFreqSector(freq, -1);
                return;
            }
            
            writeNodes(freq, numnodes, nodes);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private void loadFreqMap()
    {
        try
        {
            freqMapOffsets = new int[RedstoneEther.numfreqs];
            freqMapFile.seek(0);
            
            for(int freq = 1; freq <= RedstoneEther.numfreqs; freq++)//read freq offsets
            {        
                freqMapOffsets[freq-1] = freqMapFile.readShort() & 0xFFFF;
                if(freqMapOffsets[freq-1] == 0xFFFF)//empty handling
                {
                    freqMapOffsets[freq-1] = -1;
                }
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private void initSectors()
    {
        int numsmallsectors;
        int numlargesectors;
        try
        {
            numsmallsectors = (int) (smallSectorFile.length() / smallsectorsize);
            numlargesectors = (int) (largeSectorFile.length() / largesectorsize);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
        usedSmallSectors = new ArrayList<Boolean>(numsmallsectors);
        for(int i = 0; i < numsmallsectors; i++)//intialize array
        {
            usedSmallSectors.add(false);
        }
        
        usedLargeSectors = new ArrayList<Boolean>(numlargesectors);
        for(int i = 0; i < numlargesectors; i++)//intialize array
        {
            usedLargeSectors.add(false);
        }
    }
    
    private void loadNodes()
    {        
        for(int freq = 1; freq <= RedstoneEther.numfreqs; freq++)
        {
            RedstoneEther.server().setDimensionTransmitterCount(freq, dimension, 0);
            
            int nextsector = freqMapOffsets[freq-1];
            
            if(nextsector == -1)
            {
                continue;
            }
            
            while(true)
            {
                seekSector(nextsector);
                setSectorUsed(nextsector, true);
                
                int numnodes = getSectorLength(nextsector);
                        
                for(int j = 0; j < numnodes; j++)
                {
                    try
                    {
                        RedstoneEther.server().loadTransmitter(dimension, rwfile.readInt(), rwfile.readInt(), rwfile.readInt(), freq);
                    }
                    catch(Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }

                nextsector = getNextSector(nextsector);
                if(nextsector == -1)//end of list
                {
                    break;
                }
            }
            
            RedstoneEther.server().setFreqClean(freq, dimension);
        }
    }
    
    public void removeTrailingSectors()
    {
        try
        {
            if(lastcleanuptime != 0 && System.currentTimeMillis() - lastcleanuptime < cleanuptimeneeded)//0 means loading
            {
                return;
            }
            int lastusedsector = -1;
            
            for(int i = 0; i < usedLargeSectors.size(); i++)//find the last used sector
            {
                if(usedLargeSectors.get(i))
                {
                    lastusedsector = i;
                }
            }
            for(int i = usedLargeSectors.size() - 1; i > lastusedsector; i--)//remove sectors from list
            {
                usedLargeSectors.remove(i);
            }
            largeSectorFile.setLength((lastusedsector+1)*largesectorsize);//truncate the file
            
            for(int i = 0; i < usedSmallSectors.size(); i++)
            {
                if(usedSmallSectors.get(i))
                {
                    lastusedsector = i;
                }
            }
            
            for(int i = usedSmallSectors.size() - 1; i > lastusedsector; i--)
            {
                usedSmallSectors.remove(i);
            }
            smallSectorFile.setLength((lastusedsector+1)*smallsectorsize);
            
            lastcleanuptime = System.currentTimeMillis();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public void loadEther()
    {
        loadFreqMap();
        initSectors();
        loadNodes();
        lastcleanuptime = 0;
        removeTrailingSectors();
    }
    
    public static void loadFreqInfo()
    {
        freqProp.load();
        loadinginfo = true;
        for(Iterator<Entry<String, String>> iterator = freqProp.propertyMap.entrySet().iterator(); iterator.hasNext();)
        {
            Entry<String, String> entry = iterator.next();
            boolean success = false;
            try
            {
                int freq;
                {
                    Integer val = Integer.parseInt(entry.getKey().substring(0, entry.getKey().indexOf('.')));
                    if(val == null)
                        continue;
                    freq = val;
                }
                
                if(entry.getKey().endsWith(".colour") || entry.getKey().endsWith(".c"))
                {
                    int colourindex;
                    {
                        Integer val = Integer.parseInt(entry.getValue());
                        if(val == null)
                            continue;
                        colourindex = val;
                    }
                    
                    RedstoneEther.server().setFreqColour(freq, colourindex);
                    success = true;
                }
                else if(entry.getKey().endsWith(".name") || entry.getKey().endsWith(".n"))
                {
                    RedstoneEther.server().setFreqName(freq, entry.getValue());
                    success = true;
                }
                else if(entry.getKey().endsWith(".owner"))
                {
                    RedstoneEther.server().setFreqOwner(freq, entry.getValue());
                    success = true;
                }
            }
            catch(Exception e)
            {
            }
            finally
            {
                if(!success)
                    iterator.remove();
            }
        }
        loadinginfo = false;
        freqProp.save();
    }
    
    public static void loadDimensionHash()
    {
        if(activeMapFile.length() == 0)
            return;
        
        DataInputStream din;
        try
        {
            din = new DataInputStream(new FileInputStream(activeMapFile));

            try
            {
                while(true)
                    RedstoneEther.server().setDimensionTransmitterCount(din.readShort(), din.readInt(), din.readInt());
            }
            catch(EOFException eof)
            {}
            finally
            {
                din.close();
            }
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public static void saveDimensionHash()
    {
        if(!hashChanged)
            return;
        
        try
        {
            DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(activeMapFile)));
            
            for(int freq = 1; freq <= RedstoneEther.numfreqs; freq++)
            {
                ArrayList<Entry<Integer, Integer>> map = freqDimensionHashes[freq];
                if(map == null)
                    continue;
                
                for(Entry<Integer, Integer> entry : map)
                {
                    if(entry.getValue() > 0)
                    {
                        dout.writeShort(freq);
                        dout.writeInt(entry.getKey());
                        dout.writeInt(entry.getValue());
                    }
                }
            }
            
            dout.close();
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public static ConfigFile config()
    {
        return globalconfig;
    }

    public static boolean isLoading()
    {
        return loadinginfo;
    }

    public static void unloadAll()
    {
        for(SaveManager manager : managers.values())
            manager.unload();
        managers.clear();
    }

    private void unload()
    {
        try
        {
            freqMapFile.close();
            smallSectorFile.close();
            largeSectorFile.close();
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
