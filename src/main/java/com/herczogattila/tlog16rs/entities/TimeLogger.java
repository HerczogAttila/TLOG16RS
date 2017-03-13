/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.herczogattila.tlog16rs.core.exceptions.NotNewMonthException;
import java.io.Serializable;
import java.util.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 *
 * @author Attila
 */
@Entity
@lombok.Getter
@lombok.Setter
public class TimeLogger implements Serializable {
    @Id
    @GeneratedValue
    private int id;
    private String name;
    private String password;
    private String salt;
        
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkMonth> months;

    public TimeLogger() { this("default"); }
    
    public TimeLogger(String name) {
        months = new ArrayList();
        this.name = name;
    }

    public boolean isNewMonth(WorkMonth month) {
        return months.stream().noneMatch(d -> d.getMonth() == month.getMonth() &&
                d.getYear() == month.getYear());
    }

    public void addMonth(WorkMonth month) {
        if(!isNewMonth(month))
            throw new NotNewMonthException();
        
        months.add(month);
    }
}
