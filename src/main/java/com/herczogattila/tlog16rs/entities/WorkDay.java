package com.herczogattila.tlog16rs.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.herczogattila.tlog16rs.core.exceptions.FutureWorkException;
import com.herczogattila.tlog16rs.core.exceptions.NegativeMinutesOfWorkException;
import com.herczogattila.tlog16rs.core.exceptions.NotMultipleQuarterHourException;
import com.herczogattila.tlog16rs.core.exceptions.NotSeparatedTimesException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

@Entity
@lombok.Getter
@lombok.Setter
public final class WorkDay {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @JsonIgnore
    private int id;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Task> tasks;
    private long requiredMinPerDay;
    @Transient
    @JsonIgnore
    private transient LocalDate actualDay;
    private String date;
    
    private long extraMinPerDay;
    private long sumMinPerDay;
    
    public WorkDay() { this(450); }
    
    public WorkDay(int requiredMinPerDay) {
        this(requiredMinPerDay, LocalDate.now().getYear() + ". " + LocalDate.now().getMonthValue() + ". " + LocalDate.now().getDayOfMonth());
    }
    
    public WorkDay(int requiredMinPerDay, int year, int month, int day) {
        this(requiredMinPerDay, year + ". " + month + ". " + day);
    }

    public WorkDay(int requiredMinPerDay, String actualDay) {
        if(requiredMinPerDay < 0)
            throw new NegativeMinutesOfWorkException();

        tasks = new ArrayList();
        this.requiredMinPerDay = requiredMinPerDay;
        this.actualDay = stringToLocalDate(actualDay);
        date = actualDay;
        
        if(this.actualDay.compareTo(LocalDate.now()) > 0)
            throw new FutureWorkException();
        
        Refresh();
    }

    public static LocalDate stringToLocalDate(String date) {
        String[] parts = date.split(". ");
        int y = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        int d = Integer.parseInt(parts[2]);
        return LocalDate.of(y, m, d);
    }

    public long getExtraMinPerDay() {
        return getSumPerDay() - requiredMinPerDay;
    }

    public boolean isSeparatedTime(Task task) {
        if(tasks.stream().anyMatch(t -> t.getEndTime() == null ||
                t.getStartTime().compareTo(task.getStartTime()) == 0 ||
                (task.getEndTime() != null &&
                t.getStartTime().isAfter(task.getStartTime()) &&
                t.getEndTime().isBefore(task.getEndTime()))
                )
            )
            return false;
        
        if(task.getEndTime() == null) {
            return tasks.stream().noneMatch(t -> 
                (t.getEndTime().isAfter(task.getStartTime()) &&
                t.getStartTime().isBefore(task.getStartTime()))
            );
        }
        
        return tasks.stream().noneMatch(t -> t.isSeparatedTime(task));
    }

    public void addTask(Task task) {
        if(!task.isMultipleQuarterHour())
            throw new NotMultipleQuarterHourException();

        if(!isSeparatedTime(task))
            throw new NotSeparatedTimesException();

        tasks.add(task);
        
        Refresh();
    }
    
    @JsonIgnore
    public boolean isWeekDay() {
        return !(actualDay.getDayOfWeek() == DayOfWeek.SUNDAY ||
                actualDay.getDayOfWeek() == DayOfWeek.SATURDAY);
    }

    @JsonIgnore
    public long getSumPerDay() {
        return tasks.stream().mapToLong(Task::getMinPerTask).sum();
    }

    public LocalTime lastTaskEndTime() {
        if(tasks.isEmpty())
            return null;
        
        return tasks.get(tasks.size() - 1).getEndTime();
    }
    
    public long getRequiredMinPerDay() {
        if(requiredMinPerDay < 0)
            throw new NegativeMinutesOfWorkException();
        
        return requiredMinPerDay;
    }
    
    public void setRequiredMinPerDay(long requiredMinPerDay) {
        if(requiredMinPerDay < 0)
            throw new NegativeMinutesOfWorkException();
        
        this.requiredMinPerDay = requiredMinPerDay;
    }

    public void setActualDay(LocalDate actualDay) {
        if(actualDay.compareTo(LocalDate.now()) > 0)
            throw new FutureWorkException();
        
        this.actualDay = actualDay;
        date = actualDay.toString();
    }
    
    public void Refresh() {
        extraMinPerDay = getExtraMinPerDay();
        sumMinPerDay = getSumPerDay();
    }
    
    @JsonIgnore
    public int getDayOfMonth() {
        String[] fields = date.split(". ");
        if(fields.length >= 3) {
            return Integer.parseInt(fields[2]);
        }
        
        return 0;
    }
}
