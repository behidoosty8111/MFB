package com.example.mfb.domain.model

enum class SourceType {
    ZIP,
    HTML
}

enum class ImportStatus {
    SUCCESS,
    FAILED,
    PENDING
}

data class Project(
    val id: Long = 0,
    val projectUuid: String = "",
    val name: String,
    val description: String = "",
    val packageId: String = "com.mfb.app",
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val originalFileName: String = "",
    val sourceType: SourceType = SourceType.ZIP,
    val mainHtmlRelativePath: String = "index.html",
    val internalProjectPath: String = "",
    val fileCount: Int = 1,
    val totalSizeBytes: Long = 0,
    val importStatus: ImportStatus = ImportStatus.SUCCESS,
    val thumbnailPath: String? = null,
    val isZipArchive: Boolean = (sourceType == SourceType.ZIP),
    val sourcePath: String = internalProjectPath,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = 0L,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val iconUri: String? = null,
    val missingLocalAssets: List<String> = emptyList()
)
