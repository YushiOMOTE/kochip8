data class Pixel(val x: Int, val y: Int, var value: Boolean = false)

class Display {
    private val width = 64
    private val height = 32

    data class PixelState(var pixel: Pixel, var updated: Boolean = true)

    private var pixels: Array<PixelState> =
        Array(width * height) { PixelState(Pixel(it % width, it / width)) }

    fun clear() {
        pixels.forEach {
            it.updated = it.pixel.value
            it.pixel.value = false
        }
    }

    fun set(x: Int, y: Int, value: Boolean): Boolean {
        val p = pixels[(x % width) + (y % width) * width]
        val old = p.pixel.value
        val new = p.pixel.value xor value
        p.pixel.value = new
        p.updated = old != new
        return (old and !new)
    }

    fun updates(): List<Pixel> = pixels.filter { it.updated }.map {
        it.updated = false
        it.pixel
    }
}