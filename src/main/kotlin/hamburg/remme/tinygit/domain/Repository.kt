package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.normalize
import hamburg.remme.tinygit.shorten

class Repository(var path: String = "") {

    val shortPath: String get() = path.shorten()
    var proxyHost: String = "" // TODO: needed here?
    var proxyPort: Int = 80 // TODO: needed here?

    fun resolve(file: File) = "${path.normalize()}/${file.path}"

    override fun toString() = path

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Repository

        if (path != other.path) return false

        return true
    }

    override fun hashCode() = path.hashCode()

}
