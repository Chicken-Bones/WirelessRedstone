package codechicken.wirelessredstone.addons;

import java.util.TreeSet;

import codechicken.wirelessredstone.core.FreqCoord;

public class WirelessMapNodeStorage
{    
    public void clear()
    {
        nodes.clear();
        devices.clear();
    }
    
    public TreeSet<FreqCoord> nodes = new TreeSet<FreqCoord>();
    public TreeSet<FreqCoord> devices = new TreeSet<FreqCoord>();
}


