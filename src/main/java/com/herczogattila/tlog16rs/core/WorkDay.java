/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.core;

import com.herczogattila.tlog16rs.core.exceptions.FutureWorkException;
import com.herczogattila.tlog16rs.core.exceptions.NegativeMinutesOfWorkException;
import com.herczogattila.tlog16rs.core.exceptions.NotMultipleQuarterHourException;
import com.herczogattila.tlog16rs.core.exceptions.NotSeparatedTimesException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Attila
 */
public class WorkDay {
    private List<Task> tasks;
    private long requiredMinPerDay;
    private LocalDate actualDay;
    
    /**
     * Default Constructor.
     */
    public WorkDay() { this(450); }
    
    /**
     * @param requiredMinPerDay 
     */
    public WorkDay(int requiredMinPerDay) {
        this(requiredMinPerDay, LocalDate.now().getYear() + ". " + LocalDate.now().getMonthValue() + ". " + LocalDate.now().getDayOfMonth());
    }
    
    /**
     * @param requiredMinPerDay
     * @param year
     * @param month
     * @param day 
     */
    public WorkDay(int requiredMinPerDay, int year, int month, int day) {
        this(requiredMinPerDay, year + ". " + month + ". " + day);
    }
    
    /**
     * @param requiredMinPerDay
     * @param actualDay 
     * @exception NegativeMinutesOfWorkException
     * @exception FutureWorkException
     */
    public WorkDay(int requiredMinPerDay, String actualDay) {
        if(requiredMinPerDay < 0)
            throw new NegativeMinutesOfWorkException();

        tasks = new ArrayList<>();
        this.requiredMinPerDay = requiredMinPerDay;
        this.actualDay = stringToLocalDate(actualDay);
        
        if(this.actualDay.compareTo(LocalDate.now()) > 0)
            throw new FutureWorkException();
    }

    /**
     * Convert date string to LocalDate.
     * @param date
     * @return LocalDate
     */
    public static LocalDate stringToLocalDate(String date) {
        String[] parts = date.split(". ");
        int y = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        int d = Integer.parseInt(parts[2]);
        return LocalDate.of(y, m, d);
    }

    /**
     * Method, which calculates the difference between requiredMinPerDay and sumPerDay.
     * @return long
     */
    public long getExtraMinPerDay() {
        return getSumPerDay() - requiredMinPerDay;
    }
    
    /**
     * Should be able to decide if a Task has a common time interval with any existing Task's time interval.
     * @param task
     * @return boolean
     */
    public boolean isSeparatedTime(Task task) {
        if(task.getEndTime() == null)
            return tasks.stream().noneMatch((t) -> (
                t.getEndTime() != null &&
                (t.getEndTime().isAfter(task.getStartTime()) && t.getStartTime().isBefore(task.getStartTime())))
            );
        
        return tasks.stream().noneMatch((t) -> (
                t.getEndTime() != null &&
                (t.getEndTime().isAfter(task.getStartTime()) && t.getStartTime().isBefore(task.getStartTime())) ||
                (t.getEndTime().isAfter(task.getEndTime()) && t.getStartTime().isBefore(task.getEndTime())) ||
                (t.getStartTime().equals(task.getStartTime()) && t.getEndTime().equals(task.getEndTime()))
            )
        );
    }
    
    /**
     * Add a task to the list of tasks, if length is multiple of the quarter hour and the task time intervals have no common parts.
     * @param task 
     * @exception NotMultipleQuarterHourException
     * @exception NotSeparatedTimesException
     */
    public void addTask(Task task) {
        if(!task.isMultipleQuarterHour())
            throw new NotMultipleQuarterHourException();

        if(!isSeparatedTime(task))
            throw new NotSeparatedTimesException();

        tasks.add(task);
    }
    
    /**
     * Decide if actual day is a weekday.
     * @return boolean
     */
    public boolean isWeekDay() {
        return !(actualDay.getDayOfWeek() == DayOfWeek.SUNDAY ||
                actualDay.getDayOfWeek() == DayOfWeek.SATURDAY);
    }

    /**
     * Sum of the minPerTask values every day.
     * @return long
     */
    public long getSumPerDay() {
        return tasks.stream().mapToLong(s -> s.getMinPerTask()).sum();
    }
    
    /**
     * @return LocalTime The latest tasks endTime.
     */
    public LocalTime lastTaskEndTime() {
        if(tasks.isEmpty())
            return null;
        
        return tasks.get(tasks.size() - 1).getEndTime();
    }

    /**
     * @return long The required min per day.
     * @exception NegativeMinutesOfWorkException
     */
    public long getRequiredMinPerDay() {
        if(requiredMinPerDay < 0)
            throw new NegativeMinutesOfWorkException();
        
        return requiredMinPerDay;
    }

    /**
     * Set the required min per day.
     * @param requiredMinPerDay
     */
    public void setRequiredMinPerDay(long requiredMinPerDay) {
        this.requiredMinPerDay = requiredMinPerDay;
    }

    /**
     * Set the actual day.
     * @param actualDay 
     * @exception FutureWorkException
     */
    public void setActualDay(LocalDate actualDay) {
        if(actualDay.compareTo(LocalDate.now()) > 0)
            throw new FutureWorkException();
        
        this.actualDay = actualDay;
    }

    public LocalDate getActualDay() {
        return actualDay;
    }

    public List<Task> getTasks() {
        return tasks;
    }
}
