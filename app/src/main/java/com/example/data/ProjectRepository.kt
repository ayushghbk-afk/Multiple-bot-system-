package com.example.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjectsFlow()

    fun getMessagesForProject(projectId: Int): Flow<List<MessageEntity>> =
        projectDao.getMessagesForProjectFlow(projectId)

    fun getTasksForProject(projectId: Int): Flow<List<TaskEntity>> =
        projectDao.getTasksForProjectFlow(projectId)

    suspend fun createProject(title: String, prompt: String): Int {
        val project = ProjectEntity(
            title = title,
            prompt = prompt,
            status = "Planning"
        )
        return projectDao.insertProject(project).toInt()
    }

    suspend fun updateProjectStatus(projectId: Int, status: String) {
        val project = projectDao.getProjectById(projectId)
        if (project != null) {
            projectDao.updateProject(project.copy(status = status))
        }
    }

    suspend fun addMessage(projectId: Int, senderName: String, senderRole: String, message: String) {
        val messageEntity = MessageEntity(
            projectId = projectId,
            senderName = senderName,
            senderRole = senderRole,
            message = message
        )
        projectDao.insertMessage(messageEntity)
    }

    suspend fun addTasks(projectId: Int, tasks: List<Pair<String, String>>) {
        tasks.forEachIndexed { index, (title, assignee) ->
            projectDao.insertTask(
                TaskEntity(
                    projectId = projectId,
                    title = title,
                    assignee = assignee,
                    status = "Pending",
                    orderIndex = index
                )
            )
        }
    }

    suspend fun updateTaskStatus(task: TaskEntity, status: String) {
        projectDao.updateTask(task.copy(status = status))
    }

    suspend fun deleteProject(projectId: Int) {
        projectDao.deleteProjectById(projectId)
    }

    // Config Manager
    val allConfigs: Flow<List<ConfigEntity>> = projectDao.getAllConfigFlow()

    suspend fun saveConfig(key: String, value: String) {
        projectDao.insertConfig(ConfigEntity(key, value))
    }

    suspend fun getConfig(key: String): String? {
        return projectDao.getConfigByKey(key)?.value
    }

    // Dynamic Team Manager
    val allCustomTeamMembers: Flow<List<TeamMemberEntity>> = projectDao.getAllTeamMembersFlow()

    suspend fun addCustomTeamMember(
        name: String,
        role: String,
        motto: String,
        avatarChar: String,
        baseColorHex: String,
        personalizedApiKey: String
    ) {
        projectDao.insertTeamMember(
            TeamMemberEntity(
                name = name,
                role = role,
                motto = motto,
                avatarChar = avatarChar,
                baseColorHex = baseColorHex,
                personalizedApiKey = personalizedApiKey
            )
        )
    }

    suspend fun deleteCustomTeamMember(memberId: Int) {
        projectDao.deleteTeamMemberById(memberId)
    }
}
