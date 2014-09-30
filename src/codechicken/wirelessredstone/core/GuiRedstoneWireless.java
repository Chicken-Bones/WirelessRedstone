package codechicken.wirelessredstone.core;

import java.util.ArrayList;

import codechicken.lib.util.LangProxy;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import codechicken.core.gui.GuiCCButton;
import codechicken.core.gui.GuiCCTextField;
import codechicken.core.gui.GuiScreenWidget;
import codechicken.lib.render.CCRenderState;

public class GuiRedstoneWireless extends GuiScreenWidget implements IGuiRemoteUseable
{
    private static LangProxy lang = new LangProxy("wrcbe_core");
    RedstoneEther ether = RedstoneEther.get(true);

    public GuiRedstoneWireless(InventoryPlayer inventoryplayer, ITileWireless tileentityredstonewireless) {
        super();
        itile = tileentityredstonewireless;
        etile = (TileEntity) tileentityredstonewireless;
        selectedfreq = itile.getFreq();
        inventory = inventoryplayer;
    }

    public GuiRedstoneWireless(InventoryPlayer inventoryplayer) {
        super();
        item = (ItemWirelessFreq) inventoryplayer.getCurrentItem().getItem();
        selectedfreq = item.getItemFreq(inventoryplayer.getCurrentItem());
        inventory = inventoryplayer;
    }

    @Override
    public void initGui() {
        largeGui = SaveManager.config().getTag("AdvancedGui").getBooleanValue(false);
        if (largeGui) {
            xSize = 236;
            ySize = 190;
        } else {
            xSize = 176;
            ySize = 166;
        }
        super.initGui();
    }

    @Override
    public void addWidgets() {
        if (largeGui) {
            add(new GuiCCButton(218, 4, 14, 15, "x").setActionCommand("close"));
            add(new GuiCCButton(135, 20, 20, 20, "+").setActionCommand("+1"));
            add(new GuiCCButton(81, 20, 20, 20, "-").setActionCommand("-1"));
            add(new GuiCCButton(158, 20, 20, 20, "+10").setActionCommand("+10"));
            add(new GuiCCButton(58, 20, 20, 20, "-10").setActionCommand("-10"));
            add(new GuiCCButton(180, 20, 26, 20, "+100").setActionCommand("+100"));
            add(new GuiCCButton(29, 20, 26, 20, "-100").setActionCommand("-100"));
            add(new GuiCCButton(174, 42, 32, 20, "+1000").setActionCommand("+1000"));
            add(new GuiCCButton(29, 42, 32, 20, "-1000").setActionCommand("-1000"));

            add(new GuiCCButton(118 + 63, 105 + 62, 50, 16, lang.translate("button.simple")).setActionCommand("toggleSize"));

            add(setfreqbutton = new GuiCCButton(143, 45, 26, 20, lang.translate("button.set")).setActionCommand("setFreq"));
            add(setnamebutton = new GuiCCButton(171, 83, 50, 20, "").setActionCommand("setName"));
            add(setcolourbutton = new GuiCCButton(118 + 53, 105 + 35, 61, 20, lang.translate("button.colour")).setActionCommand("setColour"));

            add(new GuiButtonArrow(118 + 64, 105 + 17, 12, 12, 3).setActionCommand("nextItem"));
            add(new GuiButtonArrow(118 + 90, 105 + 17, 12, 12, 1).setActionCommand("prevItem"));

            add(slotnames = new GuiNameSlot(18, 104));

            add(textboxfreq = new GuiCCTextField(118 - 20, 105 - 59, 40, 18, "")
                    .setActionCommand("setFreq")
                    .setMaxStringLength(4)
                    .setAllowedCharacters("0123456789"));

            add(textboxname = new GuiCCTextField(118 - 100, 105 - 20, 150, 16, "")
                    .setActionCommand("setName")
                    .setMaxStringLength(20));
            add(dyeslot = new GuiInvItemSlot(118 + 75, 105 + 15, inventory, RedstoneEther.getColourSetters(), 14).setActionCommand("setColour"));

            updateColourSetButton();
            updateFreqSetButton();
            reloadNameText();
            updateNames();
        } else {
            add(new GuiCCButton(88 + 69, 83 - 77, 14, 15, "x").setActionCommand("close"));
            add(new GuiCCButton(88 + 13, 83 - 50, 20, 20, "+").setActionCommand("+1"));
            add(new GuiCCButton(88 - 33, 83 - 50, 20, 20, "-").setActionCommand("-1"));
            add(new GuiCCButton(88 + 35, 83 - 50, 20, 20, "+10").setActionCommand("+10"));
            add(new GuiCCButton(88 - 55, 83 - 50, 20, 20, "-10").setActionCommand("-10"));
            add(new GuiCCButton(88 + 57, 83 - 50, 26, 20, "+100").setActionCommand("+100"));
            add(new GuiCCButton(88 - 83, 83 - 50, 26, 20, "-100").setActionCommand("-100"));
            add(new GuiCCButton(88 + 39, 83 - 28, 32, 20, "+1000").setActionCommand("+1000"));
            add(new GuiCCButton(88 - 70, 83 - 28, 32, 20, "-1000").setActionCommand("-1000"));

            add(new GuiCCButton(88 - 27, 83 - 15, 54, 16, lang.translate("button.advanced")).setActionCommand("toggleSize"));
        }
    }

    public void updateScreen() {
        if (itile != null && mc.theWorld.getTileEntity(etile.xCoord, etile.yCoord, etile.zCoord) != etile)//tile changed
        {
            mc.currentScreen = null;
            mc.setIngameFocus();
        }

        super.updateScreen();

        if (toggle) {
            largeGui = !largeGui;
            SaveManager.config().getTag("AdvancedGui").setBooleanValue(largeGui);
            reset();
            toggle = false;
        }

        if (largeGui) {
            updateColourSetButton();
            updateFreqSetButton();
            updateNames();
        }
    }

    @Override
    public void drawScreen(int mousex, int mousey, float f) {
        drawDefaultBackground();
        super.drawScreen(mousex, mousey, f);
    }

    @Override
    public void drawForeground() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        if (largeGui) {
            drawCentered(getSetObjectName(), 118, 105 - 100, 0x303030);
            drawRight(lang.translate("label.value"), 96, 105 - 54, 0x404040);
            drawCentered(lang.translate("label.name"), 94, 105 - 31, 0x404040);
            drawCentered(lang.translate("label.colour"), 201, 107, ether.getFreqColour(selectedfreq));

            String sactualfreq = Integer.toString(getSetObjectFreq());
            if (!ether.canBroadcastOnFrequency(mc.thePlayer, selectedfreq))//jammed freq render
            {
                drawCentered("" + selectedfreq, 118, 105 - 82, 0xFF4040);
                drawCentered(sactualfreq, 118, 105 - 72, 0x4040FF);
            } else {
                drawCentered(sactualfreq, 118, 105 - 79, 0xFFFFFF);
            }
        } else {
            drawCentered(getSetObjectName(), 88, 83 - 78, 0x303030);
            drawCentered(lang.translate("label.freq"), 88, 83 - 67, 0x404040);

            String sactualfreq = Integer.toString(getSetObjectFreq());
            if (!ether.canBroadcastOnFrequency(mc.thePlayer, selectedfreq))//jammed freq render
            {
                drawCentered("" + selectedfreq, 88, 83 - 47, 0xFF4040);
                drawCentered(sactualfreq, 88, 83 - 37, 0x4040FF);
            } else {
                drawCentered(sactualfreq, 88, 83 - 44, 0xFFFFFF);
            }
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private void drawCentered(String s, int x, int y, int colour) {
        fontRendererObj.drawString(s, x - fontRendererObj.getStringWidth(s) / 2, y, colour);
    }

    private void drawRight(String s, int x, int y, int colour) {
        fontRendererObj.drawString(s, x - fontRendererObj.getStringWidth(s), y, colour);
    }

    private void updateFreqSetButton() {
        String s = textboxfreq.getText();
        int i;
        try {
            i = Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            i = -1;
        }

        setfreqbutton.setEnabled(i >= 0 && i <= RedstoneEther.numfreqs);
    }

    private void updateColourSetButton() {
        int selected = dyeslot.getSelectedIndex();
        if (selected == -1) {
            setcolourbutton.setEnabled(false);
            return;
        }
        if (selected == RedstoneEther.numcolours)
            selected = -1;

        setcolourbutton.setEnabled(selectedfreq != 0 && selected != ether.getFreqColourId(selectedfreq) &&
                ether.canBroadcastOnFrequency(mc.thePlayer, selectedfreq));
    }

    private boolean nameIsUsed(String name) {
        ArrayList<String> names = ether.getAllNames();
        return names.contains(name);
    }

    private void updateNames() {
        String name = ether.getFreqName(selectedfreq);
        String tname = textboxname.getText();
        boolean set = name == null || name.equals("") || !name.equals(tname);
        boolean canset = !tname.equals("") && !nameIsUsed(tname) && selectedfreq != 0;

        setnamebutton.setEnabled((set && canset || !set) &&
                ether.canBroadcastOnFrequency(mc.thePlayer, selectedfreq));
        setnamebutton.text = set ? "Set Name" : "Remove";

        if (set) {
            slotnames.updateNameList(mc.thePlayer, textboxname.getText());
        } else if (name.equals(tname)) {
            slotnames.updateNameList(mc.thePlayer, "");//set name can pick from box
            slotnames.removeName(tname);
        } else {
            slotnames.clearNameList();
        }
    }

    private void reloadNameText() {
        String name = ether.getFreqName(selectedfreq);
        if (name == null)
            name = "";

        textboxname.setText(name);
    }

    @Override
    public void actionPerformed(String ident, Object... params) {
        if (ident.equals("close")) {
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
        } else if (ident.startsWith("+") || ident.startsWith("-")) {
            selectedfreq += Integer.parseInt(ident.startsWith("+") ? ident.substring(1) : ident);
            if (selectedfreq > RedstoneEther.numfreqs)
                selectedfreq -= RedstoneEther.numfreqs + 1;
            if (selectedfreq < 0)
                selectedfreq += RedstoneEther.numfreqs + 1;
            setNewFreq();
        } else if (ident.equals("toggleSize")) {
            toggle = true;
        } else if (ident.equals("setFreq")) {
            selectedfreq = Integer.parseInt(textboxfreq.getText());
            setNewFreq();
        } else if (ident.equals("setName")) {
            if (setnamebutton.isEnabled()) {
                if (setnamebutton.text.equals("Set Name")) {
                    ether.setFreqName(selectedfreq, textboxname.getText());
                    WRCoreCPH.sendSetFreqInfo(selectedfreq, textboxname.getText(), ether.getFreqColourId(selectedfreq));
                } else {
                    ether.setFreqName(selectedfreq, "");
                    WRCoreCPH.sendSetFreqInfo(selectedfreq, "", ether.getFreqColourId(selectedfreq));
                    textboxname.setText("");
                }
            }
        } else if (ident.equals("setColour")) {
            int colourid = dyeslot.getSelectedIndex();
            if (colourid == -1)
                return;

            if (colourid == RedstoneEther.numcolours)
                colourid = -1;

            if (colourid == ether.getFreqColourId(selectedfreq))
                return;

            dyeslot.decrementCurrentStack();
            ether.setFreqColour(selectedfreq, colourid);
            WRCoreCPH.sendSetFreqInfo(selectedfreq, ether.getFreqName(selectedfreq), colourid);
        } else if (ident.equals("nextItem")) {
            dyeslot.cycleNextItem();
        } else if (ident.equals("prevItem")) {
            dyeslot.cyclePrevItem();
        } else if (ident.equals("selectName")) {
            String slotname = slotnames.getSelectedName();
            if (slotname.equals(""))
                return;

            int freq = ether.getFreqByName(slotname);
            if (freq != -1) {
                selectedfreq = freq;
                setNewFreq();
            }
        }
    }

    private void setNewFreq() {
        if (ether.canBroadcastOnFrequency(mc.thePlayer, selectedfreq)) {
            if (itile == null)
                item.setFreq(inventory.player, inventory.currentItem, inventory.getCurrentItem(), selectedfreq);
            else
                RedstoneEther.get(true).setFreq(itile, selectedfreq);
        }

        if (largeGui)
            reloadNameText();
    }

    private String getSetObjectName() {
        return itile == null ? item.getGuiName() : itile.getGuiName();
    }

    private int getSetObjectFreq() {
        return itile == null ? item.getItemFreq(inventory.getCurrentItem()) : itile.getFreq();
    }

    @Override
    public void drawBackground() {
        if (largeGui)
            CCRenderState.changeTexture("wrcbe_core:textures/gui/wirelessLarge.png");
        else
            CCRenderState.changeTexture("wrcbe_core:textures/gui/wirelessSmall.png");
        GL11.glColor4f(1, 1, 1, 1);
        drawTexturedModalRect(0, 0, 0, 0, xSize, ySize);
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private GuiCCTextField textboxname;
    private GuiCCTextField textboxfreq;
    private GuiNameSlot slotnames;
    private GuiInvItemSlot dyeslot;

    private GuiCCButton setfreqbutton;
    private GuiCCButton setnamebutton;
    private GuiCCButton setcolourbutton;

    private int selectedfreq;
    private boolean toggle;

    protected TileEntity etile;
    protected ITileWireless itile;
    protected ItemWirelessFreq item;
    protected InventoryPlayer inventory;

    public boolean largeGui;
}
