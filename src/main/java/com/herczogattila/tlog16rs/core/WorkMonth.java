/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.herczogattila.tlog16rs.core.exceptions.NotNewDateException;
import com.herczogattila.tlog16rs.core.exceptions.NotTheSameMonthException;
import com.herczogattila.tlog16rs.core.exceptions.WeekendNotEnabledException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Attila
 */
public class WorkMonth {
    private final List<WorkDay> days;
    private final YearMonth date;
    
    /**
     * @param year
     * @param month 
     */
    public WorkMonth(int year, int month) {
        days = new ArrayList<>();
        date = YearMonth.of(year, month);
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
        return days.stream().noneMatch((d) -> (d.getActualDay().getDayOfMonth() == day.getActualDay().getDayOfMonth()));
    }

    /**
     * Decides, if this day should be in this month or it fits into an other month by date.
     * @param day
     * @return boolean
     */
    public boolean isSameMonth(WorkDay day) {
        return day.getActualDay().getMonthValue() == date.getMonthValue() && day.getActualDay().getYear() == date.getYear();
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
    }

    /**
     * Sum of the minPerTask values every day.
     * @return long
     */
    public long getSumPerMonth() {
        return days.stream().mapToLong(s -> s.getSumPerDay()).sum();
    }
    
    /**
     * Sum of the requiredMinPerDay values.
     * @return long
     */
    public long getRequiredMinPerMonth() {
        return days.stream().mapToLong(s -> s.getRequiredMinPerDay()).sum();
    }
    
    @JsonIgnore
    public int getYear() {
        return date.getYear();
    }
    
    @JsonIgnore
    public int getMonth() {
        return date.getMonthValue();
    }

    public String getDate() {
        return date.toString();
    }

    public List<WorkDay> getDays() {
        return days;
    }
}
