/* 
 * Copyright (C) Creactiviti LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Arik Cohen <arik@creactiviti.com>, Mar 2017
 */
package com.creactiviti.piper.core.job;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import com.creactiviti.piper.core.task.JobTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

public class JdbcJobRepository implements JobRepository {

  private NamedParameterJdbcOperations jdbc;
  private ObjectMapper json = new ObjectMapper();
  
  @Override
  public Job findOne(String aId) {
    List<Job> query = jdbc.query("select * from job where id = :id", Collections.singletonMap("id", aId),this::jobRowMappper);
    if(query.size() == 1) {
      return query.get(0);
    }
    return null;
  }
  
  @Override
  public Job findJobByTaskId(String aTaskId) {
    Map<String, String> params = Collections.singletonMap("id", aTaskId);
    return jdbc.queryForObject("select * from job j where j.id = (select job_id from job_task jt where jt.id=:id)", params, this::jobRowMappper);
  }

  @Override
  public List<Job> findAll() {
    return jdbc.query("select * from job order by id desc",this::jobRowMappper);
  }
  
  @Override
  public List<JobTask> getExecution(String aJobId) {
    return jdbc.query("select * From job_task where job_id = :jobId ", Collections.singletonMap("jobId", aJobId),this::jobTaskRowMappper);
  }
    
  @Override
  public void update (Job aJob) {
    MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
    sqlParameterSource.addValue("id", aJob.getId());
    sqlParameterSource.addValue("data", writeValueAsJsonString(aJob));
    jdbc.update("update job set data=:data where id = :id ", sqlParameterSource);
  }

  @Override
  public void create (Job aJob) {
    MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
    sqlParameterSource.addValue("id", aJob.getId());
    sqlParameterSource.addValue("data", writeValueAsJsonString(aJob));
    jdbc.update("insert into job (id,data) values (:id,:data)", sqlParameterSource);
  }
  
  @Override
  public void create(JobTask aJobTask) {
    MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
    sqlParameterSource.addValue("id", aJobTask.getId());
    sqlParameterSource.addValue("jobId", aJobTask.getJobId());
    sqlParameterSource.addValue("data", writeValueAsJsonString(aJobTask));
    jdbc.update("insert into job_task (id,job_id,data) values (:id,:jobId,:data)", sqlParameterSource);
  }
  
  @Override
  public void update(JobTask aJobTask) {
    MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
    sqlParameterSource.addValue("id", aJobTask.getId());
    sqlParameterSource.addValue("jobId", aJobTask.getJobId());
    sqlParameterSource.addValue("data", writeValueAsJsonString(aJobTask));
    jdbc.update("update job_task set data=:data where id = :id ", sqlParameterSource);
  }
    
  public void setJdbcOperations (NamedParameterJdbcOperations aJdbcOperations) {
    jdbc = aJdbcOperations;
  }
  
  public void setJson(ObjectMapper aJson) {
    json = aJson;
  }
  
  private JobTask jobTaskRowMappper (ResultSet aRs, int aIndex) throws SQLException {
    MutableJobTask t = new MutableJobTask(readValueFromString(aRs.getString("data")));
    return t;
  }
    
  private Job jobRowMappper (ResultSet aRs, int aIndex) throws SQLException {
    Map<String, Object> map = readValueFromString(aRs.getString("data"));
    return new MutableJob(map);
  }
  
  private Map<String,Object> readValueFromString (String aValue) {
    if(aValue == null) {
      return null;
    }
    try {
      return json.readValue(aValue, Map.class);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private String writeValueAsJsonString (Object aValue) {
    if(aValue == null) {
      return null;
    }
    try {
      return json.writeValueAsString(aValue);
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
  }


  

}
