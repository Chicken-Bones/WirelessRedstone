package codechicken.wirelessredstone.core;

import org.lwjgl.input.Keyboard;

import codechicken.core.gui.GuiWidget;

import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class GuiInvItemSlot extends GuiWidget
{
    private static RenderItem itemRenderer = new RenderItem();
    
    protected ItemStack[] invitems;
    protected int[] invslotnumbers;
    protected ItemStack[] defaultitems;
    protected InventoryPlayer inv;
    protected int selecteditem;
    
    public boolean focused;
    public String actionCommand;

    public GuiInvItemSlot(int x, int y, InventoryPlayer playerinv, ItemStack[] defaults, int selection)
    {
        super(x, y, 16, 16);
        
        defaultitems = defaults;
        invitems = new ItemStack[defaultitems.length];
        invslotnumbers = new int[defaultitems.length];
        inv = playerinv;
        searchInventoryItems();
        selectItem(selection);
    }
    
    public GuiInvItemSlot setActionCommand(String s)
    {
        actionCommand = s;
        return this;
    }
    
    @Override
    public void draw(int mousex, int mousey, float frame)
    {
        drawSlotBox(x, y);
        if(invitems[selecteditem] != null)
            itemRenderer.renderItemIntoGUI(fontRenderer, renderEngine, invitems[selecteditem], x, y);
    }
    
    public void drawSlotBox(int x, int y)
    {
        drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);//big grey
        drawRect(x - 1, y - 1, x + 16, y + 16, 0xFF373737);//small black
        drawRect(x + 0, y + 0, x + 17, y + 17, 0xFFFFFFFF);//small white
        drawRect(x + 0, y + 0, x + 16, y + 16, 0xFF8B8B8B);//small grey
    }
    
    public void setFocused(boolean focus)
    {
        focused = focus;
    }

    @Override
    public void mouseClicked(int mousex, int mousey, int button)
    {
        setFocused(pointInside(mousex, mousey));
    }
    
    @Override
    public void keyTyped(char c, int keyindex)
    {
        if(!focused)
            return;
        
        if(keyindex == Keyboard.KEY_LEFT)
            cyclePrevItem();
        if(keyindex == Keyboard.KEY_RIGHT)
            cycleNextItem();
        if(keyindex == Keyboard.KEY_RETURN && actionCommand != null)
            sendAction(actionCommand);
    }
    
    public void decrementCurrentStack()
    {
        if(invitems[selecteditem] == null)
        {
            return;
        }

        int slot = invslotnumbers[selecteditem];
        if(inv.player.worldObj.isRemote)
            WRCoreCPH.sendDecrementSlot(slot);
        
        ItemStack item = invitems[selecteditem];
        item.stackSize -= 1;
        
        if(item.stackSize == 0)
        {
            inv.mainInventory[slot] = null;
            searchInventoryItems();
            selectItem(selecteditem);
        }
    }
    
    public boolean currentStackExists()
    {
        return invitems[selecteditem] != null;
    }
    
    public void selectItem(int index)
    {
        selecteditem = index;
        if(invitems[selecteditem] == null)
            cycleNextItem();
    }
    
    public void cycleNextItem()
    {
        int cycleindex = selecteditem;
        while(true)
        {
            cycleindex++;
            if(cycleindex >= invitems.length)
                cycleindex = 0;
            if(cycleindex == selecteditem)
                return;
            
            if(invitems[cycleindex] != null)
            {
                selecteditem = cycleindex;
                return;
            }
        }
    }
    
    public void cyclePrevItem()
    {
        int cycleindex = selecteditem;
        while(true)
        {
            cycleindex--;
            if(cycleindex < 0)
                cycleindex = invitems.length - 1;
            if(cycleindex == selecteditem)
                return;
            
            if(invitems[cycleindex] != null)
            {
                selecteditem = cycleindex;
                return;
            }
        }
    }
    
    private void searchInventoryItems()
    {
        for(int i = 0; i < defaultitems.length; i++)
        {
            invitems[i] = null;
            invslotnumbers[i] = -1;
            for(int j = 0; j < inv.mainInventory.length; j++)
            {
                ItemStack invstack = inv.getStackInSlot(j);
                if(invstack == null)
                    continue;
                
                if(defaultitems[i].isItemEqual(invstack))
                {
                    invitems[i] = invstack;
                    invslotnumbers[i] = j;
                    break;
                }
            }
        }
    }

    public int getSelectedIndex()
    {
        return invitems[selecteditem] == null ? -1 : selecteditem;
    }
}
