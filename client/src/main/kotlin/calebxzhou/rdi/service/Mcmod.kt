package calebxzhou.rdi.service

import calebxzhou.rdi.integrate.jei.RJeiPlugin
import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.ui2.frag.alertOk
import calebxzhou.rdi.util.*
import kotlinx.coroutines.launch
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.ingredients.ITypedIngredient
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.jsoup.Jsoup
import java.util.*
import kotlin.jvm.optionals.getOrNull

object Mcmod {

    const val SERVER_PAGE = "https://play.mcmod.cn/sv20188037.html"
    const val searchUrl = "https://search.mcmod.cn/s?key="
    val headers
        get() =
            """  
Host: search.mcmod.cn
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
Accept-Language: zh-CN,zh;q=0.5
Accept-Encoding: 1
Connection: keep-alive
Cookie: MCMOD_SEED=${
                (1..26)
                    .map { ('a'..'z') + ('0'..'9').random() } // Pick a random character from the allowed set
                    .joinToString("")
            }; search_history_list=
Upgrade-Insecure-Requests: 1
Sec-Fetch-Dest: document
Sec-Fetch-Mode: navigate
Sec-Fetch-Site: none
Sec-Fetch-User: ?1
Priority: u=0, i
        """.trimIndent().split("\n").map { it.split(": ") }.map { it[0] to it[1] }


    suspend fun search(id: ResourceLocation, name: String): String? {
        val resp =
            httpRequest(url = searchUrl + "${id.namespace} $name".urlEncoded, headers = headers).body
        val resultItems = Jsoup.parse(resp).select(".result-item")
        lgr.info("搜到了${resultItems.size}个")
        // Variable to store the first href
        var firstHref: String? = resultItems.firstOrNull()?.select(".head a")?.attr("href")

        for (item in resultItems) {
            // Extract the registration name
            val registrationNameElement = item.select(".body p").first()
            val registrationName = registrationNameElement?.text()?.trim()?.substringAfter("注册名: ")
            lgr.info("注册名 $registrationName ")
            // Check if the registration name matches the target
            if (registrationName == id.toString()) {
                // Extract the href attribute from the <a> tag in the head section
                val href = item.select(".head a").attr("href")
                lgr.info("找到了更合适的 $id $href")
                return href
            }
        }
        return firstHref
    }

    fun getServerInfo() = ioScope.launch{
        httpRequest(false, SERVER_PAGE, headers = headers)
    }
    fun searchItemOpen(item: Item){
        ioScope.launch {
            search(item.id, item.chineseName.string)?.openAsUri()
                ?: search(item.id, item.englishName.string)?.openAsUri()
                ?: uiThread {  alertOk("没有在mc百科搜到${item.chineseName.string}") }
        }
    }
    fun onKeyPressIngame(){
        //先打开所看的 再打开所持的
        mc.player?.lookingAtBlock?.let { searchItemOpen(it.block.asItem()) }?:mc.player?.mainHandItem?.item?.let { searchItemOpen(it.asItem()) }
    }
    fun onKeyPressGui() {
        RJeiPlugin.jeiRuntime?.let { jei ->
            mc.screen?.let { screen ->
                val  screenItems :List<Optional<ItemStack>> = jei.screenHelper
                    .getClickableIngredientUnderMouse(screen, MouseX.toDouble(), MouseY.toDouble())
                    .map{it.typedIngredient}
                    .map(ITypedIngredient<*>::getItemStack).toList();
                val itemStack = jei.recipesGui.getIngredientUnderMouse(VanillaTypes.ITEM_STACK).getOrNull()?:
                jei.bookmarkOverlay.getIngredientUnderMouse(VanillaTypes.ITEM_STACK)?:
                    jei.ingredientListOverlay.getIngredientUnderMouse(VanillaTypes.ITEM_STACK)?: screenItems.firstOrNull()?.getOrNull()
                itemStack?.item?.let { item->

                    searchItemOpen(item)

                }
                /*.findFirst().getOrNull()?.let {
                val ist: ItemStack = it.ingredientType.castIngredient(VanillaTypes.ITEM_STACK)

            } */
            }
        }
    }
}