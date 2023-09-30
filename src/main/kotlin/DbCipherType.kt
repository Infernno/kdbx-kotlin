enum class DbCipherType(
    val code: String
) {
    AES128_CBC("61ab05a1-9464-41c3-8d74-3a563df8dd35"),
    AES256_CBC("31c1f2e6-bf71-4350-be58-05216afc5aff"),
    Twofish_CBC("ad68f29f-576f-4bb9-a36a-d47af965346c"),
    ChaCha20("d6038a2b-8b6f-4cb5-a524-339a31dbb59a");

    companion object {
        fun from(code: String): DbCipherType? {
            for (value in entries) {
                if (value.code == code)
                    return value
            }

            return null
        }
    }
}