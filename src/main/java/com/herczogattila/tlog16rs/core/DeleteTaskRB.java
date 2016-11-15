/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.core;

/**
 *
 * @author Attila
 */
public class DeleteTaskRB {
    private int year, month, day;
    private String taskId, startTime;

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getStartTime() {
        return startTime;
    }
}
