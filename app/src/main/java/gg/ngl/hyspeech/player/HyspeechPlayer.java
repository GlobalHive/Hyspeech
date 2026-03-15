package gg.ngl.hyspeech.player;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.Config;

import java.io.File;

public class HyspeechPlayer {
    private PlayerRef player;
    private Config<HyspeechPlayerConfig> config;

    public HyspeechPlayer(PlayerRef player) {
        this.player = player;

        config = new Config<>(
                new File("config/hyspeech/player_data/").toPath(),
                player.getUuid().toString(),
                HyspeechPlayerConfig.CODEC
        );

        getConfig().load().thenAccept((config) -> {
            if (config.playerUuid == null)
                config.setUuid(getPlayerRef().getUuid());
            if(config.metaData == null)
                config.setMetaData(new String[0]);
        }).exceptionally((throwable -> {
            throwable.printStackTrace();
            return null;
        }));

        config.save();
    }

    protected HyspeechPlayer() {
    }

    public PlayerRef getPlayerRef() {
        return this.player;
    }

    public Config<HyspeechPlayerConfig> getConfig() {
        return config;
    }

    public void setConfig(Config<HyspeechPlayerConfig> config) {
        this.config = config;
    }
}
