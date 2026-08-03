package com.jarvis.ai;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Task Manager — full to-do list with priorities, due dates, voice-driven.
 * "Add task: call dentist"
 * "My tasks" / "Show to-do list"
 * "Mark dentist done"
 * "High priority: submit report by Friday"
 * "Delete all completed tasks"
 */
public class TaskManager {

    private static final String PREFS    = "task_prefs";
    private static final String KEY_DATA = "tasks_json";
    private static final String DATE_FMT = "yyyy-MM-dd";

    public enum Priority { LOW, MEDIUM, HIGH }

    static class Task {
        int     id;
        String  title;
        Priority priority;
        String  dueDate;   // yyyy-MM-dd or ""
        boolean done;
        String  createdAt;

        Task(int id, String title, Priority priority, String dueDate) {
            this.id        = id;
            this.title     = title;
            this.priority  = priority;
            this.dueDate   = dueDate != null ? dueDate : "";
            this.done      = false;
            this.createdAt = new SimpleDateFormat(DATE_FMT, Locale.US).format(new Date());
        }

        Task(JSONObject j) throws Exception {
            id        = j.getInt("id");
            title     = j.getString("title");
            priority  = Priority.valueOf(j.optString("priority", "MEDIUM"));
            dueDate   = j.optString("dueDate", "");
            done      = j.optBoolean("done", false);
            createdAt = j.optString("createdAt", "");
        }

        JSONObject toJson() throws Exception {
            return new JSONObject()
                .put("id", id).put("title", title)
                .put("priority", priority.name())
                .put("dueDate", dueDate).put("done", done)
                .put("createdAt", createdAt);
        }
    }

    // ── Load / Save ───────────────────────────────────────────────────────────
    private static List<Task> load(Context ctx) {
        List<Task> list = new ArrayList<>();
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATA, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(new Task(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return list;
    }

    private static void save(Context ctx, List<Task> tasks) {
        try {
            JSONArray arr = new JSONArray();
            for (Task t : tasks) arr.put(t.toJson());
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putString(KEY_DATA, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static int nextId(List<Task> tasks) {
        int max = 0;
        for (Task t : tasks) if (t.id > max) max = t.id;
        return max + 1;
    }

    // ── Detection ─────────────────────────────────────────────────────────────
    public static boolean isTaskCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.startsWith("add task") || lower.startsWith("new task") ||
               lower.startsWith("todo ") || lower.startsWith("to do ") ||
               lower.startsWith("to-do ") || lower.contains("my tasks") ||
               lower.contains("my to-do") || lower.contains("show tasks") ||
               lower.contains("task list") || lower.contains("to-do list") ||
               lower.startsWith("mark ") && lower.contains("done") ||
               lower.startsWith("complete ") || lower.contains("finish task") ||
               lower.contains("delete task") || lower.contains("remove task") ||
               lower.contains("clear completed") || lower.contains("pending tasks") ||
               lower.contains("high priority task") || lower.contains("due today");
    }

    // ── Handle ────────────────────────────────────────────────────────────────
    public static String handle(Context ctx, String text) {
        String lower = text.toLowerCase(Locale.US);
        List<Task> tasks = load(ctx);

        // Show tasks
        if (lower.contains("my tasks") || lower.contains("show tasks") ||
            lower.contains("task list") || lower.contains("to-do list") ||
            lower.contains("my to-do") || lower.contains("pending tasks")) {
            return buildList(tasks, lower.contains("pending") || lower.contains("undone"));
        }

        // Clear completed
        if (lower.contains("clear completed") || lower.contains("delete completed") ||
            lower.contains("remove completed")) {
            tasks.removeIf(t -> t.done);
            save(ctx, tasks);
            return "[EMOTION:neutral] Completed tasks cleared, sir. Clean slate.";
        }

        // Delete all
        if ((lower.contains("delete all") || lower.contains("clear all")) && lower.contains("task")) {
            tasks.clear(); save(ctx, tasks);
            return "[EMOTION:neutral] All tasks deleted, sir.";
        }

        // Mark done
        if (lower.startsWith("mark ") || lower.startsWith("complete ") ||
            lower.startsWith("done ") || lower.contains("finish task")) {
            String keyword = text
                .replaceAll("(?i)^(mark|complete|done|finish task)\\s*", "")
                .replaceAll("(?i)\\s*(as\\s+)?done$", "").trim();
            Task found = findTask(tasks, keyword);
            if (found == null) return "[EMOTION:neutral] No task matching **\"" + keyword + "\"** found, sir.";
            found.done = true;
            save(ctx, tasks);
            return "[EMOTION:proud] ✅ **" + found.title + "** marked done, sir. Well done!";
        }

        // Delete specific task
        if (lower.startsWith("delete task") || lower.startsWith("remove task")) {
            String keyword = text.replaceAll("(?i)^(delete|remove)\\s+task\\s*:?\\s*", "").trim();
            Task found = findTask(tasks, keyword);
            if (found == null) return "[EMOTION:neutral] Task **\"" + keyword + "\"** not found, sir.";
            tasks.remove(found); save(ctx, tasks);
            return "[EMOTION:neutral] Task **\"" + found.title + "\"** deleted, sir.";
        }

        // Due today
        if (lower.contains("due today")) {
            String today = new SimpleDateFormat(DATE_FMT, Locale.US).format(new Date());
            StringBuilder sb = new StringBuilder("[EMOTION:neutral] **Tasks due today, sir:**\n\n");
            int count = 0;
            for (Task t : tasks)
                if (!t.done && today.equals(t.dueDate)) {
                    sb.append(priorityIcon(t.priority)).append(" ").append(t.title).append("\n");
                    count++;
                }
            return count == 0 ? "[EMOTION:excited] Nothing due today, sir. You're clear!" : sb.toString().trim();
        }

        // Add task (default)
        String taskText = text
            .replaceAll("(?i)^(add task|new task|todo|to do|to-do)\\s*:?\\s*", "").trim();
        if (taskText.isEmpty()) return "[EMOTION:neutral] What task would you like to add, sir?";

        Priority prio = detectPriority(lower);
        String due   = detectDueDate(lower);

        // Clean priority words from title
        taskText = taskText
            .replaceAll("(?i)\\s*(high|low|medium|urgent|important)\\s+priority\\s*", "")
            .replaceAll("(?i)\\s*by\\s+(today|tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\s*", "")
            .trim();

        Task newTask = new Task(nextId(tasks), taskText, prio, due);
        tasks.add(newTask);
        save(ctx, tasks);

        String dueStr = due.isEmpty() ? "" : " (due " + due + ")";
        return String.format(Locale.US,
            "[EMOTION:warm] %s Added: **%s**%s\nYou have **%d** pending task(s), sir.",
            priorityIcon(prio), taskText, dueStr,
            (int) tasks.stream().filter(t -> !t.done).count());
    }

    private static String buildList(List<Task> tasks, boolean pendingOnly) {
        if (tasks.isEmpty())
            return "[EMOTION:excited] No tasks at all, sir. You're completely free!";

        List<Task> show = new ArrayList<>();
        for (Task t : tasks) if (!pendingOnly || !t.done) show.add(t);
        if (show.isEmpty()) return "[EMOTION:excited] All tasks completed, sir. Phenomenal!";

        // Sort: high priority first, then undone before done
        show.sort((a, b) -> {
            if (a.done != b.done) return a.done ? 1 : -1;
            return b.priority.ordinal() - a.priority.ordinal();
        });

        StringBuilder sb = new StringBuilder("[EMOTION:neutral] **Your Tasks, sir:**\n\n");
        for (Task t : show) {
            String check = t.done ? "✅" : priorityIcon(t.priority);
            String due   = t.dueDate.isEmpty() ? "" : " _(due " + t.dueDate + ")_";
            sb.append(check).append(" ").append(t.done ? "~~" : "**").append(t.title)
              .append(t.done ? "~~" : "**").append(due).append("\n");
        }
        long pending = tasks.stream().filter(t -> !t.done).count();
        long done    = tasks.stream().filter(t -> t.done).count();
        sb.append("\n**").append(pending).append("** pending · **").append(done).append("** done");
        return sb.toString().trim();
    }

    private static Task findTask(List<Task> tasks, String keyword) {
        String kw = keyword.toLowerCase(Locale.US);
        // Exact match first
        for (Task t : tasks)
            if (t.title.equalsIgnoreCase(keyword)) return t;
        // Partial match
        for (Task t : tasks)
            if (t.title.toLowerCase(Locale.US).contains(kw)) return t;
        return null;
    }

    private static Priority detectPriority(String lower) {
        if (lower.contains("high priority") || lower.contains("urgent") || lower.contains("important")) return Priority.HIGH;
        if (lower.contains("low priority") || lower.contains("whenever") || lower.contains("someday")) return Priority.LOW;
        return Priority.MEDIUM;
    }

    private static String detectDueDate(String lower) {
        String today = new SimpleDateFormat(DATE_FMT, Locale.US).format(new Date());
        if (lower.contains("today")) return today;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (lower.contains("tomorrow")) { cal.add(java.util.Calendar.DAY_OF_YEAR, 1); return fmt(cal); }
        String[] days = {"monday","tuesday","wednesday","thursday","friday","saturday","sunday"};
        for (int i = 0; i < days.length; i++) {
            if (lower.contains(days[i])) {
                int target = i + 2; // Calendar.MONDAY = 2
                while (cal.get(java.util.Calendar.DAY_OF_WEEK) != target)
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
                return fmt(cal);
            }
        }
        return "";
    }

    private static String fmt(java.util.Calendar cal) {
        return new SimpleDateFormat(DATE_FMT, Locale.US).format(cal.getTime());
    }

    private static String priorityIcon(Priority p) {
        switch (p) {
            case HIGH:   return "🔴";
            case LOW:    return "🟢";
            default:     return "🟡";
        }
    }
}
