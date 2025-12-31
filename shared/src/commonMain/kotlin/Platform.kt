import com.mrnrod45.nstk.domain.file.FileSplitter

expect fun getFileSplitter(): FileSplitter

interface Platform {
    val name: String
}

expect class Greeting() {
    fun greet(): String
}

enum class PlatformType {
    ANDROID,
    DESKTOP
}

expect fun getPlatformType(): PlatformType
