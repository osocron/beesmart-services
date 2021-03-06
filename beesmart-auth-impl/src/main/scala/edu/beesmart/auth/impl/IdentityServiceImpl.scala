package edu.beesmart.auth.impl

import java.util.UUID

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, Forbidden}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import edu.beesmart.common.authentication.AuthenticationServiceComposition._
import edu.beesmart.common.authentication.TokenContent
import edu.beesmart.common.validation.ValidationUtil._
import edu.beesmart.auth.api.IdentityService
import edu.beesmart.auth.api.request.WithUserCreationFields
import edu.beesmart.auth.api.response.{TokenRefreshDone, UserLoginDone}
import edu.beesmart.auth.impl.util.{JwtTokenUtil, SecurePasswordHashing}

import scala.concurrent.{ExecutionContext, Future}

class IdentityServiceImpl(
                           persistentRegistry: PersistentEntityRegistry,
                           identityRepository: IdentityRepository
                         )(implicit ec: ExecutionContext) extends IdentityService
{
  override def registerClient() = ServiceCall { request =>
    validate(request)

    def executeCommandCallback = () => {
      val ref = persistentRegistry.refFor[IdentityEntity](UUID.randomUUID().toString)

      ref.ask(
        RegisterClient(
          company = request.company,
          firstName = request.firstName,
          lastName = request.lastName,
          email = request.email,
          username = request.username,
          password = request.password
        )
      )
    }

    reserveUsernameAndEmail(request, executeCommandCallback)
  }

  override def getIdentityState() = authenticated { (tokenContent, _) =>
    ServerServiceCall { _ =>
      val ref = persistentRegistry.refFor[IdentityEntity](tokenContent.clientId.toString)

      ref.ask(GetIdentityState())
    }
  }

  override def loginUser() = ServiceCall { request =>
    def passwordMatches(providedPassword: String, storedHashedPassword: String) = SecurePasswordHashing.validatePassword(providedPassword, storedHashedPassword)

    validate(request)

    for {
      maybeUser <- identityRepository.findUserByUsername(request.username)

      token = maybeUser.filter(user => passwordMatches(request.password, user.hashedPassword))
        .map(user =>
          TokenContent(
            clientId = user.clientId,
            userId = user.id,
            username = user.username
          )
        )
        .map(tokenContent => JwtTokenUtil.generateTokens(tokenContent))
        .getOrElse(throw Forbidden("Username and password combination not found"))
    }
      yield {
        UserLoginDone(token.authToken, token.refreshToken.getOrElse(throw new IllegalStateException("Refresh token missing")))
      }
  }

  override def refreshToken() = authenticatedWithRefreshToken { tokenContent =>
    ServerServiceCall { _ =>
      val token = JwtTokenUtil.generateAuthTokenOnly(tokenContent)

      Future.successful(TokenRefreshDone(token.authToken))
    }
  }

  override def createUser() = authenticated { (tokenContent, _) =>
    ServerServiceCall { request =>
      validate(request)

      def executeCommandCallback = () => {
        val ref = persistentRegistry.refFor[IdentityEntity](tokenContent.clientId.toString)

        ref.ask(
          CreateUser(
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            username = request.username,
            password = request.password
          )
        )
      }

      reserveUsernameAndEmail(request, executeCommandCallback)
    }
  }

  private def reserveUsernameAndEmail[B](request: WithUserCreationFields, onSuccess: () => Future[B]): Future[B] = {
    def rollbackReservations(request: WithUserCreationFields,usernameReserved: Boolean, emailReserved: Boolean) = {
      if (usernameReserved) {
        identityRepository.unreserveUsername(request.username)
      }
      if (emailReserved) {
        identityRepository.unreserveEmail(request.email)
      }
    }

    val canProceed = for {
      usernameReserved <- identityRepository.reserveUsername(request.username)
      emailReserved <- identityRepository.reserveEmail(request.email)
    }
      yield (usernameReserved, emailReserved)

    canProceed.flatMap(canProceed => {
      (canProceed._1 && canProceed._2) match {
        case true => onSuccess.apply()
        case false => {
          rollbackReservations(request, canProceed._1, canProceed._2)
          throw BadRequest("Either username or email is already taken.")
        }
      }
    })
  }
}
