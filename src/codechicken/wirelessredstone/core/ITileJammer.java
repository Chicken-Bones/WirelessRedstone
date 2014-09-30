package codechicken.wirelessredstone.core;

import codechicken.lib.vec.Vector3;
import net.minecraft.entity.Entity;

public interface ITileJammer
{
    public void jamTile(ITileWireless tile);
    public void jamEntity(Entity entity);
    public Vector3 getFocalPoint();
}
