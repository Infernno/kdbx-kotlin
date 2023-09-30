import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DbVariantMap(
    val version: UShort,
    val items: Map<String, Value>,
) {

    override fun toString(): String {
        return StringBuilder().apply {

            appendLine("VariantMap version: $version")

            items.forEach { (k, v) ->
                appendLine("$k: ${v.type} - ${v.value.contentToString()}")
            }

        }.toString()
    }

    class Value(
        val type: ValueType,
        val value: ByteArray,
    ) {
        fun toInt(): Int {
            return ByteBuffer
                .allocate(value.size)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(value)
                .getInt(0)
        }

        fun toUInt64(): ULong {
            return ByteBuffer
                .allocate(value.size)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(value)
                .getLong(0)
                .toULong()
        }
    }

    enum class ValueType(val code: Int) {
        UInt32(0x04),
        UInt64(0x05),
        Bool(0x08),
        Int32(0x0C),
        Int64(0x0D),
        String(0x18),
        ByteArray(0x42);

        companion object {
            fun from(code: Int): ValueType? {
                for (type in entries) {
                    if (type.code == code)
                        return type
                }

                return null
            }
        }
    }

    companion object {
        fun fromByteArray(byteArray: ByteArray): DbVariantMap = byteArray.inputStream().use { stream ->
            val version = stream.readShort()
            val items = mutableMapOf<String, Value>()

            while (true) {
                val type = stream.read()

                if (type == 0)
                    break

                val keySize = stream.readInt()
                val key = stream.readNBytes(keySize.toInt())
                val valueSize = stream.readInt()
                val value = stream.readNBytes(valueSize.toInt())

                val typeEnum = ValueType.from(type)

                if (typeEnum == null) {
                    System.err.println("Unknown type: $type")
                    continue
                }

                items[String(key)] = Value(
                    type = typeEnum,
                    value = value
                )
            }

            DbVariantMap(version, items)
        }
    }
}
