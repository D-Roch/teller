package models

import models.database.{ OrganisationMemberships, People, Addresses }
import org.joda.time.DateTime
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.db.slick.DB.withSession
import play.api.Play.current
import scala.slick.lifted.Query

/**
 * A person, such as the owner or employee of an organisation.
 */
case class Person(
  id: Option[Long],
  firstName: String,
  lastName: String,
  emailAddress: String,
  address: Address,
  bio: Option[String],
  interests: Option[String],
  twitterHandle: Option[String],
  facebookUrl: Option[String],
  linkedInUrl: Option[String],
  googlePlusUrl: Option[String],
  boardMember: Boolean = false,
  stakeholder: Boolean = true,
  active: Boolean = true,
  created: DateTime = DateTime.now(),
  createdBy: String,
  updated: DateTime,
  updatedBy: String) {

  def fullName: String = firstName + " " + lastName

  /**
   * Associates this person with given organisation.
   */
  def addMembership(organisationId: Long): Unit = {
    withSession { implicit session ⇒
      OrganisationMemberships.forInsert.insert(this.id.get, organisationId)
    }
  }

  /**
   * Removes this person’s membership in the given organisation.
   */
  def deleteMembership(organisationId: Long): Unit = {
    withSession { implicit session ⇒
      OrganisationMemberships.filter(membership ⇒ membership.personId === id && membership.organisationId === organisationId).mutate(_.delete)
    }
  }

  /**
   * Returns a list of the organisations this person is a member of.
   */
  def membership: List[Organisation] = withSession { implicit session ⇒
    val query = for {
      membership ← OrganisationMemberships if membership.personId === this.id
      organisation ← membership.organisation
    } yield organisation
    query.sortBy(_.name.toLowerCase).list
  }

  /**
   * Inserts this person into the database and returns the saved Person, with the ID added.
   */
  def insert: Person = DB.withSession { implicit session ⇒
    val newAddress = Address.insert(this.address)
    val addressId = newAddress.id.getOrElse(0L)

    val p = Person.unapply(this).get
    val insertTuple = (p._2, p._3, p._4, addressId, p._6, p._7, p._8, p._9, p._10, p._11, p._12, p._13, p._14, p._15, p._16, p._17, p._18)
    val newId = People.forInsert.insert(insertTuple)
    this.copy(id = Some(newId))
  }

  /**
   * Updates this person in the database and returns the saved person.
   */
  def update: Person = DB.withSession { implicit session ⇒
    session.withTransaction {

      val addressId = People.filter(_.id === this.id).map(_.addressId).first

      val addressQuery = for {
        address ← Addresses if address.id === addressId
      } yield address
      addressQuery.update(address.copy(id = Some(addressId)))

      val p = Person.unapply(this).get
      // We need to skip the 15th and 16th fields (created, createdBy)
      val personUpdateTuple = (p._2, p._3, p._4, p._6, p._7, p._8, p._9, p._10, p._11, p._12, p._13, p._17, p._18)
      val updateQuery = People.filter(_.id === id).map(_.forUpdate)
      updateQuery.update(personUpdateTuple)
      this
    }
  }
}

object Person {

  /**
   * Activates the organisation, if the parameter is true, or deactivates it.
   */
  def activate(id: Long, active: Boolean): Unit = withSession { implicit session ⇒
    val query = for {
      person ← People if person.id === id
    } yield person.active
    query.update(active)
  }

  def delete(id: Long): Unit = {
    withSession { implicit session ⇒
      People.where(_.id === id).mutate(_.delete)
    }
  }

  def find(id: Long): Option[Person] = withSession { implicit session ⇒
    val query = for {
      person ← People if person.id === id
      address ← person.address
    } yield (person, address)

    query.list.headOption.map {
      case (person, address) ⇒
        person.copy(address = address)
    }
  }

  def findAll: List[Person] = withSession { implicit session ⇒
    val query = for {
      person ← People
      address ← person.address
    } yield (person, address)

    query.sortBy(_._1.lastName.toLowerCase).list.map {
      case (person, address) ⇒
        person.copy(address = address)
    }
  }

  def findActive: List[Person] = withSession { implicit session ⇒
    Query(People).filter(_.active === true).sortBy(_.lastName.toLowerCase).list
  }

  def findAllActive: List[Person] = withSession { implicit session ⇒
    Query(People).filter(_.active === true).sortBy(_.lastName.toLowerCase).list
  }
}

