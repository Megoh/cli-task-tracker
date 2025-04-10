package com.dominik.tasktracker;

import com.dominik.tasktracker.model.Task;
import com.zaxxer.hikari.HikariDataSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TaskDAO {
    private static final Logger LOGGER = Logger.getLogger(TaskDAO.class.getName());
    private final HikariDataSource dataSource;

    public TaskDAO(@NotNull HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void executeInTransaction(TransactionWork work) throws TaskDAOException {
        Connection conn = null;
        boolean originalAutoCommit = true;

        try {
            conn = dataSource.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            work.execute(conn);

            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    LOGGER.log(Level.INFO, "Transaction rolled back");
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
            LOGGER.log(Level.SEVERE, "Transaction failed", e);
            throw new TaskDAOException("Transaction failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing connection", e);
                }
            }
        }
    }

    @FunctionalInterface
    private interface TransactionWork {
        void execute(Connection conn) throws Exception;
    }

    public @NotNull Task createTask(@NotNull Task task) throws TaskDAOException {
        if (task.getDescription() == null || task.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Task cannot be null.");
        }

        String sql = "INSERT INTO tasks (description, status, created_at, updated_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, task.getDescription());
            stmt.setObject(2, task.getStatus().name());
            stmt.setTimestamp(3, Timestamp.valueOf(task.getCreatedAt()));
            stmt.setTimestamp(4, Timestamp.valueOf(task.getUpdatedAt()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new TaskDAOException("Task creation failed. No rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    task.setId(generatedKeys.getInt(1));
                } else {
                    throw new TaskDAOException("Task creation failed. No ID obtained.");
                }
            }

            return task;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating task", e);
            throw new TaskDAOException("Error creating task", e);
        }
    }

    public @Nullable Task getTaskById(int id) throws TaskDAOException {
        String sql = "SELECT id, description, status, created_at, updated_at FROM tasks WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Task task = new Task(
                            rs.getString("description"),
                            rs.getString("status"));
                    task.setId(rs.getInt("id"));
                    task.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    task.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return task;
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task by ID: " + id, e);
            throw new TaskDAOException("Error getting task by ID: " + id, e);
        }
    }

    public @NotNull List<Task> getAllTasks() throws TaskDAOException {
        String sql = "SELECT id, description, status, created_at, updated_at FROM tasks";
        List<Task> tasks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String description = rs.getString("description");
                String status = rs.getString("status");

                Task task = new Task(description, status);

                task.setId(rs.getInt("id"));
                task.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                task.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

                tasks.add(task);
            }
            return tasks;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all tasks", e);
            throw new TaskDAOException("Error getting all tasks", e);
        }
    }

    public void updateTask(@NotNull Task task) throws TaskDAOException {
        String sql = "UPDATE tasks SET description = ?, status = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, task.getDescription());
            stmt.setString(2, task.getStatus().name());
            stmt.setTimestamp(3, Timestamp.valueOf(task.getUpdatedAt()));
            stmt.setInt(4, task.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new TaskDAOException("Task update failed. No rows affected. Task ID: " + task.getId());
            }
            LOGGER.log(Level.INFO, "Task updated successfully: {0}", task);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating task: " + task.getId(), e);
            throw new TaskDAOException("Error updating task: " + task.getId(), e);
        }
    }

    public void deleteTask(int id) throws TaskDAOException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new TaskDAOException("Deleting task failed, no rows affected. Task ID: " + id);
            }
            LOGGER.log(Level.INFO, "Task deleted successfully: {0}", id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting task: " + id, e);
            throw new TaskDAOException("Error deleting task: " + id, e);
        }
    }

    public static class TaskDAOException extends Exception {
        public TaskDAOException(String message) {
            super(message);
        }

        public TaskDAOException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
