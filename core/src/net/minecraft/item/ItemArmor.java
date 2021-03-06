package net.minecraft.item;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.Player;
import net.minecraft.init.Items;
import net.minecraft.inventory.CreativeTabs;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.List;

public class ItemArmor extends Item {

	/**
	 * Holds the 'base' maxDamage that each armorType have.
	 */
	private static final int[] maxDamageArray = new int[] {11, 16, 15, 13};
	public static final String[] EMPTY_SLOT_NAMES = new String[] {
			"minecraft:items/empty_armor_slot_helmet", "minecraft:items/empty_armor_slot_chestplate", "minecraft:items/empty_armor_slot_leggings", "minecraft:items/empty_armor_slot_boots"
	};

	/**
	 * Stores the armor type: 0 is helmet, 1 is plate, 2 is legs and 3 is boots
	 */
	public final int armorType;

	/**
	 * Holds the amount of damage that the armor reduces at full durability.
	 */
	public final int damageReduceAmount;

	/**
	 * Used on RenderPlayer to select the correspondent armor to be rendered on the player: 0 is cloth, 1 is chain, 2 is
	 * iron, 3 is diamond and 4 is gold.
	 */
	public final int renderIndex;

	/**
	 * The EnumArmorMaterial used for this ItemArmor
	 */
	private final ItemArmor.ArmorMaterial material;

	public ItemArmor(ItemArmor.ArmorMaterial material, int renderIndex, int armorType) {
		this.material = material;
		this.armorType = armorType;
		this.renderIndex = renderIndex;
		this.damageReduceAmount = material.getDamageReductionAmount(armorType);
		this.setMaxDamage(material.getDurability(armorType));
		this.maxStackSize = 1;
		this.setCreativeTab(CreativeTabs.tabCombat);
	}

	public int getColorFromItemStack(ItemStack stack, int renderPass) {
		if (renderPass > 0) {
			return 16777215;
		}
		int i = this.getColor(stack);

		if (i < 0) {
			i = 16777215;
		}

		return i;
	}

	/**
	 * Return the enchantability factor of the item, most of the time is based on material.
	 */
	public int getItemEnchantability() {
		return this.material.getEnchantability();
	}

	/**
	 * Return the armor material for this armor item.
	 */
	public ItemArmor.ArmorMaterial getArmorMaterial() {
		return this.material;
	}

	/**
	 * Return whether the specified armor ItemStack has a color.
	 */
	public boolean hasColor(ItemStack stack) {
		return this.material != ItemArmor.ArmorMaterial.LEATHER ? false : !stack.hasTagCompound() ? false : !stack.getTagCompound().hasKey("display",
				10) ? false : stack.getTagCompound().getCompoundTag("display").hasKey("color", 3);
	}

	/**
	 * Return the color for the specified armor ItemStack.
	 */
	public int getColor(ItemStack stack) {
		if (this.material != ItemArmor.ArmorMaterial.LEATHER) {
			return -1;
		}
		NBTTagCompound nbttagcompound = stack.getTagCompound();

		if (nbttagcompound != null) {
			NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

			if (nbttagcompound1 != null && nbttagcompound1.hasKey("color", 3)) {
				return nbttagcompound1.getInteger("color");
			}
		}

		return 10511680;
	}

	/**
	 * Remove the color from the specified armor ItemStack.
	 */
	public void removeColor(ItemStack stack) {
		if (this.material == ItemArmor.ArmorMaterial.LEATHER) {
			NBTTagCompound nbttagcompound = stack.getTagCompound();

			if (nbttagcompound != null) {
				NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

				if (nbttagcompound1.hasKey("color")) {
					nbttagcompound1.removeTag("color");
				}
			}
		}
	}

	/**
	 * Sets the color of the specified armor ItemStack
	 */
	public void setColor(ItemStack stack, int color) {
		if (this.material != ItemArmor.ArmorMaterial.LEATHER) {
			throw new UnsupportedOperationException("Can\'t dye non-leather!");
		}
		NBTTagCompound nbttagcompound = stack.getTagCompound();

		if (nbttagcompound == null) {
			nbttagcompound = new NBTTagCompound();
			stack.setTagCompound(nbttagcompound);
		}

		NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

		if (!nbttagcompound.hasKey("display", 10)) {
			nbttagcompound.setTag("display", nbttagcompound1);
		}

		nbttagcompound1.setInteger("color", color);
	}

	/**
	 * Return whether this item is repairable in an anvil.
	 */
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return this.material.getRepairItem() == repair.getItem() ? true : super.getIsRepairable(toRepair, repair);
	}

	/**
	 * Called whenever this item is equipped and the right mouse button is pressed. Args: itemStack, world, entityPlayer
	 */
	public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, Player playerIn) {
		int i = EntityLivingBase.getArmorPosition(itemStackIn) - 1;
		ItemStack itemstack = playerIn.getCurrentArmor(i);

		if (itemstack == null) {
			playerIn.setCurrentItemOrArmor(i, itemStackIn.copy());
			itemStackIn.stackSize = 0;
		}

		return itemStackIn;
	}

	@Override
	public void addInformation(ItemStack stack, Player player, List<String> tooltip, boolean advanced) {
		tooltip.add("§9Защита: §a-" + getProtection(stack, player) + "%");
	}

	private float getProtection(ItemStack item, Player player) {

		DamageSource src = DamageSource.causePlayerDamage(player);
		int magic = EnchantmentHelper.getEnchantmentModifierDamageNoRandom(new ItemStack[] {item}, src);
		if (magic > 20) magic = 20;
		if (magic < 0) magic = 0;

		int o = 25 - magic;
		float m = 1 - o / 25.0F;
		float material = 1 - damageReduceAmount / 25F;
		return (int) ((1 - material * (1 - m)) * 1000) / 10F;

	}

	public static enum ArmorMaterial {
		LEATHER("leather", 5, new int[] {1, 3, 2, 1}, 15),
		CHAIN("chainmail", 15, new int[] {2, 5, 4, 1}, 12),
		IRON("iron", 15, new int[] {2, 6, 5, 2}, 9),
		GOLD("gold", 7, new int[] {2, 5, 3, 1}, 25),
		DIAMOND("diamond", 33, new int[] {3, 8, 6, 3}, 10);

		private final String name;
		private final int maxDamageFactor;
		private final int[] damageReductionAmountArray;
		private final int enchantability;

		private ArmorMaterial(String name, int maxDamage, int[] reductionAmounts, int enchantability) {
			this.name = name;
			this.maxDamageFactor = maxDamage;
			this.damageReductionAmountArray = reductionAmounts;
			this.enchantability = enchantability;
		}

		public int getDurability(int armorType) {
			return ItemArmor.maxDamageArray[armorType] * this.maxDamageFactor;
		}

		public int getDamageReductionAmount(int armorType) {
			return this.damageReductionAmountArray[armorType];
		}

		public int getEnchantability() {
			return this.enchantability;
		}

		public Item getRepairItem() {
			return this == LEATHER ? Items.leather : this == CHAIN ? Items.iron_ingot : this == GOLD ? Items.gold_ingot : this == IRON ? Items.iron_ingot : this == DIAMOND ? Items.diamond : null;
		}

		public String getName() {
			return this.name;
		}
	}

}
