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
import com.herczogattila.tlog16rs.core.ModifyWorkDayRB;
import com.herczogattila.tlog16rs.core.StartTaskRB;
import com.herczogattila.tlog16rs.core.UserRB;
import com.herczogattila.tlog16rs.entities.Task;
import static com.herczogattila.tlog16rs.entities.Task.stringToLocalTime;
import com.herczogattila.tlog16rs.entities.TimeLogger;
import com.herczogattila.tlog16rs.entities.WorkDay;
import com.herczogattila.tlog16rs.core.WorkDayRB;
import com.herczogattila.tlog16rs.entities.WorkMonth;
import com.herczogattila.tlog16rs.core.WorkMonthRB;
import groovy.util.logging.Slf4j;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import java.security.InvalidKeyException;
import java.security.Key;
import javax.ws.rs.HeaderParam;

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
            timeLogger = new TimeLogger("default");
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
            } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
        }
        
        return wm;
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
        } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
        
        return null;
    }
    
    private Task findTask(int year, int month, int day, String taskId, String startTime) {
        LocalTime start = stringToLocalTime(startTime);
        WorkDay wd = findOrCreateWorkDay(year, month, day);
        if(wd == null)
            return null;
        
        for(Task t : wd.getTasks()) {
            if(t.getTaskId().equals(taskId) && t.getStartTime().equals(start))
                return t;
        }
        
        return null;
    }
    
    private Task findOrCreateTask(int year, int month, int day, String taskId, String startTime) {
        LocalTime start = stringToLocalTime(startTime);
        WorkDay wd = findOrCreateWorkDay(year, month, day);
        if(wd == null)
            return null;
        
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
        } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
        
        return null;
    }
    
    @Path("/workmonths")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkmonths(@HeaderParam("Authorization") String authorization) {
        return Response.ok(timeLogger.getMonths()).build();
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
        } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
        
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
        } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
        
        return null;
    }
    
    @Path("/workmonths/workdaysweekend")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkDay addNewWeekendDay(WorkDayRB day) {
        try {
            WorkMonth month = findOrCreateWorkMonth(day.getYear(), day.getMonth());

            WorkDay workDay = new WorkDay(day.getRequiredHours(), day.getYear(), day.getMonth(), day.getDay());
            month.addWorkDay(workDay, true);
            
            ebeanServer.save(timeLogger);

            return workDay;
        } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
        
        return null;
    }
    
    @Path("/workmonths/workdays/{year}/{month}/{day}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WorkDay getWorkDay(@PathParam(value = "year") int year, @PathParam(value = "month") int month, @PathParam(value = "day") int day) {
        try {
            WorkDay workDay = findOrCreateWorkDay(year, month, day);
            if (workDay != null) {
                return workDay;
            }
        } catch(RuntimeException e) {
            LOG.warn(e.getMessage(), e);
        }
        
        return null;
    }
    
    @Path("/workmonths/workdays/modify")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkDay modifyWorkDay(ModifyWorkDayRB modifyWorkDay) {
        try {
            WorkDay workDay = findOrCreateWorkDay(modifyWorkDay.getYear(), modifyWorkDay.getMonth(), modifyWorkDay.getDay());
            if (workDay != null) {
                workDay.setRequiredMinPerDay(modifyWorkDay.getRequiredMinutes());
                workDay.Refresh();
                return workDay;
            }
        } catch(RuntimeException e) {
            LOG.warn(e.getMessage(), e);
        }
        
        return null;
    }
    
    @Path("/workmonths/{year}/{month}/{day}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Task> getWorkDayTasks(@PathParam(value = "year") int year, @PathParam(value = "month") int month, @PathParam(value = "day") int day) {
        WorkDay wd = findOrCreateWorkDay(year, month, day);
        if (wd != null)
            return wd.getTasks();
        
        return new ArrayList<>();
    }
    
    @Path("/workmonths/workdays/tasks/start")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void startTask(StartTaskRB startTask) {
        try {
            WorkDay day = findOrCreateWorkDay(startTask.getYear(), startTask.getMonth(), startTask.getDay());
            if(day == null)
                return;

            Task task = new Task(startTask.getTaskId());
            task.setStartTime(startTask.getStartTime());
            task.setComment(startTask.getComment());
            day.addTask(task);
            
            ebeanServer.save(timeLogger);
            
        } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
    }
    
    @Path("/workmonths/workdays/tasks/finish")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void finishTask(FinishingTaskRB finishTask) {
        try {
            Task task = findTask(finishTask.getYear(), finishTask.getMonth(), finishTask.getDay(), finishTask.getTaskId(), finishTask.getStartTime());
            if(task == null) {
                WorkDay wd = findOrCreateWorkDay(finishTask.getYear(), finishTask.getMonth(), finishTask.getDay());
                if(wd != null) {
                    task = new Task(finishTask.getTaskId(), "", finishTask.getStartTime(), finishTask.getEndTime());
                    wd.addTask(task);
                }
            } else {
                task.setEndTime(finishTask.getEndTime());
                WorkDay wd = findOrCreateWorkDay(finishTask.getYear(), finishTask.getMonth(), finishTask.getDay());
                if(wd != null) {
                    wd.Refresh();
                }
            }
            
            ebeanServer.save(timeLogger);
            
        } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
    }
    
    @Path("/workmonths/workdays/tasks/modify")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void modifyTask(ModifyTaskRB modifyTask) {
        try {
            Task task = findOrCreateTask(modifyTask.getYear(), modifyTask.getMonth(), modifyTask.getDay(), modifyTask.getTaskId(), modifyTask.getStartTime());
            if(task == null)
                return;
            
            task.setTaskId(modifyTask.getNewTaskId());
            task.setComment(modifyTask.getNewComment());
            task.setStartTime(modifyTask.getNewStartTime());
            task.setEndTime(modifyTask.getNewEndTime());
            
            WorkDay wd = findOrCreateWorkDay(modifyTask.getYear(), modifyTask.getMonth(), modifyTask.getDay());
            if(wd != null) {
                wd.Refresh();
            }

            ebeanServer.save(timeLogger);
            
        } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
    }
    
    @Path("/workmonths/workdays/tasks/delete")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteTask(DeleteTaskRB deleteTask) {
        try {
            WorkDay day = findOrCreateWorkDay(deleteTask.getYear(), deleteTask.getMonth(), deleteTask.getDay());
            if(day == null)
                return;
            
            Task t = findTask(deleteTask.getYear(), deleteTask.getMonth(), deleteTask.getDay(), deleteTask.getTaskId(), deleteTask.getStartTime());
            if(t != null)
                day.getTasks().remove(t);
            
            day.Refresh();
            
            ebeanServer.save(timeLogger);
            
        } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
    }
    
    @Path("/workmonths/deleteall")
    @PUT
    public void deleteAllWorkmonths() {
        ebeanServer.delete(TimeLogger.class, 1);
        TimeLogger tl = new TimeLogger("cleared");
        tl.setId(1);
        ebeanServer.save(tl);
        timeLogger = ebeanServer.find(TimeLogger.class, 1);
    }
    
    @Path("/registering")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registering(UserRB user) throws NoSuchAlgorithmException {
        List<TimeLogger> users = ebeanServer.find(TimeLogger.class)
                .where()
                .eq("name", user.getName())
                .findList();
        
        if(users.isEmpty()) {
            TimeLogger newUser = new TimeLogger(user.getName());
            
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            newUser.setSalt(String.format("%064x", new java.math.BigInteger(1, salt)));
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String password = user.getPassword() + newUser.getSalt();
            
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            newUser.setPassword(String.format("%064x", new java.math.BigInteger(1, hash)));
            
            ebeanServer.save(newUser);
            return Response.ok().build();
        } else {
            return Response.ok().status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Path("/authenticate")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response authenticate(UserRB user) throws NoSuchAlgorithmException, InvalidKeyException {
        List<TimeLogger> users = ebeanServer.find(TimeLogger.class)
                .where()
                .eq("name", user.getName())
                .findList();
        
        if(!users.isEmpty()) {
            TimeLogger tl = users.get(0);
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String password = user.getPassword() + tl.getSalt();
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            String hashString = String.format("%064x", new java.math.BigInteger(1, hash));

            if(hashString.equals(tl.getPassword())) {
                Key key = MacProvider.generateKey();
                
//                String secret = "secret";
//                
//                String header = "{\"alg\":\"HS256\"}";
//                String encHeader = Base64.getEncoder().encodeToString(header.getBytes());
//                System.out.println("header: " + encHeader + ".");
                
                String payload = "{\"sub\":\"" + user.getName() + "\"}";
//                String encPayload = Base64.getEncoder().encodeToString(payload.getBytes());
//                System.out.println("payload: " + encPayload + ".");
//                
//                String signed = HMACSHA256(encHeader + "." + encPayload, secret);
//                System.out.println("signed: " + signed + ".");
                
                String jwt = Jwts.builder()
                    .setPayload(payload)
                    .signWith(SignatureAlgorithm.HS256, key)
                    .compact();
                
                return Response.ok(jwt).build();
            }
        }
        
        return Response.status(401).build();
    }
    
    /*private String HMACSHA256(String message, String secret) {
        try {

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            return Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(message.getBytes()));
        } catch (Exception e) {
            System.out.println("Error");
           }
        return "";
    }*/
}
