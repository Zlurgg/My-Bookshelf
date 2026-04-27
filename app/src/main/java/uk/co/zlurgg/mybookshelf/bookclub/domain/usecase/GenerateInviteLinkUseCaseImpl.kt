package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import java.net.URLEncoder

/**
 * Implementation of GenerateInviteLinkUseCase.
 *
 * Generates a web URL that:
 * 1. Shows a landing page for users without the app
 * 2. Triggers a deep link to open the app when clicked
 *
 * URL format: {baseUrl}/club?code={clubCode}&name={encodedName}
 * Deep link format: mybookshelf://club/{clubCode}
 */
class GenerateInviteLinkUseCaseImpl(
    private val shareBaseUrl: String
) : GenerateInviteLinkUseCase {

    companion object {
        private const val CLUB_PATH = "club"
        private const val PARAM_CODE = "code"
        private const val PARAM_NAME = "name"
    }

    override operator fun invoke(clubCode: String, clubName: String?): String {
        val builder = StringBuilder(shareBaseUrl)
            .append("/")
            .append(CLUB_PATH)
            .append("?")
            .append(PARAM_CODE)
            .append("=")
            .append(clubCode)

        if (!clubName.isNullOrBlank()) {
            val encodedName = URLEncoder.encode(clubName, "UTF-8")
            builder.append("&")
                .append(PARAM_NAME)
                .append("=")
                .append(encodedName)
        }

        return builder.toString()
    }
}
