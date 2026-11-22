package app.ryadom.core.model

/** Ошибка, которую можно показать человеку. Код переводится в текст на уровне UI. */
class RyadomException(val reason: Reason) : Exception(reason.name) {
    enum class Reason {
        INVALID_EMAIL, WEAK_PASSWORD, EMPTY_NAME, ACCOUNT_NOT_FOUND, EMAIL_TAKEN,
        INVITE_INVALID, NEEDS_CLOUD, NOT_ALLOWED, LAST_OWNER, NOT_FOUND,
    }
}

