import java.io.InputStream

class DbHeader(
    val type: DbHeaderType,
    val size: UInt,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DbHeader

        if (type != other.type) return false
        if (size != other.size) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "DbHeader: type = $type, size = $size, data = ${data.contentToString()}"
    }
}

fun InputStream.readHeaders(): Map<DbHeaderType, DbHeader> {
    val headers = mutableMapOf<DbHeaderType, DbHeader>()

    while (true) {
        val header = readHeader()

        if (header == null || header.type == DbHeaderType.End) {
            break
        }

        headers[header.type] = header
    }

    return headers
}

fun InputStream.readHeader(): DbHeader? {
    val type = DbHeaderType.from(read()) ?: return null

    val size = readInt()
    val data = readNBytes(size.toInt())

    return DbHeader(
        type = type,
        size = size,
        data = data
    )
}