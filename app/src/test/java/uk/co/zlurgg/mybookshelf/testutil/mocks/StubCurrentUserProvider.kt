package uk.co.zlurgg.mybookshelf.testutil.mocks

import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider

class StubCurrentUserProvider(
    var userId: String? = null
) : CurrentUserProvider {
    override fun getCurrentUserId(): String? = userId
}
