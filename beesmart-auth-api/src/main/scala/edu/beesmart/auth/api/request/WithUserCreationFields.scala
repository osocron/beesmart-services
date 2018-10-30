package edu.beesmart.auth.api.request

trait WithUserCreationFields {
  val firstName: String
  val lastName: String
  val email: String
  val username: String
  val password: String
}
