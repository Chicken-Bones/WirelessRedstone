package codechicken.wirelessredstone.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import codechicken.core.gui.GuiScrollSlot;
import net.minecraft.entity.player.EntityPlayer;

public class GuiNameSlot extends GuiScrollSlot
{
    public class NameSlotComparator implements Comparator<String>
    {
        public int compare(String s1, String s2) {
            boolean match1 = doesNameMatch(s1, match);
            boolean match2 = doesNameMatch(s2, match);
            if (match1 == match2)
                return s1.compareToIgnoreCase(s2);

            return match1 ? -1 : 1;
        }
    }

    public GuiNameSlot(int x, int y) {
        super(x, y, 150, 71);
        setActionCommand("selectName");
    }

    @Override
    public int getSlotHeight(int slot) {
        return 10;
    }

    @Override
    protected int getNumSlots() {
        return names.size();
    }

    @Override
    public void drawOverlay(float frame) {
        super.drawOverlay(frame);
        drawRect(x, y - 7, x + width, y - 1, 0xFFC6C6C6);//top box blend
        drawRect(x, y + height + 1, x + width, y + height + 6, 0xFFC6C6C6);//bottom box blend
    }

    @Override
    protected void drawSlot(int slot, int x, int y, int mx, int my, float frame) {
        String name = names.get(slot);
        int colour;
        if (selectedslot == slot)
            colour = 0xF0F0F0;
        else if (!doesNameMatch(name, match))
            colour = 0x707070;
        else
            colour = 0xA0A0A0;

        fontRenderer.drawString(name, x, y, colour);
    }

    @Override
    protected void slotClicked(int slot, int button, int mx, int my, int count) {
        if (count == 2)
            sendAction(actionCommand);
        else
            selectedslot = slot;
    }

    @Override
    public void selectNext() {
        if (selectedslot > 0 && selectedslot < getNumSlots()) {
            selectedslot++;
            showSlot(selectedslot);
        }
    }

    @Override
    public void selectPrev() {
        if (selectedslot > 0) {
            selectedslot--;
            showSlot(selectedslot);
        }
    }

    @Override
    protected void unfocus() {
        selectedslot = -1;
    }

    private void sortNames() {
        Collections.sort(names, new NameSlotComparator());
    }

    public void updateNameList(EntityPlayer player, String match) {
        names = RedstoneEther.get(true).getAllowedNames(player);
        this.match = match;
        sortNames();
    }

    public void removeName(String name) {
        names.remove(name);
        sortNames();
    }

    public void addName(String name) {
        names.add(name);
        sortNames();
    }

    public void clearNameList() {
        names.clear();
    }

    public static boolean doesNameMatch(String name, String match) {
        return name.length() >= match.length() && name.substring(0, match.length()).equalsIgnoreCase(match);
    }

    public String getSelectedName() {
        return selectedslot == -1 ? "" : names.get(selectedslot);
    }

    private ArrayList<String> names = new ArrayList<String>();
    private String match = "";
    private int selectedslot = -1;
}
