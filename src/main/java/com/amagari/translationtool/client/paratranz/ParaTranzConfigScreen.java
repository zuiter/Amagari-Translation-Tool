package com.amagari.translationtool.client.paratranz;

import com.amagari.translationtool.translation.WorldLanguageMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.io.IOException;
import java.nio.file.Path;

public class ParaTranzConfigScreen extends Screen {
	private static final int FIELD_HEIGHT = 20;
	private static final int FIELD_GAP = 40;
	private static final int LABEL_COLOR = 0xFFA0A0A0;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int ERROR_COLOR = 0xFFFF7070;
	private static final int MAX_TOKEN_LENGTH = 512;
	private static final int MAX_LANGUAGE_LENGTH = 32;
	private static final int BUTTON_WIDTH = 112;
	private static final int BUTTON_GAP = 12;

	private final Screen parent;
	private final Path gameDirectory;
	private final String languageCode;
	private final boolean hasToken;
	private EditBox token;
	private EditBox sourceLanguage;
	private EditBox targetLanguage;
	private EditBox maxCachedArtifacts;
	private Checkbox clearToken;
	private Checkbox triggerExport;
	private Checkbox overwriteWorldLanguageFiles;
	private String status = "";
	private int formLeft;

	public ParaTranzConfigScreen(Screen parent, Minecraft client) {
		this(parent, client.gameDirectory.toPath(), client.getLanguageManager().getSelected(), hasToken(client.gameDirectory.toPath()));
	}

	private ParaTranzConfigScreen(Screen parent, Path gameDirectory, String languageCode, boolean hasToken) {
		super(Component.literal(WorldLanguageMessages.paraConfigTitle(languageCode)));
		this.parent = parent;
		this.gameDirectory = gameDirectory;
		this.languageCode = languageCode;
		this.hasToken = hasToken;
	}

	@Override
	protected void init() {
		formLeft = Math.max(24, (width - 360) / 2);
		int fieldWidth = Math.min(360, width - 48);
		int top = Math.max(56, (height - 236) / 2);
		ParaTranzConfig current = loadConfig();

		token = new EditBox(font, formLeft, top, fieldWidth, FIELD_HEIGHT, Component.literal(WorldLanguageMessages.paraConfigTokenHint(hasToken, languageCode)));
		token.setMaxLength(MAX_TOKEN_LENGTH);
		token.setHint(Component.literal(WorldLanguageMessages.paraConfigTokenHint(hasToken, languageCode)));
		token.addFormatter((value, cursor) -> FormattedCharSequence.forward("*".repeat(value.length()), Style.EMPTY));
		addRenderableWidget(token);

		int halfWidth = fieldWidth / 2 - 6;
		int rightColumnX = formLeft + halfWidth + 12;
		sourceLanguage = addField(top + FIELD_GAP, halfWidth, MAX_LANGUAGE_LENGTH, ParaTranzConfig.DEFAULT_SOURCE_LANGUAGE, current.sourceLanguage());
		targetLanguage = addField(top + FIELD_GAP, halfWidth, MAX_LANGUAGE_LENGTH, ParaTranzConfig.DEFAULT_TARGET_LANGUAGE, current.targetLanguage());
		targetLanguage.setX(rightColumnX);
		maxCachedArtifacts = addField(top + FIELD_GAP * 2, 112, 4, Integer.toString(ParaTranzConfig.DEFAULT_MAX_CACHED_ARTIFACTS), Integer.toString(current.maxCachedArtifacts()));

		clearToken = addRenderableWidget(Checkbox.builder(Component.literal(WorldLanguageMessages.paraConfigClearTokenLabel(languageCode)), font)
				.pos(rightColumnX, top + FIELD_GAP * 2)
				.selected(false)
				.build());
		triggerExport = addRenderableWidget(Checkbox.builder(Component.literal(WorldLanguageMessages.paraConfigTriggerExportLabel(languageCode)), font)
				.pos(formLeft, top + FIELD_GAP * 3)
				.selected(current.triggerExport())
				.build());
		overwriteWorldLanguageFiles = addRenderableWidget(Checkbox.builder(Component.literal(WorldLanguageMessages.paraConfigOverwriteWorldFilesLabel(languageCode)), font)
				.pos(rightColumnX, top + FIELD_GAP * 3)
				.selected(current.overwriteWorldLanguageFiles())
				.build());

		int buttonY = top + FIELD_GAP * 4;
		int buttonX = formLeft + (fieldWidth - BUTTON_WIDTH * 2 - BUTTON_GAP) / 2;
		addRenderableWidget(Button.builder(Component.literal(WorldLanguageMessages.paraConfigSaveLabel(languageCode)), button -> save())
				.bounds(buttonX, buttonY, BUTTON_WIDTH, 20)
				.build());
		addRenderableWidget(Button.builder(Component.literal(WorldLanguageMessages.paraConfigCancelLabel(languageCode)), button -> onClose())
				.bounds(buttonX + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, 20)
				.build());
	}

	private EditBox addField(int y, int fieldWidth, int maxLength, String hint, String value) {
		EditBox field = new EditBox(font, formLeft, y, fieldWidth, FIELD_HEIGHT, Component.literal(hint));
		field.setMaxLength(maxLength);
		field.setHint(Component.literal(hint));
		field.setValue(value);
		return addRenderableWidget(field);
	}

	private void save() {
		try {
			ParaTranzConfig current = ParaTranzConfig.load(gameDirectory);
			Integer parsedMaxCachedArtifacts = parseRequiredInt(maxCachedArtifacts.getValue());
			if (parsedMaxCachedArtifacts == null || parsedMaxCachedArtifacts <= 0) {
				status = WorldLanguageMessages.paraConfigInvalidCacheCount(languageCode);
				return;
			}

			String nextToken = token.getValue().trim();
			if (clearToken.selected()) {
				nextToken = "";
			} else if (nextToken.isBlank()) {
				nextToken = current.paratranzApiToken();
			}
			ParaTranzConfig nextConfig = new ParaTranzConfig(
					nextToken,
					sourceLanguage.getValue(),
					targetLanguage.getValue(),
					triggerExport.selected(),
					parsedMaxCachedArtifacts,
					overwriteWorldLanguageFiles.selected()
			);
			ParaTranzConfig.save(gameDirectory, nextConfig);
			ParaTranzContext.updateActiveConfig(ParaTranzConfig.load(gameDirectory));
			status = WorldLanguageMessages.paraConfigSaved(languageCode);
		} catch (IOException exception) {
			status = WorldLanguageMessages.paraConfigFailed(exception.getMessage(), languageCode);
			return;
		}
		onClose();
	}

	private static Integer parseRequiredInt(String value) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, 24, TEXT_COLOR);
		graphics.text(font, WorldLanguageMessages.paraConfigTokenLabel(hasToken, languageCode), token.getX(), token.getY() - 13, LABEL_COLOR);
		graphics.text(font, WorldLanguageMessages.paraConfigSourceLabel(languageCode), sourceLanguage.getX(), sourceLanguage.getY() - 13, LABEL_COLOR);
		graphics.text(font, WorldLanguageMessages.paraConfigTargetLabel(languageCode), targetLanguage.getX(), targetLanguage.getY() - 13, LABEL_COLOR);
		graphics.text(font, WorldLanguageMessages.paraConfigCacheCountLabel(languageCode), maxCachedArtifacts.getX(), maxCachedArtifacts.getY() - 13, LABEL_COLOR);
		if (!status.isBlank()) {
			graphics.text(font, status, formLeft, height - 24, status.equals(WorldLanguageMessages.paraConfigSaved(languageCode)) ? TEXT_COLOR : ERROR_COLOR);
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static boolean hasToken(Path gameDirectory) {
		try {
			return !ParaTranzConfig.load(gameDirectory).paratranzApiToken().isBlank();
		} catch (IOException exception) {
			return false;
		}
	}

	private ParaTranzConfig loadConfig() {
		try {
			return ParaTranzConfig.load(gameDirectory);
		} catch (IOException exception) {
			status = WorldLanguageMessages.paraConfigFailed(exception.getMessage(), languageCode);
			return ParaTranzConfig.defaultConfig();
		}
	}
}
