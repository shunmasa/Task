

package org.example
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.util.*

import java.time.Duration

class Todo(
    val id: UUID = UUID.randomUUID(),
    var taskTitle: String,
    var taskDetail: String,
    var date: LocalDate,
    var time: LocalTime,
    var dueDateTimeMessage: String
)


//// this not woeking call.receiveParameters()["date"],call.receiveParameters()["time"]
////                val parameters = call.receiveParameters()
////                println(" parameters: $parameters")
//
//
////                val date = call.receiveParameters()["date"]
////                println("date: $date")
////                val time = call.receiveParameters()["time"]
////                println("time: $time")
//
//
////                val todoText = call.receiveParameters()["todo"]
////                println("todoText: $todoText")

val todos = mutableListOf<Todo>()
fun main() {
    val todos = loadTodos()
    val server = embeddedServer(Netty, port = 8080) {
        routing {
            static("/styles") {
                resources("styles")
            }

            get("/") {
                val todos = loadTodos()
                call.respondText(renderTodoList(todos), ContentType.Text.Html)
            }
            post("/add") {
                println("Received POST request to /add")

                val parameters = call.receiveParameters()
                val date = parameters["date"]
                val time = parameters["time"]
                val taskTitle = parameters["taskTitle"]
                val taskDetail = parameters["taskDetail"]
                // Check if the "todo" parameter is missing or empty
                if (taskTitle.isNullOrEmpty() || taskDetail.isNullOrEmpty()) {
                    call.respondRedirect("/")
                    return@post
                }

                // Validate that date and time are not null
                if (date != null && time != null) {
                    try {
                        // Parse the date and time strings into LocalDate and LocalTime objects
                        val parsedDate = LocalDate.parse(date)
                        val parsedTime = LocalTime.parse(time)
                        val dueDateTime = LocalDateTime.of(parsedDate , parsedTime )
                        val currentDateTime = LocalDateTime.now()
                        val difference = Duration.between(currentDateTime, dueDateTime)
                        val days = difference.toDays()
                        val hours = difference.toHours() % 24
                        val minutes = difference.toMinutes() % 60

                        // Create a new Todo object with the parsed date and time
                        val newTodo = Todo(taskTitle = taskTitle, taskDetail = taskDetail, date = parsedDate,
                            time = parsedTime,
                            dueDateTimeMessage ="Due date is $days days, $hours hours, and $minutes minutes from now")
                        todos.add(newTodo)
                        saveTodos(todos)
                        println("New todo added: $newTodo")

                        call.respondRedirect("/")

                    } catch (e: Exception) {
                        println("Error adding new todo: ${e.message}")
                        call.respondHtml {
                            head {
                                title("Error")
                            }
                            body {
                                h1 { +"Error" }
                                p { +"An error occurred while adding the todo: ${e.message}" }
                            }
                        }
                    }
                } else {
                    println("Date or time is null")
                    call.respondHtml {
                        head {
                            title("Error")
                        }
                        body {
                            h1 { +"Error" }
                            p { +"Date or time is null" }
                        }
                    }
                }
            }

            post("/update") {
                val parameters = call.receiveParameters()
                val idString = parameters["id"]
                val taskTitle = parameters["taskTitle"]
                val taskDetail = parameters["taskDetail"]
                val date =  parameters["date"]
                val time =  parameters["time"]
                if (idString != null && taskTitle != null && taskDetail != null && date != null && time != null) {
                    val id = UUID.fromString(idString)
                    val todo = todos.find { it.id == id }
                    if (todo != null) {
                        todo.taskTitle = taskTitle
                        todo.taskDetail = taskDetail
                        todo.date = LocalDate.parse(date)
                        todo.time = LocalTime.parse(time)
                        val dueDateTime = LocalDateTime.of(todo.date, todo.time)
                        val currentDateTime = LocalDateTime.now()
                        val difference = Duration.between(currentDateTime, dueDateTime)
                        val days = difference.toDays()
                        val hours = difference.toHours() % 24
                        val minutes = difference.toMinutes() % 60
                        todo.dueDateTimeMessage = "Due date is $days days, $hours hours, and $minutes minutes from now"
                        saveTodos(todos)
                    }
                }
                call.respondRedirect("/")
            }
            post("/remove") {
                val idString = call.receiveParameters()["id"]
                if (idString != null) {
                    val id = UUID.fromString(idString)
                    todos.removeIf { it.id == id }
                    saveTodos(todos)
                }
                call.respondRedirect("/")
            }

            get("/search") {
                val query = call.request.queryParameters["query"]
                val relatedNames = todos.filter {
                    (it.taskTitle + " " + it.taskDetail).contains(query ?: "", ignoreCase = true)
                }.take(10)
                // Only returning the related names list as an unordered list.
                call.respondText(contentType = ContentType.Text.Html) { // Explicitly set content type
                    buildString {
                        appendHTML().ul {
                            relatedNames.forEachIndexed { index, todo ->
                                li {
                                    +todo.taskTitle
                                    +todo.taskTitle
                                    +todo.dueDateTimeMessage
                                    form(action = "/remove", method = FormMethod.post) {
                                        input(type = InputType.hidden, name = "id") {
                                            value = todo.id.toString()
                                        }
                                        input(type = InputType.submit) {
                                            value = "Remove"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            get("/todos") {
                val todos = loadTodos()
                call.respondText(renderTodoListFragment(todos), ContentType.Text.Html)
            }
        }
    }

    server.start(wait = true)
}

fun renderTodoList(todos: MutableList<Todo>): String {
    return buildString {
        appendHTML().html {
            head {
                title("To-Do List with Calendar")
                link(rel = "stylesheet", href = "/styles/styles.css")
                script(src = "https://unpkg.com/htmx.org") {} // Make sure HTMX is included
            }
            body {
                h1 { +"To-Do List" }
                renderAddTodoForm()
                renderSearchComponent()

                // HTMX call to load todos asynchronously
                div {
                    attributes["hx-get"] = "/todos"
                    attributes["hx-trigger"] = "load"
                    attributes["hx-target"] = "#todo-list"
                    id = "todo-list"
                }

            }
        }
    }
}

fun renderTodoListFragment(todos: MutableList<Todo>): String = buildString {
    appendHTML().ul {
        todos.forEach { todo ->
            li {
                +"${todo.taskTitle} - ${todo.taskDetail} - ${todo.dueDateTimeMessage}"
                form(action = "/remove", method = FormMethod.post, classes = "inline") {
                    input(type = InputType.hidden, name = "id") { value = todo.id.toString() }
                    input(type = InputType.submit) { value = "Remove" }
                }
                updateTodo(todo)
            }
        }
    }
}
fun FlowContent.renderAddTodoForm() {
    println("Rendering Add Todo Form")
    form(action = "/add", method = FormMethod.post) {
        input(type = InputType.text, name = "taskTitle") {
            placeholder = "Add a new Task Title"
        }

        input(type = InputType.text, name = "taskDetail") {
            placeholder = "Add a new Task Detail"
            classes = setOf("input-box")
        }
        input(type = InputType.date, name = "date") {
            val date = LocalDate.now().toString()
            println("Date: $date")
            value = date
        }
        input(type = InputType.time, name = "time") {
            val time =  LocalTime.now().toString()
            println("Date: $time")
            value = time
        }
        input(type = InputType.submit) {
            value = "Add"
            onClick = "submitForm()"
        }
    }
}

fun FlowContent.updateTodo(todo: Todo) {
    form(action = "/update", method = FormMethod.post, classes = "inline") {
        input(type = InputType.hidden, name = "id") {
            value = todo.id.toString()
        }
        input(type = InputType.text, name = "taskTitle") {
            value = todo.taskTitle
        }
        input(type = InputType.text, name = "taskDetail") {
            value = todo.taskDetail
        }
        input(type = InputType.date, name = "date") {
            value = todo.date.toString()
        }
        input(type = InputType.time, name = "time") {
            value = todo.time.toString()
        }
        input(type = InputType.submit) {
            value = "Update"
        }
    }
}


fun textarea(name: String, function: () -> Unit) {

}

fun FlowContent.renderSearchComponent(query: String? = null) {
    form(action = "/search", method = FormMethod.get) { // Changed to GET for htmx
        attributes["hx-get"] = "/search"  // htmx attribute to perform AJAX GET
        attributes["hx-trigger"] = "keyup delay:500ms" // Trigger the AJAX call on keyup with delay
        attributes["hx-target"] = "#search-results" // The result will be displayed in the element with this ID
        attributes["hx-indicator"] = ".loading-indicator" // Optional: Show loading indicator
        input(type = InputType.text, name = "query") {
            placeholder = "Search for a name"
            value = query ?: ""
        }
    }
    div { id = "search-results" } // Placeholder for search results
}

fun saveTodos(todos: MutableList<Todo>) {
    val path = "todos.txt"
    try {
        println(todos)
        println("Saving to: ${File(path).absolutePath}") // Debug print
        File(path).writeText(todos.joinToString("\n") {
            "${it.id}|${it.taskTitle}|${it.taskDetail}|${it.date}|${it.time}|${it.dueDateTimeMessage}"
        })
    } catch (e: Exception) {
        println("Error saving todos: ${e.message}")
    }
}

fun loadTodos(): MutableList<Todo> {
    val todos = mutableListOf<Todo>()
    val path = "todos.txt"
    try {
        println("Loading from: ${File(path).absolutePath}") // Debugging information
        if (File(path).exists()) {
            todos.addAll(File(path).readLines().mapNotNull { line ->
                val parts = line.split("|")
                println("parts:,${parts}")
                if (parts.size == 6) { // Ensure there are exactly 6 parts expected
                    try {
                        Todo(
                            id = UUID.fromString(parts[0]),
                            taskTitle = parts[1],
                            taskDetail = parts[2],
                            date = LocalDate.parse(parts[3]), // Assuming ISO-8601 format
                            time = LocalTime.parse(parts[4]), // Assuming ISO-8601 format
                            dueDateTimeMessage = parts[5]
                        )
                    } catch (e: Exception) {
                        println("Error parsing todo item: $line, due to ${e.message}")
                        null
                    }
                } else {
                    println("Invalid line in todos.txt: $line")
                    null
                }
            })
        }
    } catch (e: Exception) {
        println("Error loading todos: ${e.message}")
    }
    return todos
}


