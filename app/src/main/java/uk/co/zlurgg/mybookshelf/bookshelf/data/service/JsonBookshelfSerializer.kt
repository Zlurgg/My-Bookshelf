package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.BookshelfExportMapper
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfSerializer
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * JSON implementation of BookshelfSerializer.
 * Handles pure serialization logic with proper error handling.
 */
class JsonBookshelfSerializer(
    private val exportMapper: BookshelfExportMapper
) : BookshelfSerializer {

    private val json = Json {
        prettyPrint = false  // Minified JSON for smaller URL size with GZip compression
        ignoreUnknownKeys = true
    }

    override fun serialize(shelf: Bookshelf): Result<String, DataError.Local> {
        return try {
            val exportData = exportMapper.toExportData(shelf)
            val jsonString = json.encodeToString(BookshelfExportData.serializer(), exportData)
            Result.Success(jsonString)
        } catch (_: SerializationException) {
            Result.Error(DataError.Local.SERIALIZATION_ERROR)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }

    override fun deserialize(jsonData: String): Result<BookshelfExportData, DataError.Local> {
        return try {
            val exportData = json.decodeFromString<BookshelfExportData>(jsonData)
            Result.Success(exportData)
        } catch (_: SerializationException) {
            Result.Error(DataError.Local.SERIALIZATION_ERROR)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}