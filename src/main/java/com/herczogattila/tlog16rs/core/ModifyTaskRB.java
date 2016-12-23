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
@lombok.Getter
@lombok.Setter
public class ModifyTaskRB {
    private int year, month, day;
    private String taskId, startTime, newTaskId, newStartTime, newEndTime, newComment;
}
