import de.mkammerer.argon2.Argon2Constants
import de.mkammerer.argon2.Argon2Factory
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


val SIGNATURE_1 = 0x9AA2D903.toUInt()
val SIGNATURE_2 = 0xB54BFB67.toUInt()

fun main(args: Array<String>) {
    // val path = "test.kdbx"
    val path = "C:\\Users\\Homie\\Documents\\Projects\\kdbx-kotlin\\src\\main\\kotlin\\test2.kdbx"
    val reader = File(path).inputStream()

    val sig1 = reader.readInt()
    val sig2 = reader.readInt()

    if (sig1 != SIGNATURE_1 && sig2 != SIGNATURE_2) {
        System.err.println("Signature doesn't match!")
        return
    }

    val minor = reader.readShort()
    val major = reader.readShort()

    val headers = reader.readHeaders()
    val encryption = headers[DbHeaderType.CipherId]?.let { DbCipherType.from(it.data.toUuid()) }

    println("DB Version: ${major}.${minor}")
    println("Encryption: $encryption")
    println()

    val signature = reader.readNBytes(64)
    val blocks = reader.readBlocks()
    println("Blocks count: ${blocks.size}")

    val decrypted = decrypt(headers, blocks)

    for ((index, block) in decrypted.withIndex()) {
        if (block.isEmpty())
            continue

        val outputFile = File("block-$index.bin")

        if (outputFile.exists()) {
            outputFile.delete()
        }

        outputFile.createNewFile()

        GZIPInputStream(block.inputStream()).use { gis ->
            outputFile.outputStream().use { gis.copyTo(it) }
        }
    }
}

private fun getDerivedKey(kdfParameters: DbHeader, password: String): ByteArray {
    require(kdfParameters.type == DbHeaderType.KdfParameters)

    val variantMap = DbVariantMap.fromByteArray(kdfParameters.data)

    val salt = variantMap.items["S"]!!.value
    val parallelism = variantMap.items["P"]!!.toInt()
    val memoryUsage = variantMap.items["M"]!!.toUInt64().toInt() / 1024
    val iterations = variantMap.items["I"]!!.toInt()
    val argonVersion = variantMap.items["V"]!!.toInt()
    val optionalKey = variantMap.items["K"]
    val optionalData = variantMap.items["A"]

    val argon2 = Argon2Factory.createAdvanced(
        Argon2Factory.Argon2Types.ARGON2d,
        salt.size,
        Argon2Constants.DEFAULT_HASH_LENGTH
    )

    val compositeKey = password.encodeToByteArray().sha256().sha256()

    return argon2.rawHash(iterations, memoryUsage, parallelism, compositeKey, salt)
}

private fun ByteArray.sha256(): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(this)
}

private fun ByteArray.sha512(): ByteArray {
    return MessageDigest.getInstance("SHA-512").digest(this)
}

private fun decrypt(headers: Map<DbHeaderType, DbHeader>, blocks: List<HashedBlock>): List<ByteArray> {
    val mainSeed = headers[DbHeaderType.MainSeed]!!.data
    val derivedKey = getDerivedKey(headers[DbHeaderType.KdfParameters]!!, "123456")

    val encryptionKey = (mainSeed + derivedKey).sha256()
    val hmacKey = (mainSeed + derivedKey + 0x01).sha512()

    println("Encryption key = ${encryptionKey.contentToString()}")
    println("HMAC key = ${hmacKey.contentToString()}")

    val iv = headers[DbHeaderType.EncryptionIV]!!.data
    val secretKey = SecretKeySpec(encryptionKey, "AES")
    val ivParameterSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
        init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
    }

    val decryptedBlock = ArrayList<ByteArray>(blocks.size)

    for (block in blocks) {
        val input = block.payload
        val decrypted = cipher.doFinal(input)

        decryptedBlock.add(decrypted)
    }

    return decryptedBlock
}
