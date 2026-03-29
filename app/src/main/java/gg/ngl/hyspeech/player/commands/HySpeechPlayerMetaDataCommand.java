package gg.ngl.hyspeech.player.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import gg.ngl.hyspeech.Hyspeech;
import gg.ngl.hyspeech.player.HyspeechPlayer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

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

public class HySpeechPlayerMetaDataCommand extends AbstractCommand {

    private final RequiredArg<PlayerRef> playerArg;
    private final RequiredArg<String> modeArg;
    private final RequiredArg<String> metaData;

    public HySpeechPlayerMetaDataCommand() {
        super("metadata", "Command for developer use only!");
        playerArg = withRequiredArg("player", "Username of the player who should open dialog.", ArgTypes.PLAYER_REF);
        modeArg = withRequiredArg("mode", "Add or remove metadata.", ArgTypes.STRING);
        metaData = withRequiredArg("meta", "The metadata to add or remove.", ArgTypes.STRING);
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext commandContext) {
        String mode = commandContext.get(modeArg);
        String meta = commandContext.get(metaData);
        PlayerRef playerRef1 = playerArg.get(commandContext);

        HyspeechPlayer hPlayer = Hyspeech.hyspeechPlayerMap.get(playerRef1);
        if (hPlayer == null) {
            commandContext.sender().sendMessage(Message.raw("Player not found or not loaded in Hyspeech."));
            return CompletableFuture.completedFuture(null);
        }

        if(mode.equalsIgnoreCase("add")) {
            hPlayer.getConfig().get().addMetaData(meta.replace("\"", "").trim());
        } else if(mode.equalsIgnoreCase("remove")) {
            hPlayer.getConfig().get().removeMetaData(meta.replace("\"", "").trim());
        } else {
            commandContext.sender().sendMessage(Message.raw("Invalid mode. Use 'add' or 'remove'."));
            return CompletableFuture.completedFuture(null);
        }
        hPlayer.getConfig().save();
        return CompletableFuture.completedFuture(null);
    }
}
