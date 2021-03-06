
package net.ddns.lsmobile.zencashvaadinwalletui4cpp.dal;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import com.xdev.dal.JPADAO;

import net.ddns.lsmobile.zencashvaadinwalletui4cpp.entities.Address;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.entities.Address_;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.entities.User;

/**
 * Home object for domain model class Address.
 * 
 * @see Address
 */
public class AddressDAO extends JPADAO<Address, Integer> {
	public AddressDAO() {
		super(Address.class);
	}

	/**
	 * @queryMethod Do not edit, method is generated by editor!
	 */
	public List<Address> isAddressFromUser(final User user, final String address) {
		final EntityManager entityManager = em();
	
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
	
		final ParameterExpression<User> userParameter = criteriaBuilder.parameter(User.class, "user");
		final ParameterExpression<String> addressParameter = criteriaBuilder.parameter(String.class, "address");
	
		final CriteriaQuery<Address> criteriaQuery = criteriaBuilder.createQuery(Address.class);
	
		final Root<Address> root_a = criteriaQuery.from(Address.class);
		root_a.alias("a");
	
		criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.equal(root_a.get(Address_.user1), userParameter),
				criteriaBuilder.like(root_a.get(Address_.address), addressParameter)));
	
		final TypedQuery<Address> query = entityManager.createQuery(criteriaQuery);
		query.setParameter(userParameter, user);
		query.setParameter(addressParameter, address);
		return query.getResultList();
	}

	/**
	 * @queryMethod Do not edit, method is generated by editor!
	 */
	public List<Address> findOrphanedAddresses() {
		final EntityManager entityManager = em();
	
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
	
		final CriteriaQuery<Address> criteriaQuery = criteriaBuilder.createQuery(Address.class);
	
		final Root<Address> root_a = criteriaQuery.from(Address.class);
		root_a.alias("a");
	
		criteriaQuery.where(criteriaBuilder.isNull(root_a.get(Address_.user1)));
	
		final TypedQuery<Address> query = entityManager.createQuery(criteriaQuery);
		return query.getResultList();
	}
}