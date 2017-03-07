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
    private transient LocalTime startTime;
    private transient LocalTime endTime;
    private String comment;
    
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
        
        if(this.taskId.isEmpty())
            throw new NoTaskIdException();
                
        if(!isValidTaskId())
            throw new InvalidTaskIdException();
        
        try {
            this.startTime = stringToLocalTime(startTime);
            this.endTime = stringToLocalTime(endTime);
        } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }

        if(this.startTime != null && this.endTime != null) {
            if(this.startTime.compareTo(this.endTime) > 0) {
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
        if(startTime == null || endTime == null)
            return 0;
        
        return MINUTES.between(startTime, endTime);
    }
    
    /**
     * @return String
     */
    @Override
    public String toString() {
        return taskId + "\t" + startTime + "\t" + endTime + "\t" + comment;
    }

    /**
     * Set the start time
     * @param hour
     * @param min 
     */
    public void setStartTime(int hour, int min) {
        startTime = LocalTime.of(hour, min);
    }

    /**
     * Set the start time.
     * @param startTime 
     */
    public void setStartTime(String startTime) {
        this.startTime = stringToLocalTime(startTime);
    }

    /**
     * Set the end time.
     * @param hour
     * @param min 
     */
    public void setEndTime(int hour, int min) {
        LocalTime newEndTime = LocalTime.of(hour, min);
        if(startTime != null && this.startTime.compareTo(newEndTime) > 0)
            throw new NotExpectedTimeOrderException();
        
        this.endTime = newEndTime;
    }

    /**
     * Set the end time.
     * @param endTime 
     */
    public void setEndTime(String endTime) {
        LocalTime newEndTime = stringToLocalTime(endTime);
        
        if(startTime != null && this.startTime.compareTo(newEndTime) > 0)
            throw new NotExpectedTimeOrderException();
        
        this.endTime = newEndTime;
    }

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
        if (endTime.isAfter(task.startTime) && startTime.isBefore(task.startTime))
            return true;
        
        if (endTime.isAfter(task.endTime) && startTime.isBefore(task.endTime))
            return true;
        
        return startTime.equals(task.startTime) && endTime.equals(task.endTime);
    }
}
