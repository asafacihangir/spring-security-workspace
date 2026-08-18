package org.phoenix.springsecurity.domain;


import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;


@Entity
@Table(name = "persistent_logins")
public class PersistentLogin implements Serializable {

    @Id
    @Column(length = 64)
    private String series;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(name = "last_used", nullable = false)
    private Date lastUsed;


    public PersistentLogin() {
    }

    public PersistentLogin(PersistentRememberMeToken token) {
        this.series = token.getSeries();
        this.username = token.getUsername();
        this.token = token.getTokenValue();
        this.lastUsed = token.getDate();
    }


    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((series == null) ? 0 : series.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PersistentLogin other = (PersistentLogin) obj;
        if (series == null) {
            return other.series == null;
        } else return series.equals(other.series);
    }

    @Override
    public String toString() {
        return "PersistentLogin{series=" + series + ", username=" + username + ", lastUsed=" + lastUsed + "}";
    }

}
