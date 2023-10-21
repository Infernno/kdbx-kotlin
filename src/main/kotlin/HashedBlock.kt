import java.io.InputStream

class HashedBlock(
    val hmacSha256: ByteArray,
    val length: Int,
    val payload: ByteArray
)

fun InputStream.readBlocks(): List<HashedBlock> {
    val blocks = mutableListOf<HashedBlock>()

    while (available() > 0) {
        blocks += readBlock()
    }

    return blocks
}

fun InputStream.readBlock(): HashedBlock {
    val hmacSha256 = readNBytes(32)
    val length = readInt().toInt()

    val payload = readNBytes(length)

    return HashedBlock(
        hmacSha256, length, payload
    )
}
