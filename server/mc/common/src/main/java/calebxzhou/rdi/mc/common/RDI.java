package calebxzhou.rdi.mc.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * calebxzhou @ 2026-01-06 23:56
 */
public class RDI {
    private static final Logger lgr = LogManager.getLogger("rdi");
    public static final String IHQ_URL;
    public static final String HOST_ID;
    public static final Boolean ALL_OP;
    static {
        String ihqUrl = System.getProperty("rdi.ihq.url");
        if (ihqUrl == null) {
            ihqUrl = "host.docker.internal:65231";
        }
        IHQ_URL = ihqUrl;

        String hostId = System.getenv("HOST_ID");
        if (hostId == null) {
            hostId = System.getProperty("rdi.host.id");
        }
        if (hostId == null) {
            throw new IllegalArgumentException("No HOST_ID provided – stopping");
        }
        HOST_ID = hostId;

        String allOpEnv = System.getenv("ALL_OP");
        if (allOpEnv == null) {
            ALL_OP = false;
        } else {
            ALL_OP = Boolean.parseBoolean(allOpEnv);
        }
    }

}
