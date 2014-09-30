package codechicken.wirelessredstone.core;

import codechicken.core.commands.CoreCommand.WCommandSender;

public class ParamPrivate extends FreqParam
{
    @Override
    public void printHelp(WCommandSender listener) {
        listener.chatT("wrcbe_core.param.private.usage");
        listener.chatT("wrcbe_core.param.private.usage1");
        listener.chatT("wrcbe_core.param.private.usage2");
        listener.chatT("wrcbe_core.param.private.usage3");
        listener.chatT("wrcbe_core.param.private.usage4");
    }

    @Override
    public String getName() {
        return "private";
    }

    @Override
    public void handleCommand(String playername, String[] subArray, WCommandSender listener) {
        RedstoneEther ether = RedstoneEther.get(false);

        if (subArray.length == 1) {
            listener.chatT("wrcbe_core.param.invalidno");
            return;
        }

        if (subArray[1].equals("all")) {
            StringBuilder returnString = new StringBuilder();
            for (int freq = 1; freq <= RedstoneEther.numfreqs; freq++) {
                if (ether.isFreqPrivate(freq)) {
                    if (returnString.length() > 0)
                        returnString.append(", ");

                    returnString.append(freq);
                }
            }

            if (returnString.length() == 0)
                listener.chatT("wrcbe_core.param.private.none");
            else
                listener.chatT("wrcbe_core.param.private.", returnString);
            return;
        }

        if (subArray[1].equals("clear")) {
            if (subArray.length == 2) {
                listener.chatT("wrcbe_core.param.invalidno");
                return;
            }

            int freq = -1;
            try {
                freq = Integer.parseInt(subArray[2]);
            } catch (NumberFormatException ne) {}

            if (freq != -1) {
                if (freq < 1 || freq > RedstoneEther.numfreqs) {
                    listener.chatT("wrcbe_core.param.invalidfreq");
                    return;
                }

                if (freq <= ether.getLastPublicFrequency()) {
                    listener.chatT("wrcbe_core.param.private.publicanyway", freq);
                    return;
                }

                if (!ether.isFreqPrivate(freq)) {
                    listener.chatT("wrcbe_core.param.private.notprivate", freq);
                    return;
                }

                ether.removeFreqOwner(freq);
                listener.chatT("wrcbe_core.param.private.nowhared", freq);
                return;
            }

            String scanPlayer = subArray[2];

            StringBuilder returnString = new StringBuilder();
            for (freq = 1; freq <= RedstoneEther.numfreqs; freq++) {
                if (ether.isFreqPrivate(freq)) {
                    if (scanPlayer.equals("all") || ether.getFreqOwner(freq).equalsIgnoreCase(scanPlayer)) {
                        if (returnString.length() > 0)
                            returnString.append(", ");

                        returnString.append(freq);
                        ether.removeFreqOwner(freq);
                    }
                }
            }

            if (returnString.length() == 0)
                if (scanPlayer.equals("all"))
                    listener.chatT("wrcbe_core.param.private.none");
                else
                    listener.chatT("wrcbe_core.param.private.noneowned", scanPlayer);
            else
                listener.chatT("wrcbe_core.param.private.nowshared2", returnString);
            return;
        }

        int freq = -1;
        try {
            freq = Integer.parseInt(subArray[1]);
        } catch (NumberFormatException ne) {}

        if (freq != -1) {
            if (freq < 1 || freq > RedstoneEther.numfreqs) {
                listener.chatT("wrcbe_core.param.invalidfreq");
                return;
            }

            if (freq <= ether.getLastPublicFrequency()) {
                listener.chatT("wrcbe_core.param.private.ispublic", freq);
                return;
            }

            if (!ether.isFreqPrivate(freq)) {
                listener.chatT("wrcbe_core.param.private.notprivate", freq);
                return;
            }

            listener.chatT("wrcbe_core.param.private.ownedby", freq, ether.getFreqOwner(freq));
            return;
        }

        String scanPlayer = subArray[1];
        if (subArray.length == 2) {
            StringBuilder returnString = new StringBuilder();
            for (freq = 1; freq <= RedstoneEther.numfreqs; freq++) {
                if (ether.isFreqPrivate(freq) && ether.getFreqOwner(freq).equalsIgnoreCase(scanPlayer)) {
                    if (returnString.length() > 0)
                        returnString.append(", ");

                    returnString.append(freq);
                }
            }

            if (returnString.length() == 0)
                listener.chatT("wrcbe_core.param.private.noneowned", scanPlayer);
            else
                listener.chatT("wrcbe_core.param.private.owns", scanPlayer, returnString);
            return;
        }

        try {
            freq = Integer.parseInt(subArray[2]);
        } catch (NumberFormatException ne) {
            listener.chatT("wrcbe_core.param.invalidfreq");
            return;
        }

        if (freq < 1 || freq > RedstoneEther.numfreqs) {
            listener.chatT("wrcbe_core.param.invalidfreq");
            return;
        }

        if (freq <= ether.getLastPublicFrequency()) {
            listener.chatT("wrcbe_core.param.private.ispublic", freq);
            return;
        }

        if (freq > ether.getLastSharedFrequency()) {
            listener.chatT("wrcbe_core.param.private.notshared", freq);
            return;
        }

        ether.setFreqOwner(freq, scanPlayer);
        if (ether.isFreqPrivate(freq) && ether.getFreqOwner(freq).equalsIgnoreCase(scanPlayer))
            listener.chatT("wrcbe_core.param.private.nowownedby", scanPlayer);
        else
            listener.chatT("wrcbe_core.param.private.limit", scanPlayer, ether.getNumPrivateFreqs());
    }

}
