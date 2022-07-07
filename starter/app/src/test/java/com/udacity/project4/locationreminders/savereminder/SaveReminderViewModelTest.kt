package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import org.hamcrest.MatcherAssert.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // Use a fake repository to be injected into the view model.
    private lateinit var reminderDataSource: FakeDataSource

    private var remindersDummy = mutableListOf<ReminderDTO>(
        ReminderDTO("tile1", "des1", "location1", 12.345, 34.567),
        ReminderDTO("tile2", "des2", "location2", 12.345, 34.567)
    )

    @Before
    fun setupViewModel() {
        stopKoin()
        reminderDataSource = FakeDataSource(remindersDummy)
        saveReminderViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), reminderDataSource)
    }

    @Test
    fun saveReminder_showLoading() = runBlockingTest {
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // WHEN: Save reminder
        val reminderData = ReminderDataItem(
            "title data",
            "des data",
            "location data", 11.234, 22.345
        )
        saveReminderViewModel.saveReminder(reminderData)
        // THEN
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun saveReminder_success() = runBlockingTest {
        val reminderData = ReminderDataItem(
            "title data",
            "des data",
            "location data", 11.234, 22.345
        )
        saveReminderViewModel.saveReminder(reminderData)

        val remindersList = (reminderDataSource.getReminders() as Result.Success).data

        val item = remindersList.last()
        assertThat(item.title, `is`(reminderData.title))
        assertThat(item.description, `is`(reminderData.description))
        assertThat(item.location, `is`(reminderData.location))
    }

    @Test
    fun validateEnteredData_titleIsEmpty_returnsFalse() = runBlockingTest {
        val reminderData = ReminderDataItem(
            "",
            "des data",
            "location data", 11.234, 22.345
        )
        val result = saveReminderViewModel.validateEnteredData(reminderData)
        val snackBarMessage = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(result, `is`(false))
        assertThat(snackBarMessage, `is`(R.string.err_enter_title))
    }

    @Test
    fun validateEnteredData_validData_returnsTrue() = runBlockingTest {
        val reminderData = ReminderDataItem(
            "title data",
            "des data",
            "location data", 11.234, 22.345
        )
        val result = saveReminderViewModel.validateEnteredData(reminderData)
        assertThat(result, `is`(true))
    }

    @Test
    fun onClear_resetAllData()= runBlockingTest {
        saveReminderViewModel.onClear()

        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), `is`(nullValue()))
    }
}
