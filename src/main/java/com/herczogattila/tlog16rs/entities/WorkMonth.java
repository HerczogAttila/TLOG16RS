/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.herczogattila.tlog16rs.core.exceptions.NotNewDateException;
import com.herczogattila.tlog16rs.core.exceptions.NotTheSameMonthException;
import com.herczogattila.tlog16rs.core.exceptions.WeekendNotEnabledException;
import java.io.Serializable;
import java.time.YearMonth;
import java.util.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 *
 * @author Attila
 */
@Entity
@lombok.Getter
@lombok.Setter
public class WorkMonth {    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @JsonIgnore
    private int id;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkDay> days;
    @Transient
    private transient YearMonth yearMonth;
    private String date;
    
    private long extraMinPerMonth;
    private long sumPerMonth;
    private long requiredMinPerMonth;
    
    public WorkMonth() {
        days = new ArrayList();
    }
    
    /**
     * @param year
     * @param month 
     */
    public WorkMonth(int year, int month) {
        days = new ArrayList<>();
        yearMonth = YearMonth.of(year, month);
        date = yearMonth.toString();
        
        extraMinPerMonth = getExtraMinPerMonth();
        sumPerMonth = getSumPerMonth();
        requiredMinPerMonth = getRequiredMinPerMonth();
    }
    
    public static YearMonth stringToYearMonth(String date) {
        String[] parts = date.split("-");
        int y = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        return YearMonth.of(y, m);
    }
    
    /**
     * Calculate, how many extra minutes did the employee work in the actual month.
     * @return long
     */
    public long getExtraMinPerMonth() {
        return getSumPerMonth() - getRequiredMinPerMonth();
    }

    /**
     * Decides if this day is already existing or not.
     * @param day
     * @return boolean
     */
    public boolean isNewDate(WorkDay day) {
        return days.stream().noneMatch(d -> d.getActualDay().getDayOfMonth() == day.getActualDay().getDayOfMonth());
    }

    /**
     * Decides, if this day should be in this month or it fits into an other month by date.
     * @param day
     * @return boolean
     */
    public boolean isSameMonth(WorkDay day) {
        return day.getActualDay().getMonthValue() == getMonth() && day.getActualDay().getYear() == getYear();
    }

    /**
     * Add a day to the list of days.
     * @param day 
     * @exception NotNewDateException
     * @exception NotTheSameMonthException
     * @exception WeekendNotEnabledException
     */
    public void addWorkDay(WorkDay day) { addWorkDay(day, false); }
    
    /**
     * Add a day to the list of days.
     * @param day
     * @param isWeekendEnabled 
     * @exception NotNewDateException
     * @exception NotTheSameMonthException
     * @exception WeekendNotEnabledException
     */
    public void addWorkDay(WorkDay day, boolean isWeekendEnabled) {
        if(!isNewDate(day))
            throw new NotNewDateException();
        
        if(!isSameMonth(day))
            throw new NotTheSameMonthException();
        
        if(!isWeekendEnabled && !day.isWeekDay()) 
            throw new WeekendNotEnabledException();
        
        days.add(day);
        
        extraMinPerMonth = getExtraMinPerMonth();
        sumPerMonth = getSumPerMonth();
        requiredMinPerMonth = getRequiredMinPerMonth();
    }

    /**
     * Sum of the minPerTask values every day.
     * @return long
     */
    public long getSumPerMonth() {
        return days.stream().mapToLong(WorkDay::getSumPerDay).sum();
    }
    
    /**
     * Sum of the requiredMinPerDay values.
     * @return long
     */
    public long getRequiredMinPerMonth() {
        return days.stream().mapToLong(WorkDay::getRequiredMinPerDay).sum();
    }
    
    @JsonIgnore
    public int getYear() {
        if(yearMonth == null)
            yearMonth = stringToYearMonth(date);
        
        return yearMonth.getYear();
    }
    
    @JsonIgnore
    public int getMonth() {
        if(yearMonth == null)
            yearMonth = stringToYearMonth(date);
        
        return yearMonth.getMonthValue();
    }
}
