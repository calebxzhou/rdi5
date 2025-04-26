package calebxzhou.rdi.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.file.Path;

/**
 * calebxzhou @ 2025-04-16 11:42
 */
@Mixin(MinecraftServer.class)
public class mOfflineMode {
    @Overwrite
    public boolean usesAuthentication() {
        return false;
    }
}
@Mixin(ProfileKeyPairManager.class)
interface mOfflineMode2{
    @Overwrite
    static ProfileKeyPairManager create(UserApiService userApiService, User user, Path gameDirectory) {
        return (ProfileKeyPairManager.EMPTY_KEY_MANAGER);
    }
}
@Mixin(Minecraft.class)
class mOfflineMode3{
    @Overwrite
    private UserApiService createUserApiService(YggdrasilAuthenticationService authenticationService, GameConfig gameConfig) {
        return UserApiService.OFFLINE;
    }
}