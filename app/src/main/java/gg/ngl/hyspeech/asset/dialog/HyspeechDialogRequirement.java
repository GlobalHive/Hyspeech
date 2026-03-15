package gg.ngl.hyspeech.asset.dialog;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

/**
 * A single dialog requirement evaluated before opening a dialog.
 */
public class HyspeechDialogRequirement {

    public static final BuilderCodec<HyspeechDialogRequirement> CODEC =
            BuilderCodec
                    .builder(HyspeechDialogRequirement.class, HyspeechDialogRequirement::new)
                    .append(
                            new KeyedCodec<>("Item", Codec.STRING),
                            (obj, val) -> obj.itemId = val,
                            obj -> obj.itemId
                    )
                    .addValidator(Item.VALIDATOR_CACHE.getValidator())
                    .documentation("Required item id.")
                    .add()
                    .append(
                            new KeyedCodec<>("Amount", Codec.INTEGER),
                            (obj, val) -> obj.amount = val,
                            obj -> obj.amount
                    )
                    .documentation("Required amount of the item.")
                    .add()
                    .append(
                        new KeyedCodec<>("TaskId", Codec.STRING),
                        (obj, val) -> obj.taskId = val,
                        obj -> obj.taskId
                    )
                    .documentation("Required task id that must exist for this player.")
                    .add()
                    .append(
                        new KeyedCodec<>("MetaData", Codec.STRING),
                        (obj, val) -> obj.metaData = val,
                        obj -> obj.metaData
                    )
                    .documentation("Required metadata that must exist for this player.")
                    .add()
                    .build();

    public String itemId;
    public int amount = 1;
    public String taskId;
    public String metaData;

    protected HyspeechDialogRequirement() {
    }

    public String getItemId() {
        return this.itemId;
    }

    public int getAmount() {
        return this.amount;
    }

    public String getTaskId() {
        return this.taskId;
    }

    public String getMetaData() {
        return this.metaData;
    }
}
