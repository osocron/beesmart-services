package edu.beesmart.common.exception

import com.lightbend.lagom.scaladsl.api.transport.{ExceptionMessage, TransportErrorCode, TransportException}

case class ValidationException[A](
                                 validateObject: A,
                                 message: String,
                                 errors: Set[ValidationError]
                                 ) extends TransportException(TransportErrorCode.BadRequest, ValidationException.generateMessage(message, errors), null)
object ValidationException {

  def generateErrors(errors: Set[ValidationError]) = {
    errors.map(error => s"${error.key}: ${error.message}\n").mkString("- ", "- ", "")
  }

  def generateMessage(message: String, errors: Set[ValidationError]): ExceptionMessage = {
    val details = s"$message\n" + generateErrors(errors)
    new ExceptionMessage("ValidationException", details)
  }

}

case class ValidationError(key: String, message: String)