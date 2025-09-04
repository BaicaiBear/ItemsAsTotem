package top.bearcabbage.itemsastotem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.mojang.brigadier.arguments.StringArgumentType.StringType.SINGLE_WORD;
import static net.minecraft.server.command.CommandManager.argument;

public class ItemsAsTotem implements ModInitializer {
	public static final String MOD_ID = "items-as-totem";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static int randomMode = 1;
	private static final IConfig config = new IConfig(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID+".json"));


	@Override
	public void onInitialize() {
		load();
		PayloadTypeRegistry.playS2C().register(DeathProtectorPayload.ID, DeathProtectorPayload.CODEC);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)->{
			dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("totem")
				.then(argument("mode", StringArgumentType.string())
					.requires(source -> source.hasPermissionLevel(2))
					.suggests(ListSuggestion.of(()->java.util.List.of("inventory","unfold")))
					.executes(context -> {
						String modeStr = StringArgumentType.getString(context, "mode");
						if (modeStr.equals("inventory")) {
							randomMode = 0;
						} else if (modeStr.equals("unfold")) {
							randomMode = 1;
						} else {
							context.getSource().sendError(Text.literal("不是有效的随机方式"));
							return 0;
						}
						save();
						return 1;
					})
				)
			);
		});
	}

	public static void save(){
		config.set("RandomMode", randomMode);
		config.save();
	}

	public static void load(){
		randomMode = config.getOrDefault("RandomMode", 1);
		if (randomMode != 0 && randomMode != 1){
			randomMode = 1;
		}
		LOGGER.info("ItemsAsTotem loaded with RandomMode: " + (randomMode==0?"inventory":"unfold"));
	}

	public static final class ListSuggestion {
		private ListSuggestion() {}

		public static CompletableFuture<Suggestions> buildSuggestions(SuggestionsBuilder builder, Collection<String> suggestionCollection) {
			String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

			if (suggestionCollection.isEmpty()) { // If the list is empty then return no suggestions
				return Suggestions.empty(); // No suggestions
			}

			for (String str : suggestionCollection) { // Iterate through the supplied list
				if (str.toLowerCase(Locale.ROOT).startsWith(remaining)) {
					builder.suggest(str); // Add every single entry to suggestions list.
				}
			}
			return builder.buildFuture(); // Create the CompletableFuture containing all the suggestions
		}

		@Contract(pure = true)
		public static @NotNull SuggestionProvider<ServerCommandSource> of(Supplier<Collection<String>> suggestionCollection) {
			return (CommandContext<ServerCommandSource> context, SuggestionsBuilder builder)
					-> buildSuggestions(builder, suggestionCollection.get());
		}
	}

	private static class IConfig {
		private final Path filePath;
		private JsonObject jsonObject;
		private final Gson gson;

		public IConfig(Path filePath) {
			this.filePath = filePath;
			this.gson = new GsonBuilder().setPrettyPrinting().create();
			try {
				if (Files.notExists(filePath.getParent())) {
					Files.createDirectories(filePath.getParent());
				}
				if (Files.notExists(filePath)) {
					Files.createFile(filePath);
					try (FileWriter writer = new FileWriter(filePath.toFile())) {
						writer.write("{}");
					}
				}

			} catch (IOException e) {
				LOGGER.error(e.toString());
			}
			loadConfig();
		}

		private void loadConfig() {
			try (FileReader reader = new FileReader(filePath.toFile())) {
				this.jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			} catch (IOException e) {
				this.jsonObject = new JsonObject();
			}
		}

		public void save() {
			try (FileWriter writer = new FileWriter(filePath.toFile())) {
				gson.toJson(jsonObject, writer);
			} catch (IOException e) {
				LOGGER.error(e.toString());
			}
		}

		public void set(String key, Object value) {
			jsonObject.add(key, gson.toJsonTree(value));
		}

		public <T> T get(String key, Class<T> clazz) {
			return gson.fromJson(jsonObject.get(key), clazz);
		}

		public <T> T getOrDefault(String key, T defaultValue) {
			if (jsonObject.has(key)) {
				@SuppressWarnings("unchecked")
				Class<T> clazz = (Class<T>) defaultValue.getClass();
				return gson.fromJson(jsonObject.get(key), clazz);
			}
			else {
				set(key, defaultValue);
				save();
				return defaultValue;
			}
		}
	}

}