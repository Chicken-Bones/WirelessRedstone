package codechicken.wirelessredstone.core;

import codechicken.core.ServerUtils;
import codechicken.core.commands.CoreCommand.WCommandSender;

public class ParamSet extends FreqParam
{
    @Override
    public void printHelp(WCommandSender listener)
    {
        listener.chatT("wrcbe_core.param.set.usage");
        listener.chatT("wrcbe_core.param.set.usage1");
        listener.chatT("wrcbe_core.param.set.usage2");
        listener.chatT("wrcbe_core.param.set.usage3");
    }

    @Override
    public String getName()
    {
        return "set";
    }

    @Override
    public void handleCommand(String playername, String[] subArray, WCommandSender listener)
    {
        RedstoneEther ether = RedstoneEther.get(false);
        
        if(subArray.length != 3)
        {
            listener.chatT("wrcbe_core.param.invalidno");
            return;
        }
        
        int freq;
        try
        {
            freq = Integer.parseInt(subArray[2]);
        }
        catch(NumberFormatException ne)
        {
            listener.chatT("");
            return;
        }
                
        if(subArray[1].equals("public"))
        {        
            if(freq < 1 || freq > RedstoneEther.numfreqs)
            {
                listener.chatT("wrcbe_core.param.invalidfreq");
                return;
            }
            
            ether.setLastPublicFrequency(freq);
            listener.chatOpsT("wrcbe_core.param.set.nowpublic", playername, ether.getLastPublicFrequency());
            
            if(freq >= ether.getLastSharedFrequency())
                listener.chatOpsT("wrcbe_core.param.set.sharedpublic", playername);
            else
                listener.chatOpsT("wrcbe_core.param.set.nowshared", playername, (freq+1), ether.getLastSharedFrequency());
        }
        else if(subArray[1].equals("shared"))
        {        
            if(freq < 1 || freq > RedstoneEther.numfreqs)
            {
                listener.chatT("wrcbe_core.param.invalidfreq");
                return;
            }
            
            boolean wasPublic = ether.getLastPublicFrequency() >= ether.getLastSharedFrequency();
            
            ether.setLastSharedFrequency(freq);
            
            if(ether.getLastSharedFrequency() >= freq)
                if(!wasPublic)
                    listener.chatOpsT("wrcbe_core.param.set.sharedremoved", playername);
                else
                    listener.chatOpsT("wrcbe_core.param.set.nowshared", playername, (ether.getLastPublicFrequency()+1), freq);
        }
        else if(subArray[1].equals("private"))
        {        
            if(freq < 0 || freq > RedstoneEther.numfreqs)
            {
                listener.chatT("Invalid Quantity.");
                return;
            }
            
            ether.setNumPrivateFreqs(freq);
            listener.chatOpsT("wrcbe_core.param.set.privateno", playername, freq);
        }
        else
        {
            listener.chatT("wrcbe_core.param.set.invalidqty");
        }
    }
}
