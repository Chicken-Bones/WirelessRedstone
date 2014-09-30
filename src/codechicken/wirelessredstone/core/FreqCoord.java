package codechicken.wirelessredstone.core;

import codechicken.lib.vec.BlockCoord;
import net.minecraft.tileentity.TileEntity;

public class FreqCoord implements Comparable<FreqCoord>
{
    public FreqCoord(int x, int y, int z, int freq)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.freq = freq;
    }

    public FreqCoord(ITileWireless itile)
    {
        TileEntity tile = (TileEntity)itile;
        x = tile.xCoord;
        y = tile.yCoord;
        z = tile.zCoord;
        freq = itile.getFreq();
    }
    
    public FreqCoord(BlockCoord node, int freq)
    {
        x = node.x;
        y = node.y;
        z = node.z;
        this.freq = freq;
    }

    public int compareTo(FreqCoord node2)
    {
        if(freq != node2.freq) return freq < node2.freq ? -1 : 1;
        if(x != node2.x) return x < node2.x ? -1 : 1;
        if(z != node2.z) return z < node2.z ? -1 : 1;
        if(y != node2.y) return y < node2.y ? -1 : 1;
        return 0;
    }
    
    public String toString()
    {
        return "["+x+","+y+","+z+" on "+freq+"]";
    }
    
    public int x;
    public int y;
    public int z;
    public int freq;
}
