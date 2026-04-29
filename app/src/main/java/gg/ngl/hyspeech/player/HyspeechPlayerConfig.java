package gg.ngl.hyspeech.player;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.UUID;

public class HyspeechPlayerConfig {

    public static final BuilderCodec<HyspeechPlayerConfig> CODEC =
            BuilderCodec
                    .builder(
                            HyspeechPlayerConfig.class,
                            HyspeechPlayerConfig::new
                    )
                    .append(new KeyedCodec<>("UUID", Codec.STRING),
                            (config, val) -> config.setUuid(UUID.fromString(val)),
                            config -> config.playerUuid.toString())
                    .add()
                    .append(new KeyedCodec<>("MetaData", Codec.STRING_ARRAY),
                            (config, val) -> config.setMetaData(val),
                            config -> config.metaData
                    )
                    .add()
                    .build();

    public UUID playerUuid;
    public String[] metaData = new String[0];

    public void setUuid(UUID uuid) {
        this.playerUuid = uuid;
    }

    public void setMetaData(String[] metaData) {
        this.metaData = metaData == null ? new String[0] : metaData;
    }

    public void addMetaData(String metaData) {
        if (this.metaData == null) {
            this.metaData = new String[0];
        }

        String[] newMetaData = new String[this.metaData.length + 1];
        System.arraycopy(this.metaData, 0, newMetaData, 0, this.metaData.length);
        newMetaData[newMetaData.length - 1] = metaData;
        this.metaData = newMetaData;
    }

    public void removeMetaData(String metaData) {
        if (this.metaData == null || this.metaData.length == 0) {
            this.metaData = new String[0];
            return;
        }

        int kept = 0;
        for (String data : this.metaData) {
            if (data == null ? metaData != null : !data.equals(metaData)) {
                kept++;
            }
        }

        String[] newMetaData = new String[kept];
        int index = 0;
        for (String data : this.metaData) {
            if (data == null ? metaData != null : !data.equals(metaData)) {
                newMetaData[index++] = data;
            }
        }

        this.metaData = newMetaData;
    }

    public void clearMetaData() {
        this.metaData = new String[0];
    }

    public boolean hasMetaData(String metaData) {
        if (this.metaData == null || this.metaData.length == 0) {
            return false;
        }

        for (String data : this.metaData) {
            if (data != null && data.equals(metaData)) {
                return true;
            }
        }
        return false;
    }
}
