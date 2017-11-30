package hamburg.remme.tinygit.git

class LocalRepository(var path: String = "") {

    val shortPath: String get() = path.split("[\\\\/]".toRegex()).last()
    var ssh: String = ""
    var username: String = ""
    var password: String = ""
    var proxyHost: String = ""
    var proxyPort: Int = 80

    override fun toString() = path

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalRepository

        if (path != other.path) return false

        return true
    }

    override fun hashCode() = path.hashCode()

}
