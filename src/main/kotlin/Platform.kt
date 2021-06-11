import kotlin.random.Random

interface Platform {
    fun yield() {}

    fun time(): Long

    fun waitKey(): UByte

    fun peekKey(key: UByte): Boolean

    fun random(): UByte {
        return Random.nextInt(0, 255).toUByte()
    }
}