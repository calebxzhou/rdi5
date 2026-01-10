package calebxzhou.rdi.mc.client.mixin;

import com.mojang.brigadier.StringReader;
import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2025-08-05 21:33
 */
@Mixin(StringReader.class)
public class mAllowChineseArgument {
    @Overwrite
    public static boolean isAllowedInUnquotedString(final char c) {
        return c >= '0' && c <= '9'
                || c >= 'A' && c <= 'Z'
                || c >= 'a' && c <= 'z'
                || c == '_' || c == '-'
                || c == '.' || c == '+'
                // Add support for Chinese characters (CJK Unified Ideographs)
                || (c >= '\u4E00' && c <= '\u9FFF')  // Basic CJK Unified Ideographs
                || (c >= '\u3400' && c <= '\u4DBF')  // CJK Unified Ideographs Extension A
                || (c >= '\uF900' && c <= '\uFAFF'); // CJK Compatibility Ideographs
    }
}
@Mixin(StringUtil.class)
class mAllowChineseName{
    @Overwrite
    public static boolean isValidPlayerName(String playerName) {
        return true;
    }
}