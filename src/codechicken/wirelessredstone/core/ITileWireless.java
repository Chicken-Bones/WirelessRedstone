package codechicken.wirelessredstone.core;

import codechicken.lib.vec.Vector3;

public interface ITileWireless
{
    /* called when the tile is Jammed
     * Should Deactivate the tile */
    public void jamTile();
    /* called when the tile is UnJammed
     * Transmitters should re-check if being powered */
    public void unjamTile();
    /* Return the current frequency of the tile */
    public int getFreq();
    /* Set the frequency of the tile
     * Should remove the tile from the old frequency and add it to the new one with correct power state */
    public void setFreq(int freq);
    /* Return the name to be displayed in the Wireless GUI */
    public String getGuiName();
    /* Return the relative graphical point on tile
     * Used for rendering certain effects */
    public Vector3 getFocalPoint();
}
