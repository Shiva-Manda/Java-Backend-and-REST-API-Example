package com.kaiburr.taskapp.repository;

import com.kaiburr.taskapp.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByNameContaining(String name);
}