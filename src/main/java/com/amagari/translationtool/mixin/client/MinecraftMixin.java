package com.amagari.translationtool.mixin.client;

import com.amagari.translationtool.client.WorldLanguageClient;
import com.amagari.translationtool.client.WorldLanguageContext;
import com.amagari.translationtool.client.paratranz.ParaTranzContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "doWorldLoad", at = @At("TAIL"))
	private void amagari_translation_tool$setWorldLanguageDirectory(
			LevelStorageSource.LevelStorageAccess levelStorageAccess,
			PackRepository packRepository,
			WorldStem worldStem,
			boolean safeMode,
			CallbackInfo callbackInfo
	) {
		WorldLanguageContext.enterWorld(levelStorageAccess.getLevelPath(LevelResource.ROOT));
		WorldLanguageClient.reloadLanguage((Minecraft) (Object) this);
	}

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
	private void amagari_translation_tool$clearWorldLanguageDirectory(Screen screen, boolean transferring, CallbackInfo callbackInfo) {
		WorldLanguageContext.leaveWorld();
		ParaTranzContext.resetSessionState();
	}
}
