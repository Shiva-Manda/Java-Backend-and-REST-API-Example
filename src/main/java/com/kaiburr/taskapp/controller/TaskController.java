package com.kaiburr.taskapp.controller;

import com.kaiburr.taskapp.model.Task;
import com.kaiburr.taskapp.model.TaskExecution;
import com.kaiburr.taskapp.repository.TaskRepository;
import com.kaiburr.taskapp.util.CommandValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/tasks")
@CrossOrigin(origins = "http://localhost:3000")
public class TaskController {

    private static final String MALICIOUS_COMMAND_ERROR = "Invalid or malicious command detected";

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable String id) {
        return taskRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Task task) {
        String command = task.getCommand();

        if (!CommandValidator.isSafe(command)) {
            return ResponseEntity
                    .badRequest()
                    .body(MALICIOUS_COMMAND_ERROR);
        }

        Task savedTask = taskRepository.save(task);
        return new ResponseEntity<>(savedTask, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable String id, @RequestBody Task taskDetails) {
        if (!CommandValidator.isSafe(taskDetails.getCommand())) {
            return ResponseEntity
                    .badRequest()
                    .body(MALICIOUS_COMMAND_ERROR);
        }

        return taskRepository.findById(id)
                .map(task -> {
                    task.setName(taskDetails.getName());
                    task.setOwner(taskDetails.getOwner());
                    task.setCommand(taskDetails.getCommand());
                    Task updated = taskRepository.save(task);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        if (!taskRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        taskRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/findByName")
    public ResponseEntity<List<Task>> findTasksByName(@RequestParam String name) {
        List<Task> tasks = taskRepository.findByNameContaining(name);
        if (tasks.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/{id}/execute")
    public ResponseEntity<?> executeTask(@PathVariable String id) {
        Optional<Task> optionalTask = taskRepository.findById(id);
        if (optionalTask.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Task task = optionalTask.get();

        if (!CommandValidator.isSafe(task.getCommand())) {
            return ResponseEntity
                    .badRequest()
                    .body(MALICIOUS_COMMAND_ERROR);
        }

        LocalDateTime startTime = LocalDateTime.now();
        String output = "";

        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.redirectErrorStream(true);

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                builder.command("cmd.exe", "/c", task.getCommand());
            } else {
                builder.command("sh", "-c", task.getCommand());
            }

            Process process = builder.start();

            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroy();
                output = "Execution timed out after 5 minutes.";
            } else {
                output = result.toString();
            }

        } catch (Exception e) {
            output = "Execution failed: " + e.getMessage();
        } finally {
            LocalDateTime endTime = LocalDateTime.now();
            TaskExecution execution = new TaskExecution(startTime, endTime, output);
            task.getTaskExecutions().add(0, execution);
            taskRepository.save(task);
        }

        return ResponseEntity.ok(task);
    }
}