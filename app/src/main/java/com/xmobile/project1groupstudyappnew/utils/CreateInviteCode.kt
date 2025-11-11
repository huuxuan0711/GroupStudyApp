package com.xmobile.project1groupstudyappnew.utils

import java.math.BigInteger
import java.security.MessageDigest

object CreateInviteCode {
    private const val BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    fun createInviteCode(id: String, length: Int = 8): String {
        // Hash ID bằng SHA-256
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(id.toByteArray())

        // Chuyển hash bytes sang BigInteger (tránh overflow và dấu âm)
        val num = BigInteger(1, hashBytes)

        // Chuyển BigInteger sang Base62
        val base62String = toBase62(num)

        // Lấy length ký tự đầu
        return if (base62String.length >= length) {
            base62String.take(length)
        } else {
            // Nếu chưa đủ ký tự, thêm padding bằng '0'
            base62String.padStart(length, '0')
        }
    }

    private fun toBase62(num: BigInteger): String {
        if (num == BigInteger.ZERO) return "0"

        val sb = StringBuilder()
        var n = num
        val base = BigInteger.valueOf(62)
        while (n > BigInteger.ZERO) {
            val rem = (n % base).toInt()
            sb.append(BASE62[rem])
            n /= base
        }
        return sb.reverse().toString()
    }
}
