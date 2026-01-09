package calebxzhou.rdi.mc.common;

import java.util.UUID;

/**
 * calebxzhou @ 2026-01-06 19:34
 */
public class RDI {
    public static final String IHQ_URL;
    public static final String GAME_IP;
    public static final String HOST_NAME;
    public static int HOST_PORT;

    static {
        String ihqUrl = System.getProperty("rdi.ihq.url");
        if (ihqUrl == null) {
            throw new IllegalArgumentException("启动方式错误：找不到服务器地址1");
        }
        IHQ_URL = ihqUrl;

        String gameIp = System.getProperty("rdi.game.ip");
        if (gameIp == null) {
            throw new IllegalArgumentException("启动方式错误：找不到服务器地址2");
        }
        GAME_IP = gameIp;

        String hostName = System.getProperty("rdi.host.name");
        if (hostName == null) {
            throw new IllegalArgumentException("启动方式错误：找不到主机名");
        }
        HOST_NAME = hostName;

        String hostPortStr = System.getProperty("rdi.host.port");
        if (hostPortStr == null) {
            throw new IllegalArgumentException("启动方式错误：找不到服务器端口");
        }
        try {
            HOST_PORT = Integer.parseInt(hostPortStr);
        } catch (NumberFormatException e) {
            // Matches Kotlin's String.toInt() behavior – throws if not a valid integer
            throw new NumberFormatException("Invalid port value: " + hostPortStr);
        }
    }
    public static String getTextureQueryUrl(UUID profileId,String authlibVer){
        return IHQ_URL+"/mc-profile/"+profileId+"/clothes?authlibVer="+authlibVer;
    }
}
