class InvalidOpCodeException(pc: UShort, op: Int) :
    Exception("Invalid opcode at %04x: %04x".format(pc.toInt(), op))

private object Sprites {
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

class Chip8(private val platform: Platform) {
    companion object {
        const val INIT_PC: UShort = 0x200u
    }

    // Display
    val display = Display()

    // Vx registers
    private val v: Array<UByte> = Array(16) { 0u }

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
    private val stack: Array<UShort> = Array(16) { 0u }

    // Memory
    private val mem: Array<UByte> = Array(4096) { i -> Sprites.Letters.getOrElse(i) { 0 }.toUByte() }

    // Time
    private var time: Long = platform.time()

    private fun timer() {
        val now = platform.time()
        if (now - time < 32) {
            return
        }
        time = now
        if (dt > 0u) {
            dt--
        }
        if (st > 0u) {
            st--
        }
    }

    private val op: Op
        get() = Op(mem[pc.toInt()].toInt() shl 8 or mem[pc.toInt() + 1].toInt())

    private var vx: UByte
        get() = v[op.x]
        set(value) = v.set(op.x, value)

    private var vy: UByte
        get() = v[op.y]
        set(value) = v.set(op.y, value)

    private var vf: UByte
        get() = v[0xf]
        set(value) = v.set(0xf, value)

    private val kk: UByte
        get() = op.kk

    private val nnn: UShort
        get() = op.nnn

    private fun push(value: UShort) = stack.set(sp++, value)

    private fun pop(): UShort = stack[--sp]

    private fun jump(addr: UShort) {
        // -2 because we always proceeds the PC by 2u at every iteration.
        pc = (addr - 2u).toUShort()
    }

    private fun next() {
        pc = (pc + 2u).toUShort()
    }

    private fun execute() {
        when (op) {
            Op(0x00e0) ->
                display.clear()
            Op(0x00ee) -> {
                jump(pop())
                next() // As the popped address is the PC to the previous instruction
            }
            Op(0x1000, 0xf000) -> jump(nnn)
            Op(0x2000, 0xf000) -> {
                push(pc)
                jump(nnn)
            }
            Op(0x3000, 0xf000) -> if (vx == kk) {
                next()
            }
            Op(0x4000, 0xf000) -> if (vx != kk) {
                next()
            }
            Op(0x5000, 0xf00f) -> if (vx == vy) {
                next()
            }
            Op(0x6000, 0xf000) -> vx = kk
            Op(0x7000, 0xf000) -> vx = (vx + kk).toUByte()
            Op(0x8000, 0xf00f) -> vx = vy
            Op(0x8001, 0xf00f) -> vx = vx or vy
            Op(0x8002, 0xf00f) -> vx = vx and vy
            Op(0x8003, 0xf00f) -> vx = vx xor vy
            Op(0x8004, 0xf00f) -> {
                vf = if (vx + vy >= 0x100u) 1u else 0u
                vx = (vx + vy).toUByte()
            }
            Op(0x8005, 0xf00f) -> {
                vf = if (vx > vy) 1u else 0u
                vx = (vx - vy).toUByte()
            }
            Op(0x8006, 0xf00f) -> {
                vf = vx and 1u
                vx = (vx.toUInt() shr 1).toUByte()
            }
            Op(0x8007, 0xf00f) -> {
                vf = if (vy > vx) 1u else 0u
                vx = (vy - vx).toUByte()
            }
            Op(0x800e, 0xf00f) -> {
                vf = (vx.toUInt() shr 7).toUByte()
                vx = (vx.toUInt() shl 1).toUByte()
            }
            Op(0x9000, 0xf00f) -> if (vx != vy) {
                next()
            }
            Op(0xa000, 0xf000) -> i = nnn
            Op(0xb000, 0xf000) -> jump((nnn + v[0]).toUShort())
            Op(0xc000, 0xf000) -> vx = platform.random() and kk
            Op(0xd000, 0xf000) -> {
                val base = i.toInt()

                vf = 0u
                (0 until op.n).forEach { y ->
                    val b = mem[base + y].toInt()
                    (0 until 8).forEach { x ->
                        val pixel = b and (1 shl (7 - x)) != 0
                        if (display.set(vx.toInt() + x, vy.toInt() + y, pixel)) {
                            vf = 1u
                        }
                    }
                }
            }
            Op(0xe09e, 0xf0ff) -> if (platform.peekKey(vx)) {
                next()
            }
            Op(0xe0a1, 0xf0ff) -> if (!platform.peekKey(vx)) {
                next()
            }
            Op(0xf007, 0xf0ff) -> vx = dt
            Op(0xf00a, 0xf0ff) -> vx = platform.waitKey()
            Op(0xf015, 0xf0ff) -> dt = vx
            Op(0xf018, 0xf0ff) -> st = vx
            Op(0xf01e, 0xf0ff) -> i = (i + vx).toUShort()
            Op(0xf029, 0xf0ff) -> i = (vx * 5u).toUShort()
            Op(0xf033, 0xf0ff) -> {
                val b = i.toInt()
                mem[b] = (vx / 100u % 10u).toUByte()
                mem[b + 1] = (vx / 10u % 10u).toUByte()
                mem[b + 2] = (vx % 10u).toUByte()
            }
            Op(0xf055, 0xf0ff) -> (0..op.x).forEach { mem[i.toInt() + it] = v[it] }
            Op(0xf065, 0xf0ff) -> (0..op.x).forEach { v[it] = mem[i.toInt() + it] }
            else -> throw InvalidOpCodeException(pc, op.value)
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

    fun dump() {
        print("execute pc=%04x op=%04x i=%04x ".format(pc.toInt(), op, i.toInt()))
        v.withIndex().map { (index, value) -> "v%x=%02x ".format(index, value.toInt()) }
            .forEach { print(it) }
        println()
        print("  sp=%02x [".format(sp))
        stack.forEach { print("%04x ".format(it.toInt())) }
        println("]")
    }
}
