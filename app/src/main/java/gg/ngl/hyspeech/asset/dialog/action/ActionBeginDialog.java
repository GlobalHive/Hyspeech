package gg.ngl.hyspeech.asset.dialog.action;

import com.hypixel.hytale.builtin.adventure.objectives.Objective;
import com.hypixel.hytale.builtin.adventure.objectives.ObjectiveDataStore;
import com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin;
import com.hypixel.hytale.builtin.adventure.objectives.task.ObjectiveTask;
import com.hypixel.hytale.builtin.adventure.objectives.task.UseEntityObjectiveTask;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerConfigData;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

import gg.ngl.hyspeech.Hyspeech;
import gg.ngl.hyspeech.asset.dialog.HyspeechDialogAsset;
import gg.ngl.hyspeech.asset.dialog.HyspeechDialogRequirement;
import gg.ngl.hyspeech.asset.dialog.action.builder.BuilderActionBeginDialog;
import gg.ngl.hyspeech.player.HyspeechPlayerConfig;
import gg.ngl.hyspeech.player.ui.page.HyspeechDialogPage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 *
 * Hyspeech - Character dialog system for Hytale
 * Copyright (C) 2026 Naughty-Klaus
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

public class ActionBeginDialog extends ActionBase {
    protected final String dialogId;

    public ActionBeginDialog(@Nonnull BuilderActionBeginDialog builder, @Nonnull BuilderSupport support) {
        super(builder);
        this.dialogId = builder.getDialogId(support);
    }

    @Override
    public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
        return super.canExecute(ref, role, sensorInfo, dt, store) && role.getStateSupport().getInteractionIterationTarget() != null;
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
        if (canExecute(ref, role, sensorInfo, dt, store)) {
            Ref<EntityStore> playerReference = role.getStateSupport().getInteractionIterationTarget();
            if (playerReference == null) {
                return false;
            }

            PlayerRef playerRefComponent = store.getComponent(playerReference, PlayerRef.getComponentType());
            if (playerRefComponent == null) {
                return false;
            }

            Player playerComponent = store.getComponent(playerReference, Player.getComponentType());
            if (playerComponent == null) {
                return false;
            }

            String initialDialogId = resolveInitialDialogId(store, playerComponent);
            if (initialDialogId == null) {
                return false;
            }

            playerComponent.getPageManager().openCustomPage(ref, store,
                    new HyspeechDialogPage(ref, store, playerRefComponent, initialDialogId));

            super.execute(ref, role, sensorInfo, dt, store);
            return true;
        }
        return false;
    }

    @Nullable
    private String resolveInitialDialogId(@Nonnull Store<EntityStore> store, @Nonnull Player playerComponent) {
        String currentDialogId = this.dialogId;
        Set<String> visitedDialogIds = new HashSet<>();

        while (currentDialogId != null && !currentDialogId.isBlank()) {
            if (!visitedDialogIds.add(currentDialogId)) {
                return null;
            }

            HyspeechDialogAsset asset = HyspeechDialogAsset.getAssetMap().getAsset(currentDialogId);
            if (asset == null) {
                return currentDialogId;
            }

            if (areRequirementsMet(asset, store, playerComponent)) {
                return currentDialogId;
            }

            currentDialogId = asset.getFail();
        }

        return null;
    }

    private boolean areRequirementsMet(@Nonnull HyspeechDialogAsset asset,
                                       @Nonnull Store<EntityStore> store,
                                       @Nonnull Player playerComponent) {
        HyspeechDialogRequirement[] requirements = asset.getRequirements();
        if (requirements == null || requirements.length == 0) {
            return true;
        }

        HyspeechPlayerConfig playerConfig = Hyspeech.hyspeechPlayerMap.get(playerComponent.getPlayerRef()).getConfig().get();

        ItemContainer allItems = playerComponent.getInventory().getCombinedHotbarFirst();
        for (HyspeechDialogRequirement requirement : requirements) {
            if (requirement == null) {
                continue;
            }

            String itemId = requirement.getItemId();
            String taskId = requirement.getTaskId();
            String metaData = requirement.getMetaData();
            boolean hasItemRequirement = itemId != null && !itemId.isBlank();
            boolean hasTaskRequirement = taskId != null && !taskId.isBlank();
            boolean hasMetaDataRequirement = metaData != null && !metaData.isBlank();

            if (!hasItemRequirement && !hasTaskRequirement && !hasMetaDataRequirement) {
                return false;
            }

            if (hasItemRequirement) {
                int requiredAmount = requirement.getAmount();
                int currentAmount = 0;
                for (short slot = 0; slot < allItems.getCapacity(); slot++) {
                    ItemStack stack = allItems.getItemStack(slot);
                    if (stack == null || stack.isEmpty()) {
                        continue;
                    }

                    if (!itemId.equals(stack.getItemId())) {
                        continue;
                    }

                    currentAmount += stack.getQuantity();
                    if (requiredAmount > 0 && currentAmount >= requiredAmount) {
                        break;
                    }
                }

                if (requiredAmount == 0) {
                    if (currentAmount > 0) {
                        return false;
                    }
                } else {
                    int minimumRequiredAmount = Math.max(1, requiredAmount);
                    if (currentAmount < minimumRequiredAmount) {
                        return false;
                    }
                }
            }

            if (hasTaskRequirement) {
                if (!isTaskRequirementMet(playerComponent, store, taskId)) {
                    return false;
                }
            }

            if (hasMetaDataRequirement && !isMetaDataRequirementMet(playerConfig, metaData)) {
                return false;
            }
        }

        return true;
    }

    private boolean isTaskRequirementMet(@Nonnull Player playerComponent,
                                         @Nonnull Store<EntityStore> store,
                                         @Nonnull String taskRequirement) {
        boolean isNegative = taskRequirement.startsWith("-");
        String normalizedTaskId = isNegative ? taskRequirement.substring(1) : taskRequirement;
        if (normalizedTaskId.isBlank()) {
            return false;
        }

        boolean hasTask = hasTaskLive(playerComponent, store, normalizedTaskId);
        return isNegative ? !hasTask : hasTask;
    }

    private boolean isMetaDataRequirementMet(@Nonnull HyspeechPlayerConfig playerConfig,
                                             @Nonnull String metaDataRequirement) {
        boolean isNegative = metaDataRequirement.startsWith("-");
        String normalizedMetaData = isNegative ? metaDataRequirement.substring(1) : metaDataRequirement;
        if (normalizedMetaData.isBlank()) {
            return false;
        }

        boolean hasMetaData = playerConfig.hasMetaData(normalizedMetaData);
        return isNegative ? !hasMetaData : hasMetaData;
    }

    private boolean hasTaskLive(@Nonnull Player playerComponent, @Nonnull Store<EntityStore> store, @Nonnull String taskId) {
        ObjectivePlugin objectivePlugin = ObjectivePlugin.get();
        if (objectivePlugin == null) {
            return false;
        }

        ObjectiveDataStore objectiveDataStore = objectivePlugin.getObjectiveDataStore();
        if (objectiveDataStore == null) {
            return false;
        }

        PlayerConfigData playerData = playerComponent.getPlayerConfigData();
        if (playerData == null || playerData.getActiveObjectiveUUIDs() == null) {
            return false;
        }

        for (UUID objectiveUuid : playerData.getActiveObjectiveUUIDs()) {
            Objective objective = objectiveDataStore.getObjective(objectiveUuid);
            if (objective == null) {
                objective = objectiveDataStore.loadObjective(objectiveUuid, store);
            }

            if (objective == null || objective.getCurrentTasks() == null) {
                continue;
            }

            for (ObjectiveTask objectiveTask : objective.getCurrentTasks()) {
                if (!(objectiveTask instanceof UseEntityObjectiveTask useEntityTask)) {
                    continue;
                }

                if (taskId.equals(useEntityTask.getAsset().getTaskId())) {
                    return true;
                }
            }
        }

        return false;
    }

}
