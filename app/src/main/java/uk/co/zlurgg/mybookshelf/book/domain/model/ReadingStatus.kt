package uk.co.zlurgg.mybookshelf.book.domain.model

/**
 * Represents the user's reading status for a book.
 */
enum class ReadingStatus {
    /** Default - not yet read */
    NOT_READ,

    /** Currently reading */
    READING,

    /** Finished reading */
    FINISHED
}
