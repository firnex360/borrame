package parcial.services;

import jakarta.persistence.EntityManager;
import parcial.logic.User;

public class UserServices extends GeneralClass<User> {

    private static UserServices instance;

    private UserServices() {
        super(User.class);
    }

    public static UserServices getInstance() {
        if (instance == null) {
            instance = new UserServices();
        }
        return instance;
    }

    /**
     *
     * @param username
     * @return
     */
    public User findByUsername(String username) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (Exception e) {
            return null; // User not found
        } finally {
            em.close();
        }
    }

}