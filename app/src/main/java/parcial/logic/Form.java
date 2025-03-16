package parcial.logic;

import java.io.Serializable;

import jakarta.persistence.*;


@Entity
public class Form implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String sector;
    private String scholarLevel;
    private String latitude;
    private String longitude; 
    @ManyToOne
    private User user;

    public Form() {
    }

    public Form(String name, String sector, String scholarLevel, String latitude, String longitude, User user) {
        this.name = name;
        this.sector = sector;
        this.scholarLevel = scholarLevel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.user = user;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getSector() {
        return sector;
    }
    public void setSector(String sector) {
        this.sector = sector;
    }
    public String getScholarLevel() {
        return scholarLevel;
    }
    public void setScholarLevel(String scholarLevel) {
        this.scholarLevel = scholarLevel;
    }
    public String getLatitude() {
        return latitude;
    }
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }
    public String getLongitude() {
        return longitude;
    }
    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    
}
