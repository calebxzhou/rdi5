package calebxzhou.rdi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModrinthProject(
    val id: String,
    val slug: String,
    val title: String,
    val description: String? = null,
    val categories: List<String> = emptyList(),
    @SerialName("client_side") val clientSide: String? = null,
    @SerialName("server_side") val serverSide: String? = null,
    val body: String? = null,
    val status: String? = null,
    @SerialName("requested_status") val requestedStatus: String? = null,
    @SerialName("additional_categories") val additionalCategories: List<String> = emptyList(),
    @SerialName("issues_url") val issuesUrl: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("wiki_url") val wikiUrl: String? = null,
    @SerialName("discord_url") val discordUrl: String? = null,
    @SerialName("donation_urls") val donationUrls: List<ModrinthDonationLink> = emptyList(),
    @SerialName("project_type") val projectType: String,
    val downloads: Long = 0,
    @SerialName("icon_url") val iconUrl: String? = null,
    val color: Int? = null,
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("monetization_status") val monetizationStatus: String? = null,
    val team: String,
    @SerialName("body_url") val bodyUrl: String? = null,
    @SerialName("moderator_message") val moderatorMessage: ModrinthModeratorMessage? = null,
    val published: String,
    val updated: String,
    val approved: String? = null,
    val queued: String? = null,
    val followers: Long = 0,
    val license: ModrinthLicense? = null,
    val versions: List<String> = emptyList(),
    @SerialName("game_versions") val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val gallery: List<ModrinthGalleryItem> = emptyList()
)

@Serializable
data class ModrinthLicense(
    val id: String? = null,
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class ModrinthModeratorMessage(
    val message: String? = null,
    val body: String? = null
)

@Serializable
data class ModrinthDonationLink(
    val id: String,
    val platform: String,
    val url: String
)

@Serializable
data class ModrinthGalleryItem(
    val url: String? = null,
    val title: String? = null,
    val description: String? = null,
    val created: String? = null,
    val featured: Boolean? = null,
    val ordering: Int? = null
)
