/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.resources;

import com.avaje.ebean.EbeanServer;
import com.herczogattila.tlog16rs.TLOG16RSConfiguration;
import com.herczogattila.tlog16rs.db.CreateDatabase;
import com.herczogattila.tlog16rs.core.DeleteTaskRB;
import com.herczogattila.tlog16rs.core.FinishingTaskRB;
import com.herczogattila.tlog16rs.core.ModifyTaskRB;
import com.herczogattila.tlog16rs.core.StartTaskRB;
import com.herczogattila.tlog16rs.core.Task;
import static com.herczogattila.tlog16rs.core.Task.stringToLocalTime;
import com.herczogattila.tlog16rs.core.TimeLogger;
import com.herczogattila.tlog16rs.core.WorkDay;
import com.herczogattila.tlog16rs.core.WorkDayRB;
import com.herczogattila.tlog16rs.core.WorkMonth;
import com.herczogattila.tlog16rs.core.WorkMonthRB;
import groovy.util.logging.Slf4j;
import java.time.LocalTime;
import java.util.List;
import javax.persistence.OptimisticLockException;
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
    
    private TimeLogger timeLogger;
    private final CreateDatabase database;
    private final EbeanServer ebeanServer;
    
    public TLOG16RSResource(TLOG16RSConfiguration config) {
        
        database = new CreateDatabase(config);
        ebeanServer = database.getEbeanServer();
        
        timeLogger = ebeanServer.find(TimeLogger.class, 1);
        
        if(timeLogger == null) {
            timeLogger = new TimeLogger();
            timeLogger.setId(1);
            ebeanServer.save(timeLogger);
        }
    }
    
    private WorkMonth findWorkMonth(int year, int month) {
        for(WorkMonth wm : timeLogger.getMonths()) {
            if(wm.getYear() == year && wm.getMonth() == month) {                
                return wm;
            }
        }
        
        return null;
    }
    
    private WorkMonth findOrCreateWorkMonth(int year, int month) {
        WorkMonth wm = findWorkMonth(year, month);
        if(wm == null) {
            wm = new WorkMonth(year, month);
            try {
                timeLogger.addMonth(wm);
                ebeanServer.save(timeLogger);
            } catch(OptimisticLockException e) { LOG.warn(e.getMessage()); }
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
            
            ebeanServer.save(timeLogger);

            return wd;
        } catch(RuntimeException e) { LOG.warn(e.getMessage()); }
        
        return null;
    }
    
    private Task findTask(int year, int month, int day, String taskId, String startTime) {
        LocalTime start = stringToLocalTime(startTime);
        WorkDay wd = findOrCreateWorkDay(year, month, day);
        for(Task t : wd.getTasks()) {
            if(t.getTaskId().equals(taskId) && t.getStartTime().equals(start))
                return t;
        }
        
        return null;
    }
    
    private Task findOrCreateTask(int year, int month, int day, String taskId, String startTime) {
        LocalTime start = stringToLocalTime(startTime);
        WorkDay wd = findOrCreateWorkDay(year, month, day);
        for(Task t : wd.getTasks()) {
            if(t.getTaskId().equals(taskId) && t.getStartTime().equals(start))
                return t;
        }
        
        try {
            Task t = new Task(taskId);
            t.setStartTime(startTime);
            wd.addTask(t);
            
            ebeanServer.save(timeLogger);
            
            return t;
        } catch(RuntimeException e) { LOG.warn(e.getMessage()); }
        
        return null;
    }
    
    @Path("/workmonths")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<WorkMonth> getWorkmonths() {
        //return timeLogger.getMonths();
        return ebeanServer.find(WorkMonth.class).findList();
        //return ebeanServer.find(TimeLogger.class, 1).getMonths();
    }
    
    @Path("/workmonths")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkMonth addNewMonth(WorkMonthRB month) {
        
        
        try {
            WorkMonth workMonth = new WorkMonth(month.getYear(), month.getMonth());
            timeLogger.addMonth(workMonth);
            
            ebeanServer.save(timeLogger);
            
            return workMonth;
        } catch(RuntimeException e) { LOG.warn(e.getMessage()); }
        
        return null;
    }
    
    @Path("/workmonths/{year}/{month}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<WorkDay> getWorkMonth(@PathParam(value = "year") int year, @PathParam(value = "month") int month) {
        return findOrCreateWorkMonth(year, month).getDays();
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
            
            ebeanServer.save(timeLogger);

            return workDay;
        } catch(RuntimeException e) { LOG.warn(e.getMessage()); }
        
        return null;
    }
    
    @Path("/workmonths/{year}/{month}/{day}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Task> getWorkDay(@PathParam(value = "year") int year, @PathParam(value = "month") int month, @PathParam(value = "day") int day) {
        return findOrCreateWorkDay(year, month, day).getTasks();
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
            
            ebeanServer.save(timeLogger);
            
        } catch(RuntimeException e) { LOG.warn(e.getMessage()); }
    }
    
    @Path("/workmonths/workdays/tasks/finish")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void finishTask(FinishingTaskRB finishTask) {
        try {
            Task task = findOrCreateTask(finishTask.getYear(), finishTask.getMonth(), finishTask.getDay(), finishTask.getTaskId(), finishTask.getStartTime());
            task.setEndTime(finishTask.getEndTime());
            
            ebeanServer.save(timeLogger);
            
        } catch(RuntimeException e) { LOG.warn(e.getMessage()); }
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

            ebeanServer.save(timeLogger);
            
        } catch(RuntimeException e) { LOG.warn(e.getMessage()); }
    }
    
    @Path("/workmonths/workdays/tasks/delete")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteTask(DeleteTaskRB deleteTask) {
        try {
            WorkDay day = findOrCreateWorkDay(deleteTask.getYear(), deleteTask.getMonth(), deleteTask.getDay());
            Task t = findTask(deleteTask.getYear(), deleteTask.getMonth(), deleteTask.getDay(), deleteTask.getTaskId(), deleteTask.getStartTime());
            if(t != null)
                day.getTasks().remove(t);
            
            ebeanServer.save(timeLogger);
            
        } catch(RuntimeException e) { LOG.warn(e.getMessage()); }
    }
    
    @Path("/workmonths/deleteall")
    @PUT
    public void deleteAllWorkmonths() {
        ebeanServer.delete(TimeLogger.class, 1);
        TimeLogger tl = new TimeLogger();
        tl.setId(1);
        ebeanServer.save(tl);
        //timeLogger.getMonths().clear();
        timeLogger = ebeanServer.find(TimeLogger.class, 1);
    }
}

