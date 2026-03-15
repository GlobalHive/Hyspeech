package gg.ngl.hyspeech;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.npc.NPCPlugin;
import gg.ngl.hyspeech.asset.dialog.HyspeechDialogAsset;
import gg.ngl.hyspeech.asset.dialog.action.builder.BuilderActionBeginDialog;
import gg.ngl.hyspeech.asset.dialog.event.DialogEventBus;
import gg.ngl.hyspeech.asset.dialog.event.DialogInputReceivedEvent;
import gg.ngl.hyspeech.asset.macro.HyspeechMacroAsset;
import gg.ngl.hyspeech.demo.DemoClass;
import gg.ngl.hyspeech.player.HyspeechPlayer;
import gg.ngl.hyspeech.player.HyspeechPlayerConfig;
import gg.ngl.hyspeech.player.commands.HyspeechCommand;
import gg.ngl.hyspeech.player.commands.HyspeechDemoCommand;
import gg.ngl.hyspeech.util.param.ParameterContext;
import gg.ngl.hyspeech.util.param.ParameterContextContributor;
import gg.ngl.hyspeech.util.param.ParameterProcessor;
import gg.ngl.hyspeech.util.param.ParameterResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

public class Hyspeech extends JavaPlugin {

    public static final Map<PlayerRef, HyspeechPlayer> hyspeechPlayerMap = new ConcurrentHashMap<>();
    private static Hyspeech INSTANCE;
    private final Map<String, ParameterProcessor<?>> processors = new ConcurrentHashMap<>();
    private final Config<HyspeechConfig> config;

    private final List<ParameterContextContributor> contributors = new ArrayList<>();

    private final DialogEventBus dialogEvents = new DialogEventBus();

    public DialogEventBus dialogEvents() {
        return dialogEvents;
    }

    public Hyspeech(JavaPluginInit init) {
        super(init);
        Hyspeech.INSTANCE = this;
        config = withConfig(HyspeechConfig.CODEC);
    }

    public static Hyspeech get() {
        return Hyspeech.INSTANCE;
    }

    public void registerContextContributor(ParameterContextContributor contributor) {
        contributors.add(contributor);
    }

    public void populateContext(ParameterContext ctx) {
        for (ParameterContextContributor contributor : contributors) {
            contributor.contribute(ctx);
        }
    }

    public <C> void registerParameter(
            String key,
            Class<C> contextType,
            ParameterResolver<C> resolver
    ) {
        processors.put(key, new ParameterProcessor<>(key, contextType, resolver));
    }

    public ParameterProcessor<?> getProcessor(String key) {
        return processors.get(key);
    }

    public String process(String message, ParameterContext ctx) {
        for (ParameterProcessor<?> processor : processors.values()) {
            if (processor.supports(ctx)) {
                message = message.replace(
                        processor.key(),
                        processor.resolve(ctx)
                );
            }
        }
        return message;
    }

    public Config<HyspeechConfig> getConfig() {
        return config;
    }

    @Override
    public void setup() {
        Hyspeech.get().registerParameter("{username}", PlayerRef.class, PlayerRef::getUsername);
        Hyspeech.get().registerParameter("{uuid}", PlayerRef.class, p -> p.getUuid().toString());
        Hyspeech.get().registerParameter("{lang}", PlayerRef.class, PlayerRef::getLanguage);

        /*
         * Start of demo-code. For use by developers. This is an example on how to use their own custom data.
         */

        Hyspeech.get().registerParameter(
                "{mydata}",
                DemoClass.class,
                DemoClass::getCustomData
        );

        Hyspeech.get().registerContextContributor(ctx -> {
            ctx.put(DemoClass.class, new DemoClass());
        });

        Hyspeech.get().dialogEvents().register(
                "IntroDialog01",
                DialogInputReceivedEvent.class,
                event -> {
                    PlayerRef player = event.context().player();
                    System.out.println("[" + player.getUsername() + "] " + event.input());
                }
        );

        /*
         * End of demo-code.
         */

        this.getCommandRegistry().registerCommand(new HyspeechCommand());
        this.getCommandRegistry().registerCommand(new HyspeechDemoCommand());

        NPCPlugin.get().registerCoreComponentType("HyspeechBeginDialog", BuilderActionBeginDialog::new);

        registerAssetTypes();
        registerEvents();
    }

    public void registerAssetTypes() {
        HytaleAssetStore.Builder<String, HyspeechDialogAsset, DefaultAssetMap<String, HyspeechDialogAsset>> dialogAssetBuilder =
                HytaleAssetStore.builder(
                        HyspeechDialogAsset.class,
                        new DefaultAssetMap<>()
                );

        HytaleAssetStore.Builder<String, HyspeechMacroAsset, DefaultAssetMap<String, HyspeechMacroAsset>> macroAssetBuilder =
                HytaleAssetStore.builder(
                        HyspeechMacroAsset.class,
                        new DefaultAssetMap<>()
                );

        this.getAssetRegistry().register(
                macroAssetBuilder
                        .setPath("HyspeechMacro")
                        .setCodec(HyspeechMacroAsset.CODEC)
                        .setKeyFunction(HyspeechMacroAsset::getId)
                        .loadsAfter(Interaction.class)
                        .build()
        );

        this.getAssetRegistry().register(
                dialogAssetBuilder
                        .setPath("HyspeechDialog")
                        .setCodec(HyspeechDialogAsset.CODEC)
                        .setKeyFunction(HyspeechDialogAsset::getId)
                        .loadsAfter(Interaction.class)
                        .build()
        );
    }

    public void registerEvents() {
        this.getEventRegistry().register(
                PlayerConnectEvent.class,
                playerConnectEvent ->
                        hyspeechPlayerMap.putIfAbsent(
                                playerConnectEvent.getPlayerRef(),
                                new HyspeechPlayer(playerConnectEvent.getPlayerRef())
                        )
        );

        this.getEventRegistry().register(
                PlayerDisconnectEvent.class,
                playerDisconnectEvent -> {
                    HyspeechPlayer player = hyspeechPlayerMap.get(playerDisconnectEvent.getPlayerRef());
                    Config<HyspeechPlayerConfig> cfg = new Config<>(
                            new File("config/hyspeech/player_data/").toPath(),
                            playerDisconnectEvent.getPlayerRef().getUsername(),
                            HyspeechPlayerConfig.CODEC
                    );

                    cfg.load().thenAccept((_cfg) -> {
                        _cfg.setUuid(player.getConfig().get().playerUuid);
                        _cfg.setMetaData(player.getConfig().get().metaData);
                    }).thenAccept((_) -> {
                        cfg.save().thenAccept((_) -> {
                            hyspeechPlayerMap.remove(playerDisconnectEvent.getPlayerRef());
                        });
                    });
                }
        );
    }

    @Override
    public void start() {
        enable();
    }

    @Override
    public void shutdown() {
        getConfig().save();
    }

    public void enable() {
        getConfig().save();
    }

    public void disable() {
    }
}
