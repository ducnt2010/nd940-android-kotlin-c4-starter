package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    //     Add testing implementation to the RemindersLocalRepository.kt
// Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        remindersLocalRepository =
            RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_retrievesReminder() = runBlocking {
        // GIVEN
        val reminder = ReminderDTO("TITLE", "DES", "LOCATION", 12.345, 21.321)
        remindersLocalRepository.saveReminder(reminder)
        // WHEN
        val result = remindersLocalRepository.getReminder(reminder.id)
        //THEN
        assertThat(result, `is`(notNullValue()))
        result as Result.Success
        assertThat(result.data.id, `is`(reminder.id))
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.location, `is`(reminder.location))
    }

    @Test
    fun getReminder_idNotFound() = runBlocking {
        val result = remindersLocalRepository.getReminder("-1")
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }

    @Test
    fun deleteAllReminders_returnsEmpty() = runBlocking {
        // GIVEN
        val reminder = ReminderDTO("TITLE", "DES", "LOCATION", 12.345, 21.321)
        remindersLocalRepository.saveReminder(reminder)

        // WHEN
        remindersLocalRepository.deleteAllReminders()

        // THEN
        val result = remindersLocalRepository.getReminders()
        result as Result.Success
        assertThat(result.data, `is`(emptyList()))
    }
}
