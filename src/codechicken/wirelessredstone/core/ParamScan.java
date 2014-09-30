package codechicken.wirelessredstone.core;

import codechicken.core.ServerUtils;
import codechicken.core.commands.CoreCommand.WCommandSender;

public class ParamScan extends FreqParam
{
    @Override
    public void printHelp(WCommandSender listener) {
        listener.chatT("wrcbe_core.param.scan.usage");
        listener.chatT("wrcbe_core.param.scan.usage1");
        listener.chatT("wrcbe_core.param.scan.usage" + (rand.nextInt(2) + 2));
    }

    @Override
    public String getName() {
        return "scan";
    }

    @Override
    public void handleCommand(String playername, String[] subArray, WCommandSender listener) {
        RedstoneEther ether = RedstoneEther.get(false);

        if (subArray.length == 1 && ServerUtils.getPlayer(playername) == null) {
            listener.chatT("wrcbe_core.param.invalidno");
            return;
        }

        String scanPlayer = subArray.length == 1 ? playername : subArray[1];

        StringBuilder freqs = new StringBuilder();
        int ranges = 0;
        int startfreq;
        int endfreq = ether.getLastPublicFrequency();
        while (true) {
            int[] freqrange = ether.getNextFrequencyRange(scanPlayer, endfreq + 1, false);
            startfreq = freqrange[0];
            endfreq = freqrange[1];
            if (startfreq == -1) {
                break;
            }

            if (ranges != 0)
                freqs.append(", ");

            if (startfreq == endfreq)
                freqs.append(startfreq);
            else
                freqs.append(startfreq).append("-").append(endfreq);

            ranges++;

            if (endfreq == RedstoneEther.numfreqs) {
                break;
            }
        }

        if (ranges == 0)
            listener.chatT("wrcbe_core.param.scan.onlypublic", scanPlayer);
        else
            listener.chatT("wrcbe_core.param.scan.list", scanPlayer, freqs);
    }

}
