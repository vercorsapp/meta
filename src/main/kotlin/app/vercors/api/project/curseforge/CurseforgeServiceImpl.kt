/*
 * Copyright (c) 2024 skyecodes
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package app.vercors.api.project.curseforge

import app.vercors.proto.ProjectOuterClass.Project
import app.vercors.proto.ProjectOuterClass.ProjectType
import app.vercors.proto.project
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurseforgeServiceImpl @Inject constructor(private val api: CurseforgeApi) : CurseforgeService {
    override suspend fun search(type: ProjectType, limit: Int): List<Project> =
        api.search(
            gameId = MINECRAFT_GAME_ID,
            classId = type.asClassId(),
            sortField = CurseforgeProjectSearchSortField.Popularity,
            sortOrder = CurseforgeProjectSearchSortOrder.Desc,
            pageSize = 10
        ).data.convertAll()

    private fun ProjectType.asClassId(): Int = when (this) {
        ProjectType.mod -> MOD_CLASS_ID
        ProjectType.modpack -> MODPACK_CLASS_ID
        ProjectType.resourcepack -> RESOURCEPACK_CLASS_ID
        ProjectType.shader -> SHADERPACK_CLASS_ID
        else -> throw IllegalArgumentException("Unrecognized ProjectType: $this")
    }

    private fun List<CurseforgeProject>.convertAll(): List<Project> = map { convert(it) }

    private fun convert(project: CurseforgeProject): Project = project {
        name = project.name
        author = project.authors[0].name
        logoUrl = project.logo.thumbnailUrl
        project.screenshots.firstOrNull()?.let { imageUrl = it.thumbnailUrl }
        description = project.summary
    }

    companion object {
        private const val MINECRAFT_GAME_ID = 432
        private const val MODPACK_CLASS_ID = 4471
        private const val MOD_CLASS_ID = 6
        private const val RESOURCEPACK_CLASS_ID = 12
        private const val SHADERPACK_CLASS_ID = 6552
    }
}
