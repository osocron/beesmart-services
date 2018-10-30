package edu.beesmart.auth.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import edu.beesmart.auth.api.request.{ClientRegistration, UserCreation, UserLogin}
import edu.beesmart.auth.api.response.{IdentityStateDone, TokenRefreshDone, UserLoginDone}
import edu.beesmart.common.response.GeneratedIdDone

trait IdentityService extends Service {
  def registerClient(): ServiceCall[ClientRegistration, GeneratedIdDone]
  def loginUser(): ServiceCall[UserLogin, UserLoginDone]
  def refreshToken(): ServiceCall[NotUsed, TokenRefreshDone]
  def getIdentityState(): ServiceCall[NotUsed, IdentityStateDone]
  def createUser(): ServiceCall[UserCreation, GeneratedIdDone]

  override final def descriptor: Descriptor = {
    import Service._

    named("identity-service").withCalls(
      restCall(Method.POST, "/api/client/registration", registerClient _),
      restCall(Method.POST, "/api/user/login", loginUser _),
      restCall(Method.PUT, "/api/user/token", refreshToken _),
      restCall(Method.GET, "/api/state/identity", getIdentityState _),
      restCall(Method.POST, "/api/user", createUser _)
    ).withAutoAcl(true)
  }
}
