package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.element.GuiButton;
import net.minecraft.client.resources.Lang;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.settings.Settings;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

public class GuiKeyBindingList extends GuiListExtended {

	private final GuiControls guiControls;
	private final Minecraft mc;
	private final GuiListExtended.IGuiListEntry[] listEntries;
	private int maxListLabelWidth = 0;

	public GuiKeyBindingList(GuiControls controls, Minecraft mc) {
		super(mc, controls.width, controls.height, 63, controls.height - 32, 20);
		this.guiControls = controls;
		this.mc = mc;
		KeyBinding[] akeybinding = ArrayUtils.clone(KeyBinding.values());
		this.listEntries = new GuiListExtended.IGuiListEntry[akeybinding.length + KeyBinding.getKeybinds().size()];
		Arrays.sort(akeybinding);
		int i = 0;
		String s = null;

		for (KeyBinding keybinding : akeybinding) {
			String s1 = keybinding.getKeyCategory();

			if (!s1.equals(s)) {
				s = s1;
				this.listEntries[i++] = new GuiKeyBindingList.CategoryEntry(s1);
			}

			int j = mc.fontRendererObj.getStringWidth(Lang.format(keybinding.getKeyDescription()));

			if (j > this.maxListLabelWidth) this.maxListLabelWidth = j;

			this.listEntries[i++] = new GuiKeyBindingList.KeyEntry(keybinding);
		}
	}

	protected int getSize() {
		return this.listEntries.length;
	}

	/**
	 * Gets the IGuiListEntry object for the given index
	 */
	public GuiListExtended.IGuiListEntry getListEntry(int index) {
		return this.listEntries[index];
	}

	protected int getScrollBarX() {
		return super.getScrollBarX() + 15;
	}

	/**
	 * Gets the width of the list
	 */
	public int getListWidth() {
		return super.getListWidth() + 32;
	}

	public class CategoryEntry implements GuiListExtended.IGuiListEntry {

		private final String labelText;
		private final int labelWidth;

		public CategoryEntry(String p_i45028_2_) {
			this.labelText = Lang.format(p_i45028_2_);
			this.labelWidth = GuiKeyBindingList.this.mc.fontRendererObj.getStringWidth(this.labelText);
		}

		public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
			GuiKeyBindingList.this.mc.fontRendererObj.drawString(this.labelText, GuiKeyBindingList.this.mc.currentScreen.width / 2 - this.labelWidth / 2, y + slotHeight - GuiKeyBindingList.this.mc.fontRendererObj.getFontHeight() - 1, 16777215);
		}

		public boolean mousePressed(int slotIndex, int p_148278_2_, int p_148278_3_, int p_148278_4_, int p_148278_5_, int p_148278_6_) {
			return false;
		}

		public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
		}

		public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {
		}

	}

	public class KeyEntry implements GuiListExtended.IGuiListEntry {

		private final KeyBinding keybinding;
		private final String keyDesc;
		private final GuiButton btnChangeKeyBinding;
		private final GuiButton btnReset;

		private KeyEntry(KeyBinding p_i45029_2_) {
			this.keybinding = p_i45029_2_;
			this.keyDesc = Lang.format(p_i45029_2_.getKeyDescription());
			this.btnChangeKeyBinding = new GuiButton(0, 0, 0, 75, 20, Lang.format(p_i45029_2_.getKeyDescription()));
			this.btnReset = new GuiButton(0, 0, 0, 50, 20, Lang.format("controls.reset"));
		}

		public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
			boolean flag = GuiKeyBindingList.this.guiControls.editing == this.keybinding;
			GuiKeyBindingList.this.mc.fontRendererObj.drawString(this.keyDesc, x + 90 - GuiKeyBindingList.this.maxListLabelWidth, y + slotHeight / 2 - GuiKeyBindingList.this.mc.fontRendererObj.getFontHeight() / 2, 16777215);
			this.btnReset.xPosition = x + 190;
			this.btnReset.yPosition = y;
			this.btnReset.enabled = this.keybinding.getKeyCode() != this.keybinding.getKeyCodeDefault();
			this.btnReset.drawButton(GuiKeyBindingList.this.mc, mouseX, mouseY);
			this.btnChangeKeyBinding.xPosition = x + 105;
			this.btnChangeKeyBinding.yPosition = y;
			this.btnChangeKeyBinding.displayString = keybinding.getKeyDisplayString();
			boolean flag1 = false;

			if (this.keybinding.getKeyCode() != 0) for (KeyBinding keybinding : KeyBinding.values())
				if (keybinding != this.keybinding && keybinding.getKeyCode() == this.keybinding.getKeyCode()) {
					flag1 = true;
					break;
				}

			if (flag) this.btnChangeKeyBinding.displayString = EnumChatFormatting.WHITE + "> " + EnumChatFormatting.YELLOW + this.btnChangeKeyBinding.displayString + EnumChatFormatting.WHITE + " <";
			else if (flag1) this.btnChangeKeyBinding.displayString = EnumChatFormatting.RED + this.btnChangeKeyBinding.displayString;

			this.btnChangeKeyBinding.drawButton(GuiKeyBindingList.this.mc, mouseX, mouseY);
		}

		public boolean mousePressed(int slotIndex, int p_148278_2_, int p_148278_3_, int p_148278_4_, int p_148278_5_, int p_148278_6_) {
			if (this.btnChangeKeyBinding.mousePressed(GuiKeyBindingList.this.mc, p_148278_2_, p_148278_3_)) {
				GuiKeyBindingList.this.guiControls.editing = this.keybinding;
				return true;
			}
			if (this.btnReset.mousePressed(GuiKeyBindingList.this.mc, p_148278_2_, p_148278_3_)) {
				keybinding.setKeyCode(keybinding.getKeyCodeDefault());
				Settings.saveOptions();
				KeyBinding.resetKeyBindingArrayAndHash();
				return true;
			}
			return false;
		}

		public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
			this.btnChangeKeyBinding.mouseReleased(x, y);
			this.btnReset.mouseReleased(x, y);
		}

		public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {}

	}

}