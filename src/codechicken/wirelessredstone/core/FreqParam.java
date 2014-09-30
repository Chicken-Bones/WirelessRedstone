package codechicken.wirelessredstone.core;

import java.util.Random;

import codechicken.core.commands.CoreCommand.WCommandSender;

public abstract class FreqParam
{
    public static Random rand = new Random();
    
    public abstract void printHelp(WCommandSender listener);
    public abstract String getName();
    public abstract void handleCommand(String playername, String[] subArray, WCommandSender listener);
}
