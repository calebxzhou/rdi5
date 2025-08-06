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

import static net.minecraft.client.multiplayer.ProfileKeyPairManager.EMPTY_KEY_MANAGER;

@Mixin(MinecraftServer.class)
public class mAlwaysOfflineMode {
    /**
     * @author calebxzhou
     * @reason 关闭验证
     */
    @Overwrite
    public boolean usesAuthentication() {
        return false;
    }
}
@Mixin(Minecraft.class)
class mFastCreateMojangService {
    /**
     * @author calebxzhou
     * @reason 永远离线
     */
    @Overwrite
    private UserApiService createUserApiService(YggdrasilAuthenticationService yggdrasilAuthenticationService, GameConfig gameConfig) {
        return UserApiService.OFFLINE;
    }
}
@Mixin(ProfileKeyPairManager.class)
interface mFastCreateMojangService2{
    /**
     * @author calebxzhou
     * @reason 永远离线
     */
    @Overwrite
    public static ProfileKeyPairManager create(UserApiService userApiService, User user, Path gameDirectory) {
        return (EMPTY_KEY_MANAGER);
    }
}