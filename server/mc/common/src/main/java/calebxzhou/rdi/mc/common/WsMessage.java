package calebxzhou.rdi.mc.common;

/**
 * calebxzhou @ 2026-01-12 17:57
 */

public final class WsMessage  {
    private final int id;
    private final Channel channel;
    private final String data;

    public WsMessage(int id, Channel channel, String data) {
        this.id = id;
        this.channel = channel;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getData() {
        return data;
    }

    public enum Channel {
        Command,
        Response
    }
}
