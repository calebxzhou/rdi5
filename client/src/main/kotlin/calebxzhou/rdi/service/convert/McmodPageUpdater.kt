package calebxzhou.rdi.service.convert

import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.service.ModService
import calebxzhou.rdi.util.json
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.net.URI
import kotlin.math.max

/**
 * calebxzhou @ 2025-10-14 14:40
 * 从mc百科获取mod的中文名称 简介 图标等信息
 */
fun main() {
    Configurator.setRootLevel(Level.DEBUG)
    //1.21.1 neoforge
    val baseUrl = "https://www.mcmod.cn/modlist.html?mcver=1.21.1&platform=1&api=13&sort=createtime"
    runBlocking {
        val mods = fetchAllPages(baseUrl)
        lgr.info("抓取完成：共 ${mods.size} 个模组")
        File("mcmod_mod_data.json").writeText(mods.json)
    }
}

private suspend fun fetchAllPages(baseUrl: String): List<McmodModBriefInfo> {
    val results = arrayListOf<McmodModBriefInfo>()

    val firstDoc = loadPageDocument(buildPageUrl(baseUrl, null)) ?: return results
    val firstPageInfo = parsePageInfo(firstDoc)
    val startPage = firstPageInfo.currentPage
    val totalPages = max(firstPageInfo.totalPages, startPage)
    val expectedTotalItems = firstPageInfo.totalItems

    lgr.info("当前 ${firstPageInfo.currentPage} / ${totalPages} 页，共计 ${expectedTotalItems} 条")

    results += extractModInfos(firstDoc)

    var page = max(startPage, 1)

    while (page < totalPages) {
        page += 1
        val pageUrl = buildPageUrl(baseUrl, page)
        val document = loadPageDocument(pageUrl) ?: break
        val pageInfo = parsePageInfo(document)
        lgr.info("当前 ${pageInfo.currentPage} / ${totalPages} 页，共计 ${expectedTotalItems} 条")
        results += extractModInfos(document)
        delay(500)
    }

    lgr.info("合计收集 ${results.size} / ${expectedTotalItems} 条")
    return results
}

private fun buildPageUrl(baseUrl: String, page: Int?): String {
    return when {
        page == null || page <= 1 -> baseUrl
        baseUrl.contains("page=", ignoreCase = true) -> {
            baseUrl.replace(Regex("([?&])page=\\d+")) { match ->
                "${match.groupValues[1]}page=$page"
            }
        }
        baseUrl.contains('?') -> "$baseUrl&page=$page"
        else -> "$baseUrl?page=$page"
    }
}

private suspend fun loadPageDocument(url: String): Document? {
    val headers = headersForUrl(url)
    val response = calebxzhou.rdi.net.httpRequest {
        url(url)
        method = io.ktor.http.HttpMethod.Get
        headers.forEach { (name, value) -> header(name, value) }
    }
    if (response.status.value !in 200..299) {
        lgr.warn("请求mcmod列表页失败: HTTP ${response.status.value} url=$url")
        return null
    }
    val body = response.bodyAsText()
    if (body.isBlank()) {
        lgr.warn("mcmod列表页返回空内容，url=$url")
        return null
    }
    return Jsoup.parse(body, url)
}

private fun headersForUrl(url: String): List<Pair<String, String>> {
    val host = runCatching { URI(url).host }.getOrNull()
    return if (host.isNullOrBlank()) {
        ModService.mcmodHeader
    } else {
        ModService.mcmodHeader.map { (key, value) ->
            if (key.equals("Host", ignoreCase = true)) key to host else key to value
        }
    }
}

private data class PageInfo(
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int
)

private val PAGE_INFO_REGEX = Regex("当前\\s*(\\d+)\\s*/\\s*(\\d+)\\s*页[^0-9]*?(\\d+)\\s*条")

private fun parsePageInfo(document: Document): PageInfo {
    val candidateText = sequence {
        yieldAll(document.select(".page-info, .common-pagination, .pagination, .page").map(Element::text))
        yield(document.body().text())
    }.firstNotNullOfOrNull { text ->
        PAGE_INFO_REGEX.find(text)?.let { match ->
            PageInfo(
                currentPage = match.groupValues[1].toInt(),
                totalPages = match.groupValues[2].toInt(),
                totalItems = match.groupValues[3].toInt()
            )
        }
    }

    return candidateText ?: throw IllegalStateException("无法解析分页信息")
}

private val CLASS_ID_REGEX = Regex("/class/(\\d+).html")

private fun extractModInfos(document: Document): List<  McmodModBriefInfo> {
    val result = arrayListOf< McmodModBriefInfo>()

    val blocks = document.select(".modlist-block")
    if (blocks.isEmpty()) {
        lgr.warn("该页面没有找到modlist-block")
    }

    for (block in blocks) {
        val link = block.selectFirst("a[href^=/class/]") ?: continue
        val href = link.attr("href").trim()
        val pageId = CLASS_ID_REGEX.find(href)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (pageId == null) {
            lgr.warn("无法从链接解析pageId: $href")
            continue
        }

        val logoUrl = block.selectFirst("img")?.let { img ->
            val dataOriginal = img.absUrl("data-original")
            val dataSrc = img.absUrl("data-src")
            val src = img.absUrl("src")
            sequenceOf(dataOriginal, dataSrc, src)
                .firstOrNull { it.isNotBlank() }
                ?: src
        }.orEmpty()

        val intro = block.selectFirst(".intro-content")?.text()?.trim().orEmpty()

        val rawNameCn = block.selectFirst(".title > .name")?.text()?.trim().orEmpty()
        val rawNameEn = block.selectFirst(".title > .ename")?.text()?.trim().orEmpty()

        val name: String
        val nameCn: String?

        if (rawNameEn.isNotBlank()) {
            name = rawNameEn
            nameCn = rawNameCn.ifBlank { null }
        } else {
            name = rawNameCn.ifBlank { "class-$pageId" }
            nameCn = null
        }

        val info = McmodModBriefInfo(
            id = pageId,
            logoUrl = logoUrl,
            name = name,
            nameCn = nameCn,
            intro = intro
        )
        lgr.info("$name $nameCn $pageId")
        result+= info
    }

    lgr.info("页面解析完成，共 ${result.size} 个mod")
    return result
}