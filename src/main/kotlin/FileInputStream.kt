import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

fun InputStream.readInt(): UInt {
    val buffer = readNBytes(4)

    return ByteBuffer
        .allocate(4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(buffer)
        .getInt(0)
        .toUInt()
}

fun InputStream.readShort(): UShort {
    val buffer = readNBytes(2)

    return ByteBuffer
        .allocate(2)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(buffer)
        .getShort(0)
        .toUShort()
}

fun ByteArray.toUuid(): String {
    val bb = ByteBuffer.wrap(this)

    val high = bb.getLong()
    val low = bb.getLong()

    val uuid = UUID(high, low)

    return uuid.toString()
}

infix fun UShort.shr(i: Int): UShort {
    return (this.toInt() shr i).toUShort()
}

fun Long.toUint64Bytes(): ByteArray {
    return ByteBuffer.allocate(Long.SIZE_BYTES)
        .putLong(this)
        .array()
}
