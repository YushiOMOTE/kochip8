class InvalidOpCodeException(private val pc: UShort, private val op: Int) :
    Exception("Invalid opcode at %04x: %04x".format(pc.toInt(), op)) {}

object Sprites {
    val Letters: IntArray = intArrayOf(
        0xf0, // 0
        0x90,
        0x90,
        0x90,
        0xf0,
        0x20, // 1
        0x60,
        0x20,
        0x20,
        0x70,
        0xf0, // 2
        0x10,
        0xf0,
        0x80,
        0xf0,
        0xf0, // 3
        0x10,
        0xf0,
        0x10,
        0xf0,
        0x90, // 4
        0x90,
        0xf0,
        0x10,
        0x10,
        0xf0, // 5
        0x80,
        0xf0,
        0x10,
        0xf0,
        0xf0, // 6
        0x80,
        0xf0,
        0x90,
        0xf0,
        0xf0, // 7
        0x10,
        0x20,
        0x40,
        0x40,
        0xf0, // 8
        0x90,
        0xf0,
        0x90,
        0xf0,
        0xf0, // 9
        0x90,
        0xf0,
        0x10,
        0xf0,
        0xf0, // A
        0x90,
        0xf0,
        0x90,
        0x90,
        0xe0, // B
        0x90,
        0xe0,
        0x90,
        0xe0,
        0xf0, // C
        0x80,
        0x80,
        0x80,
        0xf0,
        0xe0, // D
        0x90,
        0x90,
        0x90,
        0xe0,
        0xf0, // E
        0x80,
        0xf0,
        0x80,
        0xf0,
        0xf0, // F
        0x80,
        0xf0,
        0x80,
        0x80
    )
}

fun kk(op: Int): Int = op and 0xff
fun vx(op: Int): Int = op shr 8 and 0xf
fun vy(op: Int): Int = op shr 4 and 0xf
fun nnn(op: Int): Int = op and 0xfff

class Chip8(private var platform: Platform) {
    companion object {
        const val INIT_PC: UShort = 0x200u
    }

    // Vx registers
    private var v: Array<UByte> = Array(16) { 0u }

    // I register
    private var i: UShort = 0u

    // Delay timer
    private var dt: UByte = 0u

    // Sound timer
    private var st: UByte = 0u

    // Program counter
    private var pc: UShort = INIT_PC

    // Stack pointer
    private var sp: Int = 0

    // Stack
    private var stack: Array<UShort> = Array(16) { 0u }

    // Memory
    private var mem: Array<UByte> = Array(4096) { i -> Sprites.Letters.getOrElse(i) { 0 }.toUByte() }

    // Display
    var display = Display()

    // Time
    var time: Long = platform.time()

    private fun timer() {
        val now = platform.time()
        if (now - time < 16) {
            return
        }
        time = now
        if (dt > 0u) {
            println("tick")
            dt--
        }
        if (st > 0u) {
            st--
        }
    }

    private val op: Int
        get() = mem[pc.toInt()].toInt() shl 8 or mem[pc.toInt() + 1].toInt()

    private var vx: UByte
        get() = v[vx(op)]
        set(value) = v.set(vx(op), value)

    private var vy: UByte
        get() = v[vy(op)]
        set(value) = v.set(vy(op), value)

    private var vf: UByte
        get() = v[0xf]
        set(value) = v.set(0xf, value)

    private val kk: UByte
        get() = kk(op).toUByte()

    private val nnn: UShort
        get() = nnn(op).toUShort()

    private fun push(value: UShort) = stack.set(sp++, value)

    private fun pop(): UShort = stack[--sp]

    private fun jump(addr: UShort) {
        // -2 because we always proceeds the PC by 2u at every iteration.
        pc = (addr - 2u).toUShort()
    }

    private fun next() {
        pc = (pc + 2u).toUShort()
    }

    private fun dump() {
        print("execute pc=%04x op=%04x i=%04x ".format(pc.toInt(), op, i.toInt()))
        v.withIndex().map { (index, value) -> "v%x=%02x ".format(index, value.toInt()) }
            .forEach { print("${it}") }
        println()
        print("  sp=%02x [".format(sp))
        stack.forEach { print("%04x ".format(it.toInt())) }
        println("]")
    }

    private fun execute() {
        when (op and 0xf000) {
            0x0000 -> when (op) {
                0x00e0 -> {
                    display.clear()
                }
                0x00ee -> {
                    jump(pop())
                    next() // As the popped address is the PC to the previous instruction
                }
                else -> {
                    throw InvalidOpCodeException(pc, op)
                }
            }
            0x1000 -> jump(nnn)
            0x2000 -> {
                push(pc)
                jump(nnn)
            }
            0x3000 -> if (vx == kk) {
                next()
            }
            0x4000 -> if (vx != kk) {
                next()
            }
            0x5000 -> if (vx == vy) {
                next()
            }
            0x6000 -> vx = kk
            0x7000 -> vx = (vx + kk).toUByte()
            0x8000 -> when (op and 0xf) {
                0x0 -> vx = vy
                0x1 -> vx = vx or vy
                0x2 -> vx = vx and vy
                0x3 -> vx = vx xor vy
                0x4 -> {
                    vf = if (vx + vy >= 0x100u) 1u else 0u
                    vx = (vx + vy).toUByte()
                }
                0x5 -> {
                    vf = if (vx > vy) 1u else 0u
                    vx = (vx - vy).toUByte()
                }
                0x6 -> {
                    vf = vx and 1u
                    vx = (vx.toUInt() shr 1).toUByte()
                }
                0x7 -> {
                    vf = if (vy > vx) 1u else 0u
                    vx = (vy - vx).toUByte()
                }
                0xe -> {
                    vf = (vx.toUInt() shr 7).toUByte()
                    vx = (vx.toUInt() shl 1).toUByte()
                }
                else -> throw InvalidOpCodeException(pc, op)
            }
            0x9000 -> if (vx != vy) {
                next()
            }
            0xa000 -> i = nnn
            0xb000 -> jump((nnn + v[0]).toUShort())
            0xc000 -> vx = platform.random() and kk
            0xd000 -> {
                val n = op and 0xf
                val base = i.toInt()
                (0 until n).map {
                    val b = mem[base + it].toInt()
                    val collision = (0 until 8).any { x ->
                        val pixel = b and (1 shl (7 - x)) != 0
                        display.set(vx.toInt() + x, vy.toInt() + it, pixel)
                    }
                    vf = if (collision) 1u else 0u
                }
            }
            0xe000 -> when (op and 0xff) {
                0x9e -> if (platform.peekKey(vx)) {
                    next()
                }
                0xa1 -> if (!platform.peekKey(vx)) {
                    next()
                }
                else -> throw InvalidOpCodeException(pc, op)
            }
            0xf000 -> when (op and 0xff) {
                0x07 -> vx = dt
                0x0a -> vx = platform.waitKey()
                0x15 -> dt = vx
                0x18 -> st = vx
                0x1e -> i = (i + vx).toUShort()
                0x29 -> i = (vx * 5u).toUShort()
                0x33 -> {
                    val b = i.toInt()
                    mem[b] = (vx / 100u % 10u).toUByte()
                    mem[b + 1] = (vx / 10u % 10u).toUByte()
                    mem[b + 2] = (vx % 10u).toUByte()
                }
                0x55 -> (0..vx(op)).forEach { mem[i.toInt() + it] = v[it] }
                0x65 -> (0..vx(op)).forEach { v[it] = mem[i.toInt() + it] }
                else -> throw InvalidOpCodeException(pc, op)
            }
            else -> throw InvalidOpCodeException(pc, op)
        }
        next()
    }

    fun load(bytes: ByteArray) {
        bytes.withIndex().forEach { (i, v) ->
            mem[i + INIT_PC.toInt()] = v.toUByte()
        }
    }

    fun step() {
        timer()
        execute()
        platform.yield()
    }
}
