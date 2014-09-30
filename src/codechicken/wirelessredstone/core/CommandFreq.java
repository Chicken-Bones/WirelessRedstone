package codechicken.wirelessredstone.core;

import java.util.LinkedList;

import codechicken.core.commands.CoreCommand;

public class CommandFreq extends CoreCommand
{    
    public static LinkedList<FreqParam> paramHandlers = new LinkedList<FreqParam>();
    
    static
    {
        paramHandlers.add(new ParamScan());
        paramHandlers.add(new ParamJam());
        paramHandlers.add(new ParamOpen());
        paramHandlers.add(new ParamGet());
        paramHandlers.add(new ParamSet());
        paramHandlers.add(new ParamPrivate());
    }
    
    @Override
    public String getCommandName()
    {
        return "freq";
    }

    @Override
    public void handleCommand(String command, String playername, String[] args, WCommandSender listener)
    {        
        if(args[0].equals("help"))
        {
            for(FreqParam param : paramHandlers)
            {
                if(args[1].equals(param.getName()))
                {
                    param.printHelp(listener);
                    return;
                }
            }
            listener.chatT("wrcbe_core.param.missing");
            return;
        }
        
        for(FreqParam param : paramHandlers)
        {
            if(args[0].equals(param.getName()))
            {
                param.handleCommand(playername, args, listener);
                return;
            }
        }
        listener.chatT("wrcbe_core.param.missing");
    }
    
    @Override
    public void printHelp(WCommandSender listener)
    {
        listener.chatT("wrcbe_core.command.usage");
        StringBuilder paramNames = new StringBuilder();
        for(FreqParam param : paramHandlers)
        {
            if(paramNames.length() > 0)
                paramNames.append(", ");
            paramNames.append(param.getName());
        }
        listener.chatT("wrcbe_core.command.usage1", paramNames.toString());
        listener.chatT("wrcbe_core.command.usage2");
    }

    @Override
    public boolean OPOnly()
    {
        return true;
    }

    @Override
    public int minimumParameters()
    {
        return 1;
    }
}
