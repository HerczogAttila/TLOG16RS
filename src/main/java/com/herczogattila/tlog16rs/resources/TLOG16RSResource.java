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
import static com.herczogattila.tlog16rs.core.JwtService.createJWT;
import static com.herczogattila.tlog16rs.core.JwtService.parseJWT;
import com.herczogattila.tlog16rs.core.ModifyTaskRB;
import com.herczogattila.tlog16rs.core.ModifyWorkDayRB;
import com.herczogattila.tlog16rs.core.StartTaskRB;
import com.herczogattila.tlog16rs.core.UserRB;
import com.herczogattila.tlog16rs.entities.Task;
import com.herczogattila.tlog16rs.entities.TimeLogger;
import com.herczogattila.tlog16rs.entities.WorkDay;
import com.herczogattila.tlog16rs.core.WorkDayRB;
import com.herczogattila.tlog16rs.entities.WorkMonth;
import com.herczogattila.tlog16rs.core.WorkMonthRB;
import com.herczogattila.tlog16rs.core.exceptions.FutureWorkException;
import com.herczogattila.tlog16rs.core.exceptions.InvalidJWTTokenException;
import com.herczogattila.tlog16rs.core.exceptions.InvalidTaskIdException;
import com.herczogattila.tlog16rs.core.exceptions.MissingUserException;
import com.herczogattila.tlog16rs.core.exceptions.NegativeMinutesOfWorkException;
import com.herczogattila.tlog16rs.core.exceptions.NoTaskIdException;
import com.herczogattila.tlog16rs.core.exceptions.NotExpectedTimeOrderException;
import com.herczogattila.tlog16rs.core.exceptions.NotMultipleQuarterHourException;
import com.herczogattila.tlog16rs.core.exceptions.NotNewDateException;
import com.herczogattila.tlog16rs.core.exceptions.NotNewMonthException;
import com.herczogattila.tlog16rs.core.exceptions.NotSeparatedTimesException;
import com.herczogattila.tlog16rs.core.exceptions.WeekendNotEnabledException;
import groovy.util.logging.Slf4j;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
import javax.ws.rs.HeaderParam;
import org.joda.time.DateTime;

/**
 *
 * @author Attila
 */
@Path("/timelogger")
@Slf4j
public class TLOG16RSResource {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TLOG16RSResource.class);
    
    private final CreateDatabase database;
    private final EbeanServer ebeanServer;
    
    public TLOG16RSResource(TLOG16RSConfiguration config) {
        database = new CreateDatabase(config);
        ebeanServer = database.getEbeanServer();
    }
    
    @Path("/workmonths")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkmonths(@HeaderParam("Authorization") String authorization) {
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            return Response.ok(user.getMonths()).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
    
    @Path("/workmonths")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNewMonth(@HeaderParam("Authorization") String authorization, WorkMonthRB month) {
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            WorkMonth workMonth = new WorkMonth(month.getYear(), month.getMonth());
            user.addMonth(workMonth);
            
            ebeanServer.save(user);

            return Response.ok(workMonth).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch(NotNewMonthException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }
    
    @Path("/workmonths/{year}/{month}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkMonth(@HeaderParam("Authorization") String authorization,
            @PathParam(value = "year") int year, @PathParam(value = "month") int month) {
        
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            return Response.ok(findOrCreateWorkMonth(user, year, month).getDays()).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
    
    @Path("/workmonths/workdays")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNewDay(@HeaderParam("Authorization") String authorization, WorkDayRB day) {
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            WorkMonth month = findOrCreateWorkMonth(user, day.getYear(), day.getMonth());
            WorkDay workDay = new WorkDay(day.getRequiredHours(), day.getYear(), day.getMonth(), day.getDay());
            month.addWorkDay(workDay);
            
            ebeanServer.save(user);
            
            return Response.ok(workDay).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch(WeekendNotEnabledException | FutureWorkException | NegativeMinutesOfWorkException | NotNewDateException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }
    }
    
    @Path("/workmonths/workdaysweekend")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNewWeekendDay(@HeaderParam("Authorization") String authorization, WorkDayRB day) {
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            WorkMonth month = findOrCreateWorkMonth(user, day.getYear(), day.getMonth());

            WorkDay workDay = new WorkDay(day.getRequiredHours(), day.getYear(), day.getMonth(), day.getDay());
            month.addWorkDay(workDay, true);
            
            ebeanServer.save(user);
            
            return Response.ok(workDay).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch(WeekendNotEnabledException | FutureWorkException | NegativeMinutesOfWorkException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }
    }
    
    @Path("/workmonths/workdays/{year}/{month}/{day}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkDay(@HeaderParam("Authorization") String authorization, 
            @PathParam(value = "year") int year, @PathParam(value = "month") int month, @PathParam(value = "day") int day) {

        try {
            TimeLogger user = getUserIfValidToken(authorization);
            WorkDay workDay = findOrCreateWorkDay(user, year, month, day);
            return Response.ok(workDay).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
    
    @Path("/workmonths/workdays/modify")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response modifyWorkDay(@HeaderParam("Authorization") String authorization, ModifyWorkDayRB modifyWorkDay) {
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            WorkDay workDay = findOrCreateWorkDay(user, modifyWorkDay.getYear(), modifyWorkDay.getMonth(), modifyWorkDay.getDay());
            if (workDay != null) {
                workDay.setRequiredMinPerDay(modifyWorkDay.getRequiredMinutes());
                workDay.Refresh();
                ebeanServer.save(user);
            }
            
            return Response.ok(workDay).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch(NegativeMinutesOfWorkException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.NOT_MODIFIED).build();            
        }
    }
    
    @Path("/workmonths/{year}/{month}/{day}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkDayTasks(@HeaderParam("Authorization") String authorization,
            @PathParam(value = "year") int year, @PathParam(value = "month") int month, @PathParam(value = "day") int day) {
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            WorkDay wd = findOrCreateWorkDay(user, year, month, day);
            List<Task> tasks = new ArrayList<>();
            if (wd != null)
                tasks = wd.getTasks();

            return Response.ok(tasks).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
    
    @Path("/workmonths/workdays/tasks/start")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startTask(@HeaderParam("Authorization") String authorization, StartTaskRB startTask) {
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            WorkDay day = findOrCreateWorkDay(user, startTask.getYear(), startTask.getMonth(), startTask.getDay());
            if(day != null) {
                Task task = new Task(startTask.getTaskId());
                task.setStartingTime(startTask.getStartTime());
                task.setComment(startTask.getComment());
                day.addTask(task);
                day.Refresh();

                ebeanServer.save(user);
            }

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch(InvalidTaskIdException | NotSeparatedTimesException | NotExpectedTimeOrderException |
                NoTaskIdException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }
    }
    
    @Path("/workmonths/workdays/tasks/finish")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response finishTask(@HeaderParam("Authorization") String authorization, FinishingTaskRB finishTask) {        
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            Task task = findTask(user, finishTask.getYear(), finishTask.getMonth(), finishTask.getDay(), finishTask.getTaskId(), finishTask.getStartTime());
            if(task == null) {
                WorkDay wd = findOrCreateWorkDay(user, finishTask.getYear(), finishTask.getMonth(), finishTask.getDay());
                if(wd != null) {
                    task = new Task(finishTask.getTaskId(), "", finishTask.getStartTime(), finishTask.getEndTime());
                    task.Refresh();
                    wd.addTask(task);
                }
            } else {
                task.setEndTime(finishTask.getEndTime());
                task.Refresh();
                WorkDay wd = findOrCreateWorkDay(user, finishTask.getYear(), finishTask.getMonth(), finishTask.getDay());
                if(wd != null) {
                    wd.getTasks().remove(task);
                    wd.addTask(task);
                    ebeanServer.save(user);
                }
            }

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch(InvalidJWTTokenException | SignatureException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch(NotMultipleQuarterHourException | NotSeparatedTimesException |
                NotExpectedTimeOrderException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }
    }
    
    @Path("/workmonths/workdays/tasks/modify")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyTask(@HeaderParam("Authorization") String authorization, ModifyTaskRB modifyTask) {
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            Task task = findOrCreateTask(user, modifyTask.getYear(), modifyTask.getMonth(), modifyTask.getDay(), modifyTask.getTaskId(), modifyTask.getStartTime());
            if(task != null) {
                task.setTaskId(modifyTask.getNewTaskId());
                task.setComment(modifyTask.getNewComment());
                task.setStartingTime(modifyTask.getNewStartTime());
                task.setEndTime(modifyTask.getNewEndTime());
                task.Refresh();
                
                if(!task.isMultipleQuarterHour()) {
                    throw new NotMultipleQuarterHourException();
                }

                WorkDay wd = findOrCreateWorkDay(user, modifyTask.getYear(), modifyTask.getMonth(), modifyTask.getDay());
                if(wd != null) {
                    wd.getTasks().remove(task);
                    wd.addTask(task);
                    ebeanServer.save(user);
                } else {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch(RuntimeException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }
    }
    
    @Path("/workmonths/workdays/tasks/delete")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteTask(@HeaderParam("Authorization") String authorization, DeleteTaskRB deleteTask) {        
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            
            WorkDay day = findOrCreateWorkDay(user, deleteTask.getYear(), deleteTask.getMonth(), deleteTask.getDay());
            if(day != null) {            
                Task t = findTask(user, deleteTask.getYear(), deleteTask.getMonth(), deleteTask.getDay(), deleteTask.getTaskId(), deleteTask.getStartTime());

                
                if(t != null) {
                    ebeanServer.delete(t);
                    day.getTasks().remove(t);
                }

                day.Refresh();

                ebeanServer.save(user);
            }

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
    
    @Path("/workmonths/deleteall")
    @PUT
    public Response deleteAllWorkmonths(@HeaderParam("Authorization") String authorization) {        
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            
            for(WorkMonth wm: user.getMonths()) {
                ebeanServer.delete(wm);
            }            
            user.getMonths().clear();
            ebeanServer.save(user);
            
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
    
    @Path("/registering")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registering(UserRB user) {
        try {
            getUser(user.getName());
            return Response.status(Response.Status.NOT_MODIFIED).build();
        } catch (MissingUserException e) {
            return registerUser(user);
        }
    }
    
    private Response registerUser(UserRB user) {
        TimeLogger newUser = new TimeLogger(user.getName());
        newUser.setSalt(generateSalt());

        try {
            String hash = generatePasswordHash(user.getPassword(), newUser.getSalt());            
            newUser.setPassword(hash);
            ebeanServer.save(newUser);
            return Response.ok().build();
        } catch (NoSuchAlgorithmException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Path("/authenticate")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response authenticate(UserRB user) {
        try {
            TimeLogger dbUser = getUser(user.getName());
        
            String hash = generatePasswordHash(user.getPassword(), dbUser.getSalt());
            if(hash.equals(dbUser.getPassword())) {
                return Response.ok(createJWT(user.getName())).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (MissingUserException e) {
            LOG.error(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
    
    @Path("/refresh")
    @POST
    public Response refresh(@HeaderParam("Authorization") String authorization) {
        try {
            TimeLogger user = getUserIfValidToken(authorization);
            return Response.ok(createJWT(user.getName())).build();
        } catch(InvalidJWTTokenException | SignatureException | ExpiredJwtException e) {
            LOG.warn(e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
    
    @Path("/isExistUser")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response isExistUserName(String userName) {
        try {
            getUser(userName);
            return Response.status(Response.Status.NOT_MODIFIED).build();            
        } catch (MissingUserException e) {
            return Response.ok().build();
        }
    }
    
    private WorkMonth findOrCreateWorkMonth(TimeLogger timeLogger, int year, int month) {
        WorkMonth wm = timeLogger.findWorkMonth(year, month);
        if(wm == null) {
            wm = new WorkMonth(year, month);
            try {
                timeLogger.addMonth(wm);
                ebeanServer.save(timeLogger);                
            } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
        }
        
        return wm;
    }

    private WorkDay findOrCreateWorkDay(TimeLogger timeLogger, int year, int month, int day) {
        WorkMonth wm = findOrCreateWorkMonth(timeLogger, year, month);
        for(WorkDay wd : wm.getDays()) {
            if(wd.getDayOfMonth() == day)
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
    
    private Task findTask(TimeLogger timeLogger, int year, int month, int day, String taskId, String startTime) {
        WorkDay wd = findOrCreateWorkDay(timeLogger, year, month, day);
        if(wd == null)
            return null;
        
        for(Task t : wd.getTasks()) {
            if(t.getTaskId().equals(taskId) && t.getStartingTime().equals(startTime))
                return t;
        }
        
        return null;
    }
    
    private Task findOrCreateTask(TimeLogger timeLogger, int year, int month, int day, String taskId, String startTime) {
        WorkDay wd = findOrCreateWorkDay(timeLogger, year, month, day);
        if(wd == null)
            return null;
        
        for(Task t : wd.getTasks()) {
            if(t.getTaskId().equals(taskId) && t.getStartingTime().equals(startTime))
                return t;
        }
        
        try {
            Task t = new Task(taskId);
            t.setStartingTime(startTime);
            wd.addTask(t);
            
            ebeanServer.save(timeLogger);
            
            return t;
        } catch(RuntimeException e) { LOG.warn(e.getMessage(), e); }
        
        return null;
    }
    
    private TimeLogger getUserIfValidToken(String jwtToken) {
        Claims claim = parseJWT(jwtToken);
        if(DateTime.now().isAfter(claim.getExpiration().getTime()))
            throw new InvalidJWTTokenException();
        
        return getUser(claim.getSubject());
    }
    
    private TimeLogger getUser(String userName) {
        List<TimeLogger> users = ebeanServer.find(TimeLogger.class)
                .where()
                .eq("name", userName)
                .findList();
        
        if(users.isEmpty()) {
            throw new MissingUserException();
        }
        
        return users.get(0);
    }
    
    private String generateSalt() {
        byte[] salt = new SecureRandom().generateSeed(16);
        return String.format("%064x", new java.math.BigInteger(1, salt));
    }
    
    private String generatePasswordHash(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String digString = password + salt;
        byte[] hash = digest.digest(digString.getBytes(StandardCharsets.UTF_8));
        return String.format("%064x", new java.math.BigInteger(1, hash));
    }
}
