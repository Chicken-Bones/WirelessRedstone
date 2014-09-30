package codechicken.wirelessredstone.core;

import codechicken.core.ServerUtils;
import net.minecraft.entity.player.EntityPlayer;
import codechicken.core.commands.CoreCommand.WCommandSender;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;

public class ParamJam extends FreqParam
{
    @Override
    public void printHelp(WCommandSender listener) {
        listener.chatT("wrcbe_core.param.jam.usage");
        listener.chatT("wrcbe_core.param.jam.usage1");
        listener.chatT("wrcbe_core.param.jam.usage" + (rand.nextInt(5) + 2), "jam");
    }

    @Override
    public String getName() {
        return "jam";
    }

    @Override
    public void handleCommand(String playername, String[] args, WCommandSender listener) {
        jamOpenCommand(playername, Arrays.copyOfRange(args, 1, args.length), listener, true);
    }

    public static void jamOpenCommand(String playername, String[] args, WCommandSender listener, boolean jam) {
        RedstoneEtherServer ether = RedstoneEther.server();

        if (args.length == 0) {
            listener.chatT("wrcbe_core.param.invalidno");
            return;
        }

        if ((args.length == 1 && ServerUtils.getPlayer(playername) == null)) {
            listener.chatT("wrcbe_core.param.jam.noplayer");
            return;
        }

        String range = args[args.length - 1];
        String jamPlayer = args.length == 1 ? playername : args[0];

        int startfreq;
        int endfreq;

        if (range.equals("all")) {
            startfreq = 1;
            endfreq = RedstoneEther.numfreqs;
        } else if (range.equals("default")) {
            startfreq = ether.getLastSharedFrequency() + 1;
            endfreq = RedstoneEther.numfreqs;
        } else {
            int[] freqrange = RedstoneEther.parseFrequencyRange(range);
            startfreq = freqrange[0];
            endfreq = freqrange[1];
        }

        if (startfreq < 1 || endfreq > RedstoneEther.numfreqs || endfreq < startfreq) {
            listener.chatT("wrcbe_core.param.invalidfreqrange");
            return;
        }

        ether.setFrequencyRangeCommand(jamPlayer, startfreq, endfreq, jam);

        int publicend = ether.getLastPublicFrequency();
        EntityPlayer player = ServerUtils.getPlayer(jamPlayer);
        String paramName = jam ? "jam" : "open";
        ChatStyle playerStyle = new ChatStyle().setColor(EnumChatFormatting.YELLOW);
        if (startfreq == endfreq) {
            if (startfreq <= publicend) {
                listener.chatT("wrcbe_core.param.jam.errpublic");
                return;
            }
            listener.chatOpsT("wrcbe_core.param."+paramName+".opjammed", playername, jamPlayer, startfreq);
            if (player != null)
                player.addChatComponentMessage(new ChatComponentTranslation("wrcbe_core.param."+paramName+".jammed", startfreq).setChatStyle(playerStyle));
        } else {
            if (startfreq <= publicend && endfreq <= publicend) {
                listener.chatT("wrcbe_core.param.jam.errpublic");
                return;
            }
            if (startfreq <= publicend)
                startfreq = publicend + 1;

            listener.chatOpsT("wrcbe_core.param."+paramName+".opjammed2", playername, jamPlayer, startfreq + "-" + endfreq);
            if (player != null)
                player.addChatComponentMessage(new ChatComponentTranslation("wrcbe_core.param."+paramName+".jammed2", startfreq + "-" + endfreq).setChatStyle(playerStyle));
        }
    }
}
