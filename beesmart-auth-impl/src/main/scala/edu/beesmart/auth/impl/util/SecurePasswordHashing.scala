package edu.beesmart.auth.impl.util

import java.security._
import javax.crypto._
import javax.crypto.spec._

import net.iharder.Base64

import scala.util.Try

object SecurePasswordHashing {
  private val RandomSource = new SecureRandom()
  private val HashPartSeparator = ":"
  private val DefaultNrOfPasswordHashIterations = 2000
  private val SizeOfPasswordSaltInBytes = 16
  private val SizeOfPasswordHashInBytes = 32

  def hashPassword(password: String): String = hashPassword(password, generateRandomBytes(SizeOfPasswordSaltInBytes))
  def hashPassword(password: String, salt: Array[Byte]): String = hashPassword(password, salt, DefaultNrOfPasswordHashIterations)
  def hashPassword(password: String, salt: Array[Byte], nrOfIterations: Int): String = {
    val hash = pbkdf2(password, salt, nrOfIterations)
    val salt64 = Base64.encodeBytes(salt)
    val hash64 = Base64.encodeBytes(hash)

    s"$nrOfIterations$HashPartSeparator$hash64$HashPartSeparator$salt64"
  }

  def validatePassword(password: String, hashedPassword: String): Boolean = {
    def slowEquals(a: Array[Byte], b: Array[Byte]): Boolean = {
      var diff = a.length ^ b.length
      for (i <- 0 until math.min(a.length, b.length)) diff += a(i) ^ b(i)
      diff == 0
    }

    val hashParts = hashedPassword.split(HashPartSeparator)

    if (hashParts.length != 3) return false
    if (!hashParts(0).forall(_.isDigit)) return false

    val nrOfIterations = hashParts(0).toInt // this will throw a NumberFormatException for non-Int numbers...
    val hash = Try(Base64.decode(hashParts(1))).toEither
    val salt = Try(Base64.decode(hashParts(2))).toEither

    if (hash.isLeft || salt.isLeft) return false
    if (hash.right.get.length == 0 || salt.right.get.length == 0) return false

    val calculatedHash = pbkdf2(password, salt.right.get, nrOfIterations)

    slowEquals(calculatedHash, hash.right.get)
  }

  private def pbkdf2(password: String, salt: Array[Byte], nrOfIterations: Int): Array[Byte] = {
    val keySpec = new PBEKeySpec(password.toCharArray, salt, nrOfIterations, SizeOfPasswordHashInBytes * 8)
    val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

    keyFactory.generateSecret(keySpec).getEncoded
  }

  private def generateRandomBytes(length: Int): Array[Byte] = {
    val keyData = new Array[Byte](length)
    RandomSource.nextBytes(keyData)
    keyData
  }
}
