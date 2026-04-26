package gg.ngl.hyspeech.player.ui.page;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.ngl.hyspeech.Hyspeech;
import gg.ngl.hyspeech.asset.dialog.HyspeechDialogAsset;
import gg.ngl.hyspeech.asset.dialog.HyspeechDialogType;
import gg.ngl.hyspeech.asset.dialog.event.ChoiceSelectedEvent;
import gg.ngl.hyspeech.asset.dialog.event.DialogEventContext;
import gg.ngl.hyspeech.asset.dialog.event.DialogInputReceivedEvent;
import gg.ngl.hyspeech.asset.dialog.event.NextDialogEvent;
import gg.ngl.hyspeech.asset.macro.HyspeechMacroAsset;
import gg.ngl.hyspeech.util.param.ParameterContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.stream.Collectors;

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

public class HyspeechDialogPage extends InteractiveCustomUIPage<HyspeechDialogPageData> {

    private static final AssetStore<String, HyspeechDialogAsset, DefaultAssetMap<String, HyspeechDialogAsset>>
            STORE = AssetRegistry.getAssetStore(HyspeechDialogAsset.class);
    @Nonnull
    private final Ref<EntityStore> ref;
    @Nonnull
    private final Store<EntityStore> store;
    public HyspeechDialogType currentDialogType = HyspeechDialogType.UNSET;
    public boolean isProcessing = true;
    public String key;

    public String input = "";

    public HyspeechDialogPage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PlayerRef playerRef, String key) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, HyspeechDialogPageData.CODEC);
        setKey(key);

        this.ref = ref;
        this.store = store;
    }

    public Ref<EntityStore> getRef() {
        return ref;
    }

    public Store<EntityStore> getStore() {
        return store;
    }

    private DialogEventContext createEventContext(ParameterContext params) {
        return new DialogEventContext(
                key,
                playerRef,
                ref,
                store,
                params,
                Hyspeech.get()
        );
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> entRef, @Nonnull UICommandBuilder commands, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> entStore) {
        HyspeechDialogAsset asset = getAsset();

        if (asset == null) {
            this.close();
            return;
        }

        currentDialogType = asset.getType();

        if (currentDialogType.equals(HyspeechDialogType.UNSET)) {
            close();
            return;
        }

        commands.append(currentDialogType.uiPath);

        if(currentDialogType.isDialog() || currentDialogType.isInput())
            if (asset.getNext() == null)
                commands.set("#NextButton.Text", "CLOSE");

        ParameterContext ctx = new ParameterContext();
        ctx.put(PlayerRef.class, playerRef);
        ctx.put(Hyspeech.class, Hyspeech.get());
        Hyspeech.get().populateContext(ctx);

        Message message;
        if(asset.getName() != null && !asset.getName().isEmpty()) {
            message = Message.translation(asset.getName());
            message = Message.translation(Hyspeech.get().process(message.getAnsiMessage(), ctx));
        } else {
            message = Message.translation("hyspeech.dialog." + asset.getId() + ".name");
            message = Message.translation(Hyspeech.get().process(message.getAnsiMessage(), ctx));
        }

        commands.set("#NameTitle.TextSpans", message);

        if(currentDialogType.isInput()) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#Input", EventData.of("@Input", "#ContentGroup #Input.Value"), false);
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#NextButton",
                    EventData.of("InputNext", "true")
            );
        } else if (currentDialogType.isDialog()) {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#NextButton",
                    EventData.of("DialogNext", "true")
            );
        } else if (currentDialogType.isChoice()) {
            if(asset.entries != null && asset.entries.length > 0) {
                for (int i = 0; i < asset.entries.length; i++) {
                    eventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            "#Content" + i,
                            EventData.of("Choice" + i, "true")
                    );
                }
            }
        }

        /*
            Entry fulfillment is not needed for input dialog.
         */
        if(!currentDialogType.isInput())
            if(asset.entries != null && asset.entries.length > 0) {
                for (int i = 0; i < asset.entries.length; i++) {
                    message = Message.translation(asset.entries[i].content);
                    message = Message.translation(Hyspeech.get().process(message.getAnsiMessage(), ctx));
                    commands.set("#Content" + i + ".TextSpans", message);
                }
            }

        if (currentDialogType.equals(HyspeechDialogType.UNSET))
            this.close();

        isProcessing = false;
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> entRef, @Nonnull Store<EntityStore> entStore, @Nonnull HyspeechDialogPageData data) {
        boolean needsUpdate = false;

        HyspeechDialogAsset asset = getAsset();

        if (asset == null) {
            this.close();
            return;
        }

        if(data.input() != null) {
            this.input = data.input();

            needsUpdate = true;
        }

        if (!this.isProcessing) {
            if (Boolean.TRUE.equals(data.inputNext())) {
                ParameterContext params = new ParameterContext();
                params.put(PlayerRef.class, playerRef);
                params.put(Hyspeech.class, Hyspeech.get());
                Hyspeech.get().populateContext(params);

                DialogEventContext ctx = createEventContext(params);
                Hyspeech.get().dialogEvents()
                        .dispatch(key, new DialogInputReceivedEvent(ctx, asset, this.input));

                this.input = "";
                needsUpdate = true;
                isProcessing = true;
                executeMacro(asset.getMacro());
                setKey(asset.getNext());
            } else if (Boolean.TRUE.equals(data.dialogNext())) {
                ParameterContext params = new ParameterContext();
                params.put(PlayerRef.class, playerRef);
                params.put(Hyspeech.class, Hyspeech.get());
                Hyspeech.get().populateContext(params);

                DialogEventContext ctx = createEventContext(params);
                Hyspeech.get().dialogEvents()
                        .dispatch(key, new NextDialogEvent(ctx, asset));

                isProcessing = true;
                executeMacro(asset.getMacro());

                setKey(asset.getNext());
            }

            for (int i = 0; i < 4; i++) {
                if (Boolean.TRUE.equals(data.getEntry(i))) {
                    ParameterContext params = new ParameterContext();
                    params.put(PlayerRef.class, playerRef);
                    params.put(Hyspeech.class, Hyspeech.get());
                    Hyspeech.get().populateContext(params);

                    DialogEventContext ctx = createEventContext(params);
                    Hyspeech.get().dialogEvents()
                            .dispatch(key, new ChoiceSelectedEvent(ctx, asset, i,  asset.entries[i]));

                    handleEntry(i, asset);
                }
            }

            if (isProcessing) {
                this.rebuild();
            }
        }

        if(needsUpdate)
            this.sendUpdate();
    }

    private void handleEntry(int index, HyspeechDialogAsset asset) {
        if (asset.entries.length <= index)
            return;

        isProcessing = true;

        HyspeechMacroAsset macro = asset.entries[index].getMacro();
        executeMacro(macro);

        setKey(asset.entries[index].getNext());
    }

    private void executeMacro(@Nullable HyspeechMacroAsset macro) {
        if (macro == null)
            return;

        ParameterContext ctx = new ParameterContext();
        ctx.put(PlayerRef.class, playerRef);
        ctx.put(Hyspeech.class, Hyspeech.get());

        ArrayDeque<String> commands = Arrays.stream(macro.getCommands())
                .map(cmd -> Hyspeech.get().process(cmd, ctx))
                .collect(Collectors.toCollection(ArrayDeque::new));

        HytaleServer.get()
                .getCommandManager()
                .handleCommands(ConsoleSender.INSTANCE, commands);
    }

    public HyspeechDialogAsset getAsset() {
        if (STORE == null)
            return null;

        return STORE.getAssetMap().getAsset(key);
    }

    public void setKey(String key) {
        this.key = key;
    }
}
