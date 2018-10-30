package edu.beesmart.common.validation

import com.wix.accord.transform.ValidationTransform
import com.wix.accord.{Failure, Violation, validate => accordValidate}
import edu.beesmart.common.exception.{ValidationError, ValidationException}

object ValidationUtil {
  def validate[A](underValidation: A)(implicit validator: ValidationTransform.TransformedValidator[A]): Unit = {
    val validationResult = accordValidate(underValidation)

    validationResult match {
      case failure: Failure => throw ValidationException(underValidation, "Object failed validation", extractValidationErrors(failure.violations))
      case _ =>
    }
  }
  private def extractValidationErrors(violations: Set[Violation]) = {
    violations.map(violation =>
      ValidationError(key = violation.toString, message = violation.constraint)
    )
  }
}
