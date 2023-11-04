package com.skriptlang.scroll.commands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.skriptlang.scroll.Scroll;
import com.skriptlang.scroll.commands.ScriptCommand.ScrollCommandContext;
import com.skriptlang.scroll.commands.arguments.CommandParameter;
import com.skriptlang.scroll.language.Languaged;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

public class ClientCommandInitalizer implements CommandRegistrar, Languaged {

	static {
		try {
			CommandManager.setClientCommandInitalizer(new ClientCommandInitalizer());
		} catch (IllegalAccessException e) {
			assert false;
		}
	}

	@Override
	public void register(Command command) {
		com.mojang.brigadier.Command<FabricClientCommandSource> execute = new com.mojang.brigadier.Command<>() {
			@Override
			public int run(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
				ScrollCommandContext<FabricClientCommandSource> commandContext = new ScriptCommand.ScrollCommandContext<FabricClientCommandSource>(context);
				command.fill(commandContext);
				// We still want to call the command context even if the command will be cancelled represented by a negative number. ScriptCommand handles not calling the trigger.
				ScriptCommand.runTriggers(ScriptCommand.getTriggersList(), commandContext);
				return commandContext.getReturnCode();
			}
		};
		LiteralArgumentBuilder<FabricClientCommandSource> mainNode = literal(command.getName());
		if (!command.getParameters().isEmpty()) {
			for (CommandParameter<?> argument : command.getParameters()) {
				mainNode.then(argument(argument.getIdentifier(), TextArgumentType.text()));
			}
		}
		mainNode.executes(execute);
		ClientCommandManager.getActiveDispatcher().register(mainNode);
	}

	@Override
	public void unregister(Command command) {
		remover.removeCommand(command, ClientCommandManager.getActiveDispatcher().getRoot());
		PlayerManager playerManager = Scroll.getMinecraftServer().getPlayerManager();
		for (ServerPlayerEntity player : playerManager.getPlayerList())
			playerManager.sendCommandTree(player);
	}

}
