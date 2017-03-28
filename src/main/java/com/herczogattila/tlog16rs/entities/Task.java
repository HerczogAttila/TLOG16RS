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
import com.herczogattila.tlog16rs.core.exceptions.NotMultipleQuarterHourException;
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
public final class Task {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Task.class);
    
    @Id
    @GeneratedValue
    @JsonIgnore
    private Integer id;
    private String taskId;
    private String comment;
    private String startingTime;
    private String endingTime;
    
    private long sumMinPerDay;

    public Task() { this("1234"); }
    
    public Task(String taskId) {
        this.taskId = taskId;
        comment = "";
        
        if(this.taskId.isEmpty())
            throw new NoTaskIdException();

        if(!isValidTaskId())
            throw new InvalidTaskIdException();
    }
    
    public Task(String taskId, String comment, int startHour, int startMinute, int endHour, int endMinute) {
        this(taskId, comment, startHour + ":" + startMinute, endHour + ":" + endMinute);
    }
    
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

    public static LocalTime stringToLocalTime(String time) {
        if(time == null)
            return null;
        
        String[] parts = time.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        return LocalTime.of(h, m);
    }
    
    @JsonIgnore
    public boolean isValidTaskId() {
        return isValidRedmineTaskId() || isValidLTTaskId();
    }

    @JsonIgnore
    public boolean isValidRedmineTaskId() {
        if(taskId.isEmpty())
            throw new NoTaskIdException();
        
        return taskId.matches("^\\d{4}");
    }
    
    @JsonIgnore
    public boolean isValidLTTaskId() {
        if(taskId.isEmpty())
            throw new NoTaskIdException();
        
        return taskId.matches("^LT-\\d{4}");
    }
    
    @JsonIgnore
    public boolean isMultipleQuarterHour() {
        return getMinPerTask() % 15 == 0 && getMinPerTask() != 0;
    }
    
    public long getMinPerTask() {
        if(getStartTime() == null || getEndTime() == null)
            return 15;
        
        return MINUTES.between(getStartTime(), getEndTime());
    }

    @Override
    public String toString() {
        return taskId + "\t" + startingTime + "\t" + endingTime + "\t" + comment;
    }

    public void setStartTime(int hour, int min) {
        startingTime = hour + ":" + min;
    }

    public void setEndTime(int hour, int min) {
        LocalTime newEndTime = LocalTime.of(hour, min);
        if(getStartTime() != null && this.getEndTime().compareTo(newEndTime) > 0)
            throw new NotExpectedTimeOrderException();
        
        endingTime = hour + ":" + min;
    }

    public void setEndTime(String endTime) {
        if(endTime == null)
            return;
        
        LocalTime newEndTime = stringToLocalTime(endTime);
        
        if(getStartTime() != null && this.getStartTime().compareTo(newEndTime) > 0)
            throw new NotExpectedTimeOrderException();
        
        if(getStartTime() != null && MINUTES.between(getStartTime(), newEndTime) == 0)
            throw new NotMultipleQuarterHourException();
        
        endingTime = endTime;
    }
    
    @JsonIgnore
    public LocalTime getStartTime() { return stringToLocalTime(startingTime); }
    @JsonIgnore
    public LocalTime getEndTime() { return stringToLocalTime(endingTime); }

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
    
    public void Refresh() {
        this.sumMinPerDay = getMinPerTask();
    }
}
