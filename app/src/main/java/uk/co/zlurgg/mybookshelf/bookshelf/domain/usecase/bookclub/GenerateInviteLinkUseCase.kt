package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

/**
 * UseCase for generating a shareable invite link for a Book Club.
 *
 * The link can be shared via messaging apps and allows other users
 * to join the book club when opened.
 */
interface GenerateInviteLinkUseCase {
    /**
     * Generates an invite link for the given club code.
     *
     * @param clubCode The book club code
     * @param clubName Optional club name to include in the link for display purposes
     * @return A shareable URL string
     */
    fun execute(clubCode: String, clubName: String? = null): String
}
