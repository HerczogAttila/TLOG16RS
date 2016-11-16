/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.resources;

import com.herczogattila.tlog16rs.core.DeleteTaskRB;
import com.herczogattila.tlog16rs.core.FinishingTaskRB;
import com.herczogattila.tlog16rs.core.ModifyTaskRB;
import com.herczogattila.tlog16rs.core.StartTaskRB;
import com.herczogattila.tlog16rs.core.Task;
import com.herczogattila.tlog16rs.core.TimeLogger;
import com.herczogattila.tlog16rs.core.WorkDay;
import com.herczogattila.tlog16rs.core.WorkDayRB;
import com.herczogattila.tlog16rs.core.WorkMonth;
import com.herczogattila.tlog16rs.core.WorkMonthRB;
import groovy.util.logging.Slf4j;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author Attila
 */
@Path("/timelogger")
@Slf4j
public class TLOG16RSResource {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TLOG16RSResource.class);
    
    private final TimeLogger timeLogger = new TimeLogger();
    
    private WorkMonth findWorkMonth(int year, int month) {
        for(WorkMonth wm : timeLogger.getMonths()) {
            if(wm.getYear() == year && wm.getMonth()== month)
                return wm;
        }
        
        return null;
    }
    
    private WorkMonth findOrCreateWorkMonth(int year, int month) {
        WorkMonth wm = findWorkMonth(year, month);
        if(wm == null) {
            wm = new WorkMonth(year, month);
            try {
                timeLogger.addMonth(wm);
            } catch(Exception e) { LOG.warn(e.getMessage()); }
        }
        
        return wm;
    }
    
    private WorkDay findWorkDay(int year, int month, int day) {
        WorkMonth wm = findOrCreateWorkMonth(year, month);
        for(WorkDay wd : wm.getDays()) {
            if(wd.getActualDay().getDayOfMonth() == day)
                return wd;
        }
        
        return null;
    }
    
    private WorkDay findOrCreateWorkDay(int year, int month, int day) {
        WorkMonth wm = findOrCreateWorkMonth(year, month);
        for(WorkDay wd : wm.getDays()) {
            if(wd.getActualDay().getDayOfMonth() == day)
                return wd;
        }
        
        try {
            WorkDay wd = new WorkDay(450, year, month, day);
            wm.addWorkDay(wd);

            return wd;
        } catch(Exception e) { LOG.warn(e.getMessage()); }
        
        return null;
    }
    
    private Task findTask(int year, int month, int day, String taskId) {
        WorkDay wd = findOrCreateWorkDay(year, month, day);
        for(Task t : wd.getTasks()) {
            if(t.getTaskId().equals(taskId))
                return t;
        }
        
        return null;
    }
    
    private Task findOrCreateTask(int year, int month, int day, String taskId, String startTime) {
        WorkDay wd = findOrCreateWorkDay(year, month, day);
        for(Task t : wd.getTasks()) {
            if(t.getTaskId().equals(taskId))
                return t;
        }
        
        try {
            Task t = new Task(taskId);
            t.setStartTime(startTime);
            wd.addTask(t);
            
            return t;
        } catch(Exception e) { LOG.warn(e.getMessage()); }
        
        return null;
    }
    
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getMenu() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<title>Time Logger Menu</title>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<a href='workmonths'>List months</a><br>");
        sb.append("<body>");
        sb.append("</html>");
        
        return sb.toString();
    }
    
    @Path("/workmonths/{year}")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getWorkMonth(@PathParam(value = "year") int year) {
        StringBuilder sb = new StringBuilder();
        sb.append("Months:").append("<br>");
        timeLogger.getMonths().stream().forEach((wd) -> {
            if(wd.getYear() == year) {
                sb.append("<a href='workmonths/").append(year).append("/").append(wd.getMonth()).append("'>").append(wd.getMonth()).append("</a><br>");
            }
        });
        
        return sb.toString();
    }
    
    @Path("/workmonths")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<WorkMonth> getWorkmonths() {
        return timeLogger.getMonths();
    }
    
    @Path("/workmonths")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkMonth addNewMonth(WorkMonthRB month) {
        try {
            WorkMonth workMonth = new WorkMonth(month.getYear(), month.getMonth());
            timeLogger.addMonth(workMonth);
            return workMonth;
        } catch(Exception e) { LOG.warn(e.getMessage()); }
        
        return null;
    }
    
    @Path("/workmonths/{year}/{month}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WorkMonth getWorkMonth(@PathParam(value = "year") int year, @PathParam(value = "month") int month) {
        return findOrCreateWorkMonth(year, month);
    }
    
    @Path("/workmonths/workdays")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkDay addNewDay(WorkDayRB day) {
        try {
            WorkMonth month = findOrCreateWorkMonth(day.getYear(), day.getMonth());

            WorkDay workDay = new WorkDay(day.getRequiredHours(), day.getYear(), day.getMonth(), day.getDay());
            month.addWorkDay(workDay);

            return workDay;
        } catch(Exception e) { LOG.warn(e.getMessage()); }
        
        return null;
    }
    
    @Path("/workmonths/{year}/{month}/{day}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WorkDay getWorkDay(@PathParam(value = "year") int year, @PathParam(value = "month") int month, @PathParam(value = "day") int day) {
        return findOrCreateWorkDay(year, month, day);
    }
    
    @Path("/workmonths/workdays/tasks/start")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void startTask(StartTaskRB startTask) {
        try {
            WorkDay day = findOrCreateWorkDay(startTask.getYear(), startTask.getMonth(), startTask.getDay());

            Task task = new Task(startTask.getTaskId());
            task.setStartTime(startTask.getStartTime());
            task.setComment(startTask.getComment());
            day.addTask(task);
        } catch(Exception e) { LOG.warn(e.getMessage()); }
    }
    
    @Path("/workmonths/workdays/tasks/finish")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void finishTask(FinishingTaskRB finishTask) {
        try {
            Task task = findOrCreateTask(finishTask.getYear(), finishTask.getMonth(), finishTask.getDay(), finishTask.getTaskId(), finishTask.getStartTime());
            task.setEndTime(finishTask.getEndTime());
        } catch(Exception e) { LOG.warn(e.getMessage()); }
    }
    
    @Path("/workmonths/workdays/tasks/modify")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void modifyTask(ModifyTaskRB modifyTask) {
        try {
            Task task = findOrCreateTask(modifyTask.getYear(), modifyTask.getMonth(), modifyTask.getDay(), modifyTask.getTaskId(), modifyTask.getStartTime());
            task.setTaskId(modifyTask.getNewTaskId());
            task.setComment(modifyTask.getNewComment());
            task.setStartTime(modifyTask.getNewStartTime());
            task.setEndTime(modifyTask.getNewEndTime());
        } catch(Exception e) { LOG.warn(e.getMessage()); }
    }
    
    @Path("/workmonths/workdays/tasks/delete")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteTask(DeleteTaskRB deleteTask) {
        try {
            WorkDay day = findOrCreateWorkDay(deleteTask.getYear(), deleteTask.getMonth(), deleteTask.getDay());
            Task t = findTask(deleteTask.getYear(), deleteTask.getMonth(), deleteTask.getDay(), deleteTask.getTaskId());
            if(t != null)
                day.getTasks().remove(t);
        } catch(Exception e) { LOG.warn(e.getMessage()); }
    }
    
    @Path("/workmonths/deleteall")
    @PUT
    public void deleteAllWorkmonths() {
        timeLogger.getMonths().clear();
    }
}
