package io.github.achyuki.folddevtools.service

import java.io.IOException

class RemoteServiceException : IOException {
    constructor()

    constructor(message: String?) : super(message)

    constructor(cause: Throwable?) : super(cause)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
