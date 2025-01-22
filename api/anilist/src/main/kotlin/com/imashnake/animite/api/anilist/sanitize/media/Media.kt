package com.imashnake.animite.api.anilist.sanitize.media

import android.graphics.Color
import com.imashnake.animite.api.anilist.MediaQuery
import com.imashnake.animite.api.anilist.fragment.CharacterSmall
import com.imashnake.animite.api.anilist.fragment.MediaSmall
import com.imashnake.animite.api.anilist.type.MediaRankType
import androidx.core.graphics.toColorInt

private const val HQ_DEFAULT = "hqdefault"
private const val MAX_RES_DEFAULT = "maxresdefault"
private const val SD_DEFAULT = "sddefault"

data class Media(
    /** @see MediaQuery.Media.id */
    val id: Int,
    /** @see MediaQuery.Media.bannerImage */
    val bannerImage: String?,
    /** @see MediaQuery.Media.coverImage */
    val coverImage: String?,
    /** @see MediaQuery.CoverImage.color */
    val color: Int,
    /** @see MediaQuery.Media.title */
    val title: String?,
    /** @see MediaQuery.Media.description */
    val description: String,
    /** @see MediaQuery.Media.rankings */
    val rankings: List<Ranking>,
    /** @see MediaQuery.Media.genres */
    val genres: List<String>,
    /** @see MediaQuery.Media.characters */
    val characters: List<Character>,
    /** @see MediaQuery.Media.trailer */
    val trailer: Trailer?
) {
    data class Ranking(
        /** @see MediaQuery.Ranking.rank */
        val rank: Int,
        /** @see MediaQuery.Ranking.type */
        val type: Type,
    ) {
        /** @see MediaQuery.Ranking.type */
        enum class Type(val string: String) {
            RATED("Rated"),
            POPULAR("Popular"),
            SCORE("Score")
        }
    }

    data class Character(
        /** @see CharacterSmall.id */
        val id: Int,
        /** @see CharacterSmall.image */
        val image: String?,
        /** @see CharacterSmall.name */
        val name: String?,
    ) {
        internal constructor(query: CharacterSmall) : this(
            id = query.id,
            image = query.image?.large,
            name = query.name?.full,
        )
    }

    data class Trailer(
        /** @see MediaQuery.Trailer.id
         * @see MediaQuery.Trailer.site */
        val url: String?,
        /** @see MediaQuery.Trailer.thumbnail */
        val thumbnail: Thumbnail,
    ) {
        /** @see MediaQuery.Trailer.thumbnail */
        enum class Site(val baseUrl: String) {
            YOUTUBE("https://www.youtube.com/watch?v="),
            DAILYMOTION("https://www.dailymotion.com/video/"),
            UNKNOWN("")
        }

        data class Thumbnail(
            val maxResDefault: String?,
            val sdDefault: String?,
            val defaultThumbnail: String?
        )
    }

    internal constructor(query: MediaQuery.Media) : this(
        id = query.id,
        bannerImage = query.bannerImage,
        coverImage = query.coverImage?.extraLarge ?: query.coverImage?.large ?: query.coverImage?.medium,
        color = query.coverImage?.color?.toColorInt() ?: Color.TRANSPARENT,
        title = query.title?.romaji ?: query.title?.english ?: query.title?.native,
        description = query.description.orEmpty(),
        rankings = if (query.rankings == null) { emptyList() } else {
            // TODO: Is this filter valid?
            query.rankings.filter {
                it?.allTime == true && it.type != MediaRankType.UNKNOWN__
            }.map {
                Ranking(
                    rank = it!!.rank,
                    type = Ranking.Type.valueOf(it.type.name)
                )
            } + listOfNotNull(
                query.averageScore?.let {
                    Ranking(rank = it, type = Ranking.Type.SCORE)
                }
            )
        },
        genres = query.genres?.filterNotNull().orEmpty(),
        characters = query.characters?.nodes.orEmpty().filter { it?.characterSmall?.name != null }
            .map { Character(it!!.characterSmall) },
        trailer = if(query.trailer?.site == null || query.trailer.id == null) {
            null
        } else {
            Trailer(
                url = "${Trailer.Site.valueOf(query.trailer.site.uppercase()).baseUrl}${query.trailer.id}",
                thumbnail = with(query.trailer) {
                    Trailer.Thumbnail(
                        maxResDefault = thumbnail?.takeIf {
                            it.contains(HQ_DEFAULT)
                        }?.replace(HQ_DEFAULT, MAX_RES_DEFAULT),
                        sdDefault = thumbnail?.takeIf {
                            it.contains(HQ_DEFAULT)
                        }?.replace(HQ_DEFAULT, SD_DEFAULT),
                        defaultThumbnail = thumbnail
                    )
                }
            )
        }
    )

    data class Small(
        /** @see MediaSmall.id */
        val id: Int,
        /** @see MediaSmall.type */
        val type: Type,
        /** @see MediaSmall.title */
        val title: String?,
        /** @see MediaSmall.coverImage */
        val coverImage: String?
    ) {
        /** @see MediaSmall.type */
        enum class Type(val type: String) {
            ANIME("Anime"),
            MANGA("Manga"),
            UNKNOWN("Unknown"),
        }

        internal constructor(query: MediaSmall) : this(
            id = query.id,
            type = query.type?.name?.let { Type.valueOf(it) } ?: Type.UNKNOWN,
            coverImage = query.coverImage?.extraLarge ?: query.coverImage?.large,
            title = query.title?.romaji ?: query.title?.english ?: query.title?.native
        )
    }
}
