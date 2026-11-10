package com.example.mfb.data.engine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.mfb.core.security.ImportSecurityException
import com.example.mfb.core.security.ZipSecurityValidator
import com.example.mfb.domain.model.ImportStatus
import com.example.mfb.domain.model.Project
import com.example.mfb.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

sealed class ZipImportResult {
    data class Success(val project: Project) : ZipImportResult()
    data class NeedsEntryPointChoice(
        val tempDirName: String,
        val candidates: List<String>,
        val originalFileName: String,
        val suggestedName: String
    ) : ZipImportResult()
    data class Error(val message: String, val exception: Exception? = null) : ZipImportResult()
}

class CountingInputStream(private val wrapped: InputStream) : InputStream() {
    var bytesRead: Long = 0
        private set

    override fun read(): Int {
        val b = wrapped.read()
        if (b != -1) bytesRead++
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = wrapped.read(b, off, len)
        if (n != -1) bytesRead += n
        return n
    }

    override fun close() {
        wrapped.close()
    }
}

object ZipImportEngine {

    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        customProjectName: String? = null,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): ZipImportResult = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val (originalFileName, archiveSize) = getFileInfo(context, uri)

        if (archiveSize > ZipSecurityValidator.MAX_ARCHIVE_SIZE) {
            return@withContext ZipImportResult.Error(
                "Archive size exceeds safety limit of ${ZipSecurityValidator.MAX_ARCHIVE_SIZE / (1024 * 1024)} MB",
                ImportSecurityException.MaxArchiveSizeExceededException(
                    archiveSize,
                    ZipSecurityValidator.MAX_ARCHIVE_SIZE
                )
            )
        }

        val tempUuid = UUID.randomUUID().toString()
        val tempDir = File(context.cacheDir, "temp_zip_$tempUuid")
        if (!tempDir.mkdirs()) {
            return@withContext ZipImportResult.Error("Failed to initialize staging directory")
        }

        try {
            onProgress(0.10f, "Analyzing archive security & structure...")

            val rawStream = contentResolver.openInputStream(uri)
                ?: throw ImportSecurityException.CorruptedArchiveException("Unable to open archive stream")

            val countingStream = CountingInputStream(BufferedInputStream(rawStream))
            val zipInputStream = ZipInputStream(countingStream)

            var entryCount = 0
            var totalExtractedBytes = 0L
            val buffer = ByteArray(8192)

            zipInputStream.use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    entryCount++
                    val entryName = entry.name

                    // Security check: Zip Slip defense
                    val destFile = ZipSecurityValidator.resolveAndValidateDestination(tempDir, entryName)

                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        var currentFileBytes = 0L

                        FileOutputStream(destFile).use { fos ->
                            var readBytes: Int
                            while (zis.read(buffer).also { readBytes = it } != -1) {
                                currentFileBytes += readBytes
                                totalExtractedBytes += readBytes

                                ZipSecurityValidator.checkMetrics(
                                    entryCount = entryCount,
                                    totalExtractedBytes = totalExtractedBytes,
                                    compressedBytesRead = countingStream.bytesRead,
                                    currentFileBytes = currentFileBytes,
                                    currentFileName = entryName
                                )

                                fos.write(buffer, 0, readBytes)
                            }
                        }
                    }

                    val progressFraction = if (archiveSize > 0) {
                        (countingStream.bytesRead.toFloat() / archiveSize.toFloat()).coerceIn(0.15f, 0.85f)
                    } else {
                        0.5f
                    }
                    onProgress(progressFraction, "Extracting: ${entryName.substringAfterLast('/')}")

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (entryCount == 0) {
                tempDir.deleteRecursively()
                return@withContext ZipImportResult.Error("ZIP archive is empty")
            }

            onProgress(0.90f, "Verifying HTML entry points...")
            val suggestedName = customProjectName?.takeIf { it.isNotBlank() }
                ?: originalFileName.substringBeforeLast('.').ifBlank { "Imported Project" }

            when (val detection = HtmlEntryDetector.detectEntryPoint(tempDir)) {
                is HtmlDetectionResult.None -> {
                    tempDir.deleteRecursively()
                    ZipImportResult.Error(
                        "No HTML entry point (index.html or HTML files) found in archive",
                        ImportSecurityException.NoHtmlEntryPointException()
                    )
                }

                is HtmlDetectionResult.MultipleCandidates -> {
                    // Staged for user selection
                    ZipImportResult.NeedsEntryPointChoice(
                        tempDirName = tempDir.name,
                        candidates = detection.candidates,
                        originalFileName = originalFileName,
                        suggestedName = suggestedName
                    )
                }

                is HtmlDetectionResult.AutoSelected -> {
                    finalizeProject(
                        context = context,
                        stagedDir = tempDir,
                        projectName = suggestedName,
                        originalFileName = originalFileName,
                        mainHtmlPath = detection.relativePath,
                        onProgress = onProgress
                    )
                }
            }
        } catch (e: ImportSecurityException) {
            tempDir.deleteRecursively()
            ZipImportResult.Error(e.message ?: "Security validation error", e)
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            ZipImportResult.Error("Extraction failed: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    suspend fun completeWithSelectedEntryPoint(
        context: Context,
        tempDirName: String,
        selectedEntryPoint: String,
        projectName: String,
        originalFileName: String,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): ZipImportResult = withContext(Dispatchers.IO) {
        val stagedDir = File(context.cacheDir, tempDirName)
        if (!stagedDir.exists()) {
            return@withContext ZipImportResult.Error("Temporary import session expired. Please re-import the file.")
        }

        try {
            val chosenFile = File(stagedDir, selectedEntryPoint)
            if (!chosenFile.exists() || !chosenFile.isFile) {
                return@withContext ZipImportResult.Error("Selected entry point '$selectedEntryPoint' not found in archive")
            }

            finalizeProject(
                context = context,
                stagedDir = stagedDir,
                projectName = projectName,
                originalFileName = originalFileName,
                mainHtmlPath = selectedEntryPoint,
                onProgress = onProgress
            )
        } catch (e: Exception) {
            stagedDir.deleteRecursively()
            ZipImportResult.Error("Finalizing project failed: ${e.localizedMessage}", e)
        }
    }

    private fun finalizeProject(
        context: Context,
        stagedDir: File,
        projectName: String,
        originalFileName: String,
        mainHtmlPath: String,
        onProgress: (Float, String) -> Unit
    ): ZipImportResult {
        onProgress(0.95f, "Organizing project workspace...")
        val projectUuid = UUID.randomUUID().toString()
        val targetProjectDir = ProjectStorageManager.getProjectDir(context, projectUuid)

        // Move contents into targetProjectDir
        stagedDir.copyRecursively(targetProjectDir, overwrite = true)
        stagedDir.deleteRecursively()

        val (fileCount, totalSizeBytes) = ProjectStorageManager.calculateStats(targetProjectDir)

        val project = Project(
            projectUuid = projectUuid,
            name = projectName,
            description = "Imported from $originalFileName",
            packageId = "com.mfb.app.${projectName.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "app" }}",
            versionName = "1.0.0",
            versionCode = 1,
            originalFileName = originalFileName,
            sourceType = SourceType.ZIP,
            mainHtmlRelativePath = mainHtmlPath,
            internalProjectPath = targetProjectDir.absolutePath,
            fileCount = fileCount,
            totalSizeBytes = totalSizeBytes,
            importStatus = ImportStatus.SUCCESS,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        ProjectStorageManager.writeMetadata(targetProjectDir, project)
        onProgress(1.0f, "Project successfully imported!")
        return ZipImportResult.Success(project)
    }

    private fun getFileInfo(context: Context, uri: Uri): Pair<String, Long> {
        var name = "archive.zip"
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex) ?: name
                }
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        return Pair(name, size)
    }
}
