package codechicken.wirelessredstone.addons;

public class ClientMapInfo
{
    public ClientMapInfo(int xCenter, int zCenter, byte scale)
    {
        this.xCenter = xCenter;
        this.zCenter = zCenter;
        this.scale = scale;
    }
    
    int xCenter;
    int zCenter;
    byte scale;
    int dimension;
}
