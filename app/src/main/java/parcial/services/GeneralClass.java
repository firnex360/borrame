package parcial.services;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaQuery;

@Entity
public class GeneralClass<T> {

    //private static final Map<Class<?>, Map<Object, Object>> database = new HashMap<>();
    private static EntityManagerFactory emf;
    private final Class<T> entityClass;
    
    public GeneralClass(Class<T> entityClass) {

        if(emf == null) {
            emf = Persistence.createEntityManagerFactory("PersistenceUnit");
            
        }
        this.entityClass = entityClass;
    }


    public EntityManager getEntityManager(){
        return emf.createEntityManager();
    }


    /**
     *
     * @param entity
     */
    public T create(T entity) throws IllegalArgumentException, EntityExistsException, PersistenceException{
        EntityManager em = getEntityManager();

        try {

            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();

        }finally {
            em.close();
        }
        return entity;
    }

    /**
     * @param entity
     * @return
     */
    public T edit(T entity) throws PersistenceException {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        try {
            em.merge(entity);
            em.getTransaction().commit();
        }finally {
            em.close();
        }
        return entity;
    }


    /**
     * @param entityId
     * @return
     */
    public boolean delete(Object entityId) throws PersistenceException {
        boolean deleted = false;
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        try {
            T entity = em.find(entityClass, entityId);
            em.remove(entity);
            em.getTransaction().commit();
            deleted = true;
        }finally {
            em.close();
        }
        return deleted;
    }


    
    /**
     * @param id
     * @return
     */
    public T find(Object id) throws PersistenceException{

        if (id == null) {
            return null;
        }

        EntityManager em = getEntityManager();
        try{
            return em.find(entityClass, id);
        } finally {
            em.close();
        }
    }


    /**
     * @return
     */
    public List<T> findAll() throws PersistenceException{
        EntityManager em = getEntityManager();
        try{
            CriteriaQuery<T> criteriaQuery = em.getCriteriaBuilder().createQuery(entityClass);
            criteriaQuery.select(criteriaQuery.from(entityClass));
            return em.createQuery(criteriaQuery).getResultList();
        } finally {
            em.close();
        }
    }
}
