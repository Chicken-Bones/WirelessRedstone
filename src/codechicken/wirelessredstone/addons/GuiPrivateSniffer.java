package codechicken.wirelessredstone.addons;

import codechicken.lib.colour.Colour;
import codechicken.lib.colour.ColourARGB;
import codechicken.wirelessredstone.core.RedstoneEther;
import codechicken.wirelessredstone.core.WRCoreCPH;

public class GuiPrivateSniffer extends GuiWirelessSniffer
{
    public GuiPrivateSniffer()
    {
        super();
        title = "Private Sniffer";
    }
    
    @Override
    protected void mouseClicked(int mousex, int mousey, int button)
    {
        int freq = getFreqMouseOver(mousex, mousey);
        if(freq == 0)
        {
            super.mouseClicked(mousex, mousey, button);
            return;
        }
        if(RedstoneEther.get(true).canBroadcastOnFrequency(mc.thePlayer, freq) && freq > RedstoneEther.get(true).getLastPublicFrequency() && freq <= RedstoneEther.get(true).getLastSharedFrequency())
        {
            String name = RedstoneEther.get(true).isFreqPrivate(freq) ? "" : mc.thePlayer.getCommandSenderName();
            WRCoreCPH.sendSetFreqOwner(freq, name);
        }
    }
    
    @Override
    public String getFreqTip(int freq)
    {
        if(RedstoneEther.get(true).isPlayerJammed(mc.thePlayer))
            return "Jammed "+freq;
        
        if(RedstoneEther.get(true).isFreqPrivate(freq))
            return (RedstoneEther.get(true).getFreqOwner(freq).equalsIgnoreCase(mc.thePlayer.getCommandSenderName()) ? "Owned " : "Private ")+freq;
        
        if(!RedstoneEther.get(true).canBroadcastOnFrequency(mc.thePlayer, freq))
            return "Jammed "+freq;
        
        if(freq <= RedstoneEther.get(true).getLastPublicFrequency())
            return "Public "+freq;
        
        if(freq <= RedstoneEther.get(true).getLastSharedFrequency())
            return "Shared "+freq;
        
        return ""+freq;
    }
    
    @Override
    public Colour getColour(int freq)
    {
        if(RedstoneEther.get(true).isPlayerJammed(mc.thePlayer) || !RedstoneEther.get(true).canBroadcastOnFrequency(mc.thePlayer, freq))
            return colourJammed;

        if(RedstoneEther.get(true).isFreqPrivate(freq) && RedstoneEther.get(true).getFreqOwner(freq).equalsIgnoreCase(mc.thePlayer.getCommandSenderName()))
            return colourPOff.copy().interpolate(colourPOn, brightness[freq-1] / 64F);
            
        Colour colour = colourOff.copy().interpolate(colourOn, brightness[freq-1] / 64F);
        if(freq <= RedstoneEther.get(true).getLastPublicFrequency())
            colour.interpolate(colourJammed, 0.5);

        return colour;
    }
    
    @Override
    public Colour getBorder(int freq)
    {
        if(RedstoneEther.get(true).isPlayerJammed(mc.thePlayer) || !RedstoneEther.get(true).canBroadcastOnFrequency(mc.thePlayer, freq))
            return borderJammed;
        
        if(RedstoneEther.get(true).isFreqPrivate(freq) && RedstoneEther.get(true).getFreqOwner(freq).equalsIgnoreCase(mc.thePlayer.getCommandSenderName()))
            return borderPOff.copy().interpolate(borderPOn, brightness[freq-1] / 64F);
        
        Colour border;
        if(RedstoneEther.get(true).getFreqColourId(freq) != -1)
            border =  new ColourARGB(RedstoneEther.get(true).getFreqColour(freq));
        else
            border = borderOff.copy().interpolate(borderOn, brightness[freq-1] / 64F);

        if(freq <= RedstoneEther.get(true).getLastPublicFrequency())
            border.interpolate(borderJammed, 0.7);
        
        return border;
    }
}
