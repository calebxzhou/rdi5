package calebxzhou.rdi.mc.client;

import calebxzhou.rdi.mc.common.RDI;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;
import com.mojang.util.UndashedUuid;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * calebxzhou @ 2026-01-10 21:33
 */
public class RMcSessionService implements MinecraftSessionService {
    private static final Logger LGR = LoggerFactory.getLogger(RMcSessionService.class);
    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
    private final MinecraftClient client= MinecraftClient.unauthenticated(Proxy.NO_PROXY);
    @Override
    public void joinServer(UUID profileId, String authenticationToken, String serverId) throws AuthenticationException {

    }

    @Override
    public @Nullable ProfileResult hasJoinedServer(String profileName, String serverId, @Nullable InetAddress address) throws AuthenticationUnavailableException {
        return null;
    }

    @Override
    public @Nullable Property getPackedTextures(GameProfile profile) {
        return Iterables.getFirst(profile.getProperties().get("textures"), null);
    }

    @Override
    public MinecraftProfileTextures unpackTextures(Property packedTextures) {
        final String value = packedTextures.value();
        final MinecraftTexturesPayload result;
        try {
            final String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (final JsonParseException | IllegalArgumentException e) {
            LGR.error("Could not decode textures payload", e);
            return MinecraftProfileTextures.EMPTY;
        }
        if (result == null || result.textures() == null || result.textures().isEmpty()) {
            return MinecraftProfileTextures.EMPTY;
        }
        final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = result.textures();

        return new MinecraftProfileTextures(
                textures.get(MinecraftProfileTexture.Type.SKIN),
                textures.get(MinecraftProfileTexture.Type.CAPE),
                textures.get(MinecraftProfileTexture.Type.ELYTRA),
                SignatureState.SIGNED
        );
    }

    @Override
    public MinecraftProfileTextures getTextures(GameProfile profile) {
        return MinecraftSessionService.super.getTextures(profile);
    }

    @Override
    public @Nullable ProfileResult fetchProfile(UUID profileId, boolean requireSecure) {
        try {
            URL url = HttpAuthenticationService.constantURL(RDI.IHQ_URL + "/session/minecraft/profile/" + UndashedUuid.toString(profileId));
            final MinecraftProfilePropertiesResponse response = client.get(url, MinecraftProfilePropertiesResponse.class);
            if (response == null) {
                LGR.debug("NO PROFILE {} ", profileId);
                return null;
            }
            final GameProfile profile = response.toProfile();
            return new ProfileResult(profile, Set.of());
        } catch (final MinecraftClientException | IllegalArgumentException e) {
            LGR.warn("Can't get profile for {}", profileId, e);
            return null;
        }
    }

    @Override
    public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException {
        return property.value();
    }
}
