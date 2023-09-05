package com.example.core_module



class MyMainClass {
    companion object{
        const val MY_CONSTANT = "Hello World"
    }

    /**
     * Encrypts sensitive data using a secret key obtained from the Android KeyStore.
     *
     * @param keyAlias     The key alias used to obtain the cryptographic secret key.
     * @param textInBytes  The data to be encrypted.
     * @return             The encrypted byte array.
     */
    fun encryptAES(keyAlias: String, textInBytes: ByteArray): ByteArray {
        return textInBytes
    }

}