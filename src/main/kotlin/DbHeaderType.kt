enum class DbHeaderType(
    val code: Int
) {
    End(0),
    CipherId(2),
    Compression(3),
    MainSeed(4),
    EncryptionIV(7),
    KdfParameters(11),
    PublicCustomData(12);

    companion object {
        fun from(code: Int): DbHeaderType? {
            for (value in entries) {
                if (value.code == code)
                    return value
            }

            return null
        }
    }
}