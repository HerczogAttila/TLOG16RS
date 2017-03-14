/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.herczogattila.tlog16rs.core.exceptions.EmptyTimeFieldException;
import com.herczogattila.tlog16rs.core.exceptions.InvalidTaskIdException;
import com.herczogattila.tlog16rs.core.exceptions.NoTaskIdException;
import com.herczogattila.tlog16rs.core.exceptions.NotExpectedTimeOrderException;
import java.time.LocalTime;
import static java.time.temporal.ChronoUnit.MINUTES;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 *
 * @author Attila
 */
@Entity
@lombok.Getter
@lombok.Setter
public class Task {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Task.class);
    
    @Id
    @GeneratedValue
    private Integer id;
    private String taskId;
    private String comment;
    private String startingTime;
    private String endingTime;
    
    private long sumMinPerDay;

    public Task() { this("1234"); }
    
    /**
     * @param taskId 
     * @exception NoTaskIdException
     * @exception InvalidTaskIdException
     * @exception NotExpectedTimeOrderException
     * @exception EmptyTimeFieldException
     */
    public Task(String taskId) {
        this.taskId = taskId;
        comment = "";
        
        if(this.taskId.isEmpty())
            throw new NoTaskIdException();

        if(!isValidTaskId())
            throw new InvalidTaskIdException();
    }
    
    /**
     * @param taskId
     * @param comment
     * @param startHour
     * @param startMinute
     * @param endHour
     * @param endMinute 
     * @exception NoTaskIdException
     * @exception InvalidTaskIdException
     * @exception NotExpectedTimeOrderException
     * @exception EmptyTimeFieldException
     */
    public Task(String taskId, String comment, int startHour, int startMinute, int endHour, int endMinute) {
        this(taskId, comment, startHour + ":" + startMinute, endHour + ":" + endMinute);
    }
    
    /**
     * @param taskId
     * @param comment
     * @param startTime
     * @param endTime 
     * @exception NoTaskIdException
     * @exception InvalidTaskIdException
     * @exception NotExpectedTimeOrderException
     * @exception EmptyTimeFieldException
     */
    public Task(String taskId, String comment, String startTime, String endTime) {
        this.taskId = taskId;
        this.comment = comment;
        startingTime = startTime;
        endingTime = endTime;
        
        if(this.taskId.isEmpty())
            throw new NoTaskIdException();
                
        if(!isValidTaskId())
            throw new InvalidTaskIdException();

        if(getStartTime() != null && getEndTime() != null) {
            if(getStartTime().compareTo(getEndTime()) > 0) {
                throw new NotExpectedTimeOrderException();
            }
        } else {
            throw new EmptyTimeFieldException();
        }
        
        sumMinPerDay = getMinPerTask();
    }
    
    /**
     * Convert time string to LocalTime.
     * @param time
     * @return 
     */
    public static LocalTime stringToLocalTime(String time) {
        if(time == null)
            return null;
        
        String[] parts = time.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        return LocalTime.of(h, m);
    }
    
    /**
     * @return boolean
     */
    @JsonIgnore
    public boolean isValidTaskId() {
        return isValidRedmineTaskId() || isValidLTTaskId();
    }
    
    /**
     * @return boolean
     * @exception NoTaskIdException
     */
    @JsonIgnore
    public boolean isValidRedmineTaskId() {
        if(taskId.isEmpty())
            throw new NoTaskIdException();
        
        return taskId.matches("^\\d{4}");
    }
    
    /**
     * @return boolean
     * @exception NoTaskIdException
     */
    @JsonIgnore
    public boolean isValidLTTaskId() {
        if(taskId.isEmpty())
            throw new NoTaskIdException();
        
        return taskId.matches("^LT-\\d{4}");
    }
    
    /**
     * Time interval should be the multiple of the quarter hour.
     * @return 
     */
    @JsonIgnore
    public boolean isMultipleQuarterHour() {
        return getMinPerTask() % 15 == 0;
    }
    
    /**
     * Calculate the duration of a task. If start or end time is null, then return zero.
     * @return long
     */
    public long getMinPerTask() {
        if(getStartTime() == null || getEndTime() == null)
            return 0;
        
        return MINUTES.between(getStartTime(), getEndTime());
    }
    
    /**
     * @return String
     */
    @Override
    public String toString() {
        return taskId + "\t" + startingTime + "\t" + endingTime + "\t" + comment;
    }

    /**
     * Set the start time
     * @param hour
     * @param min 
     */
    public void setStartTime(int hour, int min) {
        startingTime = hour + ":" + min;
    }

    /**
     * Set the end time.
     * @param hour
     * @param min 
     */
    public void setEndTime(int hour, int min) {
        LocalTime newEndTime = LocalTime.of(hour, min);
        if(getStartTime() != null && this.getEndTime().compareTo(newEndTime) > 0)
            throw new NotExpectedTimeOrderException();
        
        endingTime = hour + ":" + min;
    }

    /**
     * Set the end time.
     * @param endTime 
     */
    public void setEndTime(String endTime) {
        if(endTime == null)
            return;
        
        LocalTime newEndTime = stringToLocalTime(endTime);
        
        if(getStartTime() != null && this.getStartTime().compareTo(newEndTime) > 0)
            throw new NotExpectedTimeOrderException();
        
        endingTime = endTime;
    }
    
    public void setEndingTime(String endTime) {
        if(endTime == null)
            return;
        
        LocalTime newEndTime = stringToLocalTime(endTime);
        
        if(getStartTime() != null && this.getStartTime().compareTo(newEndTime) > 0)
            throw new NotExpectedTimeOrderException();
        
        endingTime = endTime;
    }
    
    public LocalTime getStartTime() { return stringToLocalTime(startingTime); }
    public LocalTime getEndTime() { return stringToLocalTime(endingTime); }

    /**
     * @return String The task id.
     * @exception NoTaskIdException 
     */
    public String getTaskId() {
        if(this.taskId.isEmpty())
            throw new NoTaskIdException();
        
        return taskId;
    }
    
    public boolean isSeparatedTime(Task task) {
        if (getEndTime().isAfter(task.getStartTime()) && getStartTime().isBefore(task.getStartTime()))
            return true;
        
        if (getEndTime().isAfter(task.getEndTime()) && getStartTime().isBefore(task.getEndTime()))
            return true;
        
        return getStartTime().equals(task.getStartTime()) && getEndTime().equals(task.getEndTime());
    }
}
