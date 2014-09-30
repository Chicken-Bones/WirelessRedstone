package codechicken.wirelessredstone.core;

import codechicken.core.commands.CoreCommand.WCommandSender;

import java.util.Arrays;

public class ParamOpen extends FreqParam
{
    @Override
    public void printHelp(WCommandSender listener) {
        listener.chatT("wrcbe_core.param.open.usage");
        listener.chatT("wrcbe_core.param.open.usage1");
        listener.chatT("wrcbe_core.param.jam.usage" + (rand.nextInt(5) + 2), "open");
    }

    @Override
    public String getName() {
        return "open";
    }

    @Override
    public void handleCommand(String playername, String[] args, WCommandSender listener) {
        ParamJam.jamOpenCommand(playername, Arrays.copyOfRange(args, 1, args.length), listener, false);
    }
}
