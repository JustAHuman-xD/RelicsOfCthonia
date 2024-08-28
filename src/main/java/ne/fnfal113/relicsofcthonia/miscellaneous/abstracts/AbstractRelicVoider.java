package ne.fnfal113.relicsofcthonia.miscellaneous.abstracts;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.Getter;
import ne.fnfal113.relicsofcthonia.RelicsOfCthonia;
import ne.fnfal113.relicsofcthonia.relics.implementation.Rarity;
import ne.fnfal113.relicsofcthonia.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AbstractRelicVoider extends UnplaceableBlock {

    private static final CustomItemStack DECREMENT_CONDITION = new CustomItemStack(Material.RED_STAINED_GLASS_PANE,
            "&cDecrement Condition Quota",
            "&eClick &7to decrement the quota by 1",
            "&eShift Click &7to decrease the quota by 10"
    );

    private static final CustomItemStack INCREMENT_CONDITION = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE,
            "&aIncrement Condition Quota",
            "&eClick &7to increment the quota by 1",
            "&eShift Click &7to increase the quota by 10"
    );

    @Getter
    private final boolean globalNotifEnabled = RelicsOfCthonia.getInstance().getConfig().getBoolean("enable-relic-voider-notif", true);

    @Getter
    private final Rarity rarity;
    @Getter
    final NamespacedKey conditionKey;
    @Getter
    final NamespacedKey notifKey;

    public AbstractRelicVoider(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, Rarity rarity) {
        super(itemGroup, item, recipeType, recipe);

        this.rarity = rarity;
        this.conditionKey = Utils.createKey("condition_quota");
        this.notifKey = Utils.createKey("notif");
    }

    @SuppressWarnings("ConstantConditions")
    public void onInventoryClick(InventoryClickEvent event, ItemStack itemStack) {
        Material clickedType = event.getCurrentItem().getType();

        if (clickedType == Material.RED_STAINED_GLASS_PANE) {
            setConditionQuota(itemStack, event.getInventory(), event.isShiftClick() ? -10 : -1);
        } else if (clickedType == Material.GREEN_STAINED_GLASS_PANE) {
            setConditionQuota(itemStack, event.getInventory(), event.isShiftClick() ? 10 : 1);
        } else if (clickedType != ChestMenuUtils.getBackground().getType()) {
            toggleNotifEnabled(itemStack, event.getInventory());
        }
    }

    public void setConditionQuota(ItemStack itemStack, Inventory inventory, int integer) {
        ItemMeta meta = itemStack.getItemMeta();
        int conditionQuota = PersistentDataAPI.getInt(meta, getConditionKey(), 0);
        int finalCondition = conditionQuota + integer;
        finalCondition = Math.min(100, Math.max(0, finalCondition));
        PersistentDataAPI.setInt(meta, getConditionKey(), finalCondition);
        itemStack.setItemMeta(meta);

        boolean notifEnabled = PersistentDataAPI.getOptionalBoolean(meta, getNotifKey()).orElse(true);
        inventory.setItem(4, createMainStack(finalCondition, notifEnabled));
    }

    public void toggleNotifEnabled(ItemStack itemStack, Inventory inventory) {
        ItemMeta meta = itemStack.getItemMeta();
        int conditionQuota = PersistentDataAPI.getInt(meta, getConditionKey(), 0);
        boolean notifEnabled = PersistentDataAPI.getOptionalBoolean(meta, getNotifKey()).orElse(true);
        PersistentDataAPI.setBoolean(meta, getNotifKey(), !notifEnabled);
        inventory.setItem(4, createMainStack(conditionQuota, !notifEnabled));
    }

    public void onClick(ItemStack itemStack, Player player) {
        ItemMeta meta = itemStack.getItemMeta();
        int conditionQuota = PersistentDataAPI.getInt(meta, getConditionKey(), 0);
        boolean notifEnabled = PersistentDataAPI.getOptionalBoolean(meta, getNotifKey()).orElse(true);

        Inventory inventory = Bukkit.createInventory(null, 9, Utils.colorTranslator(meta.getDisplayName()));

        for (int i = 0; i < inventory.getSize(); i++){
            if (i == 0) {
                inventory.setItem(i, DECREMENT_CONDITION);
            } else if (i == 8) {
                inventory.setItem(i, INCREMENT_CONDITION);
            } else if (i == 4) {
                inventory.setItem(i, createMainStack(conditionQuota, notifEnabled));
            } else {
                inventory.setItem(i, ChestMenuUtils.getBackground());
            }
        }

        player.openInventory(inventory);
    }

    public void onRelicPickup(EntityPickupItemEvent event, ItemStack voider, Item pickedUpRelic, int pickedUpRelicCondition) {
        ItemMeta meta = voider.getItemMeta();
        int conditionQuota = PersistentDataAPI.getInt(meta, getConditionKey(), 0);
        boolean notifEnabled = PersistentDataAPI.getOptionalBoolean(meta, getNotifKey()).orElse(true);

        if (pickedUpRelicCondition <= conditionQuota) {
            if (isGlobalNotifEnabled() && notifEnabled) {
                Utils.sendRelicMessage("&6Successfully voided " + "&r" + pickedUpRelic.getItemStack().getItemMeta().getDisplayName(), event.getEntity());
            }

            pickedUpRelic.remove();
            event.setCancelled(true);
        }
    }

    public ItemStack createMainStack(int conditionQuota, boolean notifEnabled) {
        if (globalNotifEnabled) {
            return new CustomItemStack(Material.PURPLE_STAINED_GLASS,
                    "&cVoids &7any &f" + getRarity().name() + " &7relic whose",
                    "&6condition &7is below " + "&6&l" + conditionQuota + "%",
                    " ",
                    "&eClick &7to toggle the notification",
                    "&7Notification: " + (notifEnabled ? "&aEnabled" : "&cDisabled")
            );
        } else {
            return new CustomItemStack(Material.PURPLE_STAINED_GLASS,
                    "&cVoids &7any &f" + getRarity().name() + " &7relic whose",
                    "&6condition &7is below " + "&6&l" + conditionQuota + "%"
            );
        }
    }

}
