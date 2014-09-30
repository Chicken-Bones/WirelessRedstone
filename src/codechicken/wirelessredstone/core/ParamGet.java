package codechicken.wirelessredstone.core;

import codechicken.core.commands.CoreCommand.WCommandSender;

public class ParamGet extends FreqParam
{
    @Override
    public void printHelp(WCommandSender listener)
    {
        listener.chatT("wrcbe_core.param.get.usage");
        listener.chatT("wrcbe_core.param.get.usage1");
        listener.chatT("wrcbe_core.param.get.usage2");
        listener.chatT("wrcbe_core.param.get.usage3");
    }

    @Override
    public String getName()
    {
        return "get";
    }

    @Override
    public void handleCommand(String playername, String[] subArray, WCommandSender listener)
    {
        RedstoneEther ether = RedstoneEther.get(false);
        
        if(subArray.length != 2)
        {
            listener.chatT("wrcbe_core.param.invalidno");
            return;
        }
                
        if(subArray[1].equals("public"))
            listener.chatT("wrcbe_core.param.get.public", ether.getLastPublicFrequency());
        else if(subArray[1].equals("shared"))
            if(ether.getLastPublicFrequency() >= ether.getLastSharedFrequency())
                listener.chatT("wrcbe_core.param.get.shared0");
            else
                listener.chatT("wrcbe_core.param.get.shared", ether.getLastPublicFrequency()+1, ether.getLastSharedFrequency());
        else if(subArray[1].equals("private"))
            listener.chatT("wrcbe_core.param.get.private", ether.getNumPrivateFreqs());
        else
            listener.chatT("wrcbe_core.param.invalid");
    }
}
