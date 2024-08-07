package com.imashnake.animite.api.anilist.sanitize.profile

import com.imashnake.animite.api.anilist.UserMediaListQuery
import com.imashnake.animite.api.anilist.fragment.User
import com.imashnake.animite.api.anilist.sanitize.media.Media
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

/**
 * Sanitized [User].
 *
 * @param id
 * @param name
 * @param description
 * @param avatar
 * @param banner
 * @param count
 * @param daysWatched
 * @param meanScore
 * @param genres
 */
data class User(
    /** @see User.id */
    val id: Int,
    /** @see User.name */
    val name: String,
    /** @see User.about */
    val description: String?,
    /** @see User.avatar */
    val avatar: String?,
    /** @see User.bannerImage */
    val banner: String?,
    // region About
    /** @see User.Anime.count */
    val count: Int?,
    /** @see User.Anime.minutesWatched */
    val daysWatched: Double?,
    /** @see User.Anime.meanScore */
    val meanScore: Float?,
    /** @see User.Anime.genres */
    val genres: List<Genre>
    // endregion
) {
    /**
     * Sanitized [User.Genre]
     *
     * @param genre
     * @param mediaCount
     */
    data class Genre(
        /** @see User.Genre.genre */
        val genre: String,
        /** @see User.Genre.count */
        val mediaCount: Int,
    )

    data class MediaCollection(val namedLists: List<NamedList>) {
        data class NamedList(
            val name: String?,
            val list: List<Media.Small>
        ) {
            internal constructor(query: UserMediaListQuery.List) : this(
                name = query.name,
                list = query.entries.orEmpty().mapNotNull {
                    Media.Small(it?.media?.mediaSmall ?: return@mapNotNull null)
                }
            )
        }

        internal constructor(query: UserMediaListQuery.Data) : this(
            namedLists = query.mediaListCollection?.lists.orEmpty().mapNotNull {
                NamedList(it ?: return@mapNotNull null)
            }
        )
    }

    internal constructor(query: User) : this(
        id = query.id,
        name = query.name,
        description = query.about,
        avatar = query.avatar?.large,
        banner = query.bannerImage,
        count = query.statistics?.anime?.count,
        daysWatched = query.statistics?.anime?.minutesWatched
            ?.minutes?.toDouble(DurationUnit.DAYS),
        meanScore = query.statistics?.anime?.meanScore?.toFloat(),
        genres = query.statistics?.anime?.genres.orEmpty().filterNotNull().run {
            val totalCount = this.sumOf { genre -> genre.count }
            mapNotNull {
                Genre(
                    genre = it.genre ?: return@mapNotNull null,
                    mediaCount = it.count
                )
            }.filter {
                // Filters out anime genres that contribute to less than 5%.
                it.mediaCount > totalCount/20
            }.sortedByDescending { it.mediaCount }
        }
    )
}
